"""
Reconstruct the canonical SHL *Individual Test Solutions* catalog from the
Wayback Machine (the classic product catalog the assignment/evaluator references,
now removed from the live site).

Two phases:
  listing  -> partition + structured fields (name, url, remote, adaptive, test_type)
              parsed from the archived `?type=1&start=N` listing tables.
  detail   -> per-assessment description, job levels, languages, length
              parsed from each archived `view/<slug>/` page.

Output: data/catalog.json  (canonical shl.com URLs; NO web.archive prefix)

Robustness: bounded-concurrency async fetch, exponential backoff on 429/5xx,
on-disk HTML cache (data/cache/) so reruns/resumes are instant.
"""
import asyncio, hashlib, html as htmllib, json, re, sys, time
from pathlib import Path
import httpx

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "data"
CACHE = DATA / "cache"
CACHE.mkdir(parents=True, exist_ok=True)

UA = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    "(KHTML, like Gecko) Chrome/124.0 Safari/537.36",
      "Accept-Language": "en-US,en;q=0.9"}
CDX = "http://web.archive.org/cdx/search/cdx"
VIEW_PREFIX = "https://www.shl.com/solutions/products/product-catalog/view/"
LISTING = "https://www.shl.com/solutions/products/product-catalog"

TEST_TYPE_NAMES = {
    "A": "Ability & Aptitude", "B": "Biodata & Situational Judgement",
    "C": "Competencies", "D": "Development & 360", "E": "Assessment Exercises",
    "K": "Knowledge & Skills", "P": "Personality & Behavior", "S": "Simulations",
}

SEM = asyncio.Semaphore(6)

def cache_path(key: str) -> Path:
    return CACHE / (hashlib.md5(key.encode()).hexdigest() + ".html")

async def fetch(client: httpx.AsyncClient, url: str, use_cache=True) -> str | None:
    cp = cache_path(url)
    if use_cache and cp.exists():
        return cp.read_text(encoding="utf-8", errors="ignore")
    delay = 2.0
    for attempt in range(6):
        try:
            async with SEM:
                r = await client.get(url, headers=UA, timeout=45, follow_redirects=True)
            if r.status_code == 200:
                cp.write_text(r.text, encoding="utf-8", errors="ignore")
                return r.text
            if r.status_code in (429, 500, 502, 503, 504):
                await asyncio.sleep(delay); delay = min(delay * 1.8, 30); continue
            return None  # 404 etc.
        except Exception:
            await asyncio.sleep(delay); delay = min(delay * 1.8, 30)
    return None

def wb_id(ts: str, url: str) -> str:
    return f"http://web.archive.org/web/{ts}id_/{url}"

async def cdx_rows(client, url_pattern, extra=""):
    u = (f"{CDX}?url={url_pattern}&output=json&fl=original,timestamp,statuscode"
         f"&filter=statuscode:200&limit=20000{extra}")
    for attempt in range(5):
        try:
            r = await client.get(u, headers=UA, timeout=90)
            if r.status_code == 200 and r.text.strip():
                return r.json()[1:]
        except Exception:
            await asyncio.sleep(3)
    return []

def clean(x: str) -> str:
    return htmllib.unescape(re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", x))).strip()

# ---------------- listing ----------------
# Row markup varies across snapshots (data-course-id in 2024, data-entity-id in
# 2025) so match any <tr> that contains a catalog "view" link. Cells are
# positional: [0]=title/link, [1]=Remote Testing, [2]=Adaptive/IRT, [3]=Test Type keys.
TR_RE = re.compile(r"<tr\b.*?</tr>", re.S)
TD_RE = re.compile(r"<td\b.*?</td>", re.S)
def _parse_row(tr: str) -> dict | None:
    a = re.search(r'href="(/solutions/products/product-catalog/view/[^"]+)"[^>]*>(.*?)</a>', tr, re.S)
    if not a:
        return None
    url = "https://www.shl.com" + a.group(1)
    if not url.endswith("/"):
        url += "/"
    name = clean(a.group(2))
    tds = TD_RE.findall(tr)
    remote = ("-yes" in tds[1]) if len(tds) >= 2 else None
    adaptive = ("-yes" in tds[2]) if len(tds) >= 3 else None
    keys = re.findall(r'product-catalogue__key[^>]*>\s*([A-Z])\s*<', tds[-1]) if tds else []
    return {"name": name, "url": url, "remote_testing": remote,
            "adaptive_irt": adaptive, "test_type": keys}


def parse_listing_regions(html_text: str):
    """Split a catalog listing page into (individual_rows, job_rows) using the two
    section headings, so rows are attributed correctly even when both tables render.
    Each <tr> is assigned to the nearest heading above it."""
    i_ind = html_text.find("Individual Test Solutions")
    i_job = html_text.find("Pre-packaged Job Solutions")
    individual, job = [], []
    for m in TR_RE.finditer(html_text):
        tr = m.group(0)
        if "product-catalog/view" not in tr:
            continue
        row = _parse_row(tr)
        if not row:
            continue
        pos = m.start()
        d_ind = pos - i_ind if i_ind >= 0 and pos > i_ind else -1
        d_job = pos - i_job if i_job >= 0 and pos > i_job else -1
        # nearest preceding heading wins
        if d_ind >= 0 and (d_job < 0 or d_ind < d_job):
            individual.append(row)
        elif d_job >= 0:
            job.append(row)
        elif i_ind >= 0 and i_job < 0:
            individual.append(row)
        elif i_job >= 0 and i_ind < 0:
            job.append(row)
        else:
            individual.append(row)  # no headings at all -> assume individual page
    return individual, job

async def phase_listing(client):
    """Fetch every archived catalog listing page (both types, all offsets) and build
    authoritative Individual and Job partitions via heading-based row attribution."""
    print("[listing] enumerating archived listing snapshots via CDX ...", flush=True)
    rows = await cdx_rows(client, "shl.com/solutions/products/product-catalog*")
    # one latest snapshot per (type, start); include no-keyword pagination pages
    combos = {}
    for orig, ts, sc in rows:
        if "keyword=" in orig or "/view/" in orig:
            continue
        if "type=" not in orig:
            continue
        typ = "1" if "type=1" in orig else ("2" if "type=2" in orig else None)
        if typ is None:
            continue
        m = re.search(r"start=(\d+)", orig)
        start = int(m.group(1)) if m else 0
        key = (typ, start)
        if key not in combos or ts > combos[key][0]:
            combos[key] = (ts, orig)
    print(f"[listing] {len(combos)} archived (type,start) listing pages", flush=True)

    individual, job = {}, {}
    def merge(dst, row):
        cur = dst.get(row["url"])
        # prefer the record that actually carries test-type keys / flags
        if cur is None or (not cur.get("test_type") and row.get("test_type")):
            dst[row["url"]] = row

    async def do(key):
        ts, orig = combos[key]
        h = await fetch(client, wb_id(ts, orig))
        if not h:
            return
        ind_rows, job_rows = parse_listing_regions(h)
        for r in ind_rows:
            merge(individual, r)
        for r in job_rows:
            merge(job, r)
    await asyncio.gather(*[do(k) for k in combos])

    # a URL seen as job on any page is a job (job naming is unambiguous); drop from individual
    for u in list(individual):
        if u in job:
            del individual[u]
    print(f"[listing] Individual={len(individual)}  Job={len(job)}", flush=True)
    (DATA / "individual_listing.json").write_text(
        json.dumps(list(individual.values()), indent=2, ensure_ascii=False), encoding="utf-8")
    (DATA / "job_urls.json").write_text(
        json.dumps(sorted(job), indent=2), encoding="utf-8")
    return individual, job

# ---------------- detail ----------------
def parse_detail(html_text: str):
    out = {}
    m = re.search(r"<h1[^>]*>(.*?)</h1>", html_text, re.S)
    if m:
        out["name_detail"] = clean(m.group(1))
    for m in re.finditer(r"<h4[^>]*>(.*?)</h4>(.*?)(?=<h4|</div>\s*</div>)", html_text, re.S):
        label = clean(m.group(1)).lower(); val = clean(m.group(2))
        if label.startswith("description"):
            out["description"] = val
        elif "job level" in label:
            out["job_levels"] = val.rstrip(", ")
        elif "language" in label:
            out["languages"] = val.rstrip(", ")
        elif "assessment length" in label or "length" in label:
            mm = re.search(r"=\s*(\d+)", val)
            if mm:
                out["length_minutes"] = int(mm.group(1))
            # "Test Type:" and its letters live in this cleaned row; tags split them
            # in raw HTML, so parse from the stripped text.
            tt = re.search(r"Test Type:\s*([A-Z](?:\s+[A-Z])*)", val)
            if tt:
                out["test_type_detail"] = re.findall(r"[A-Z]", tt.group(1))
    if "test_type_detail" not in out:
        # fallback: badges before the legend row
        block = html_text
        mt = re.search(r"Test Type:\s*(.*?)(?:Remote Testing|</)", block, re.S)
        if mt:
            out["test_type_detail"] = re.findall(r">\s*([A-Z])\s*<", mt.group(1))
    return out

def extract_view_urls(html_text: str):
    urls = set()
    for m in re.finditer(r'href="(/solutions/products/product-catalog/view/[^"]+)"', html_text):
        u = "https://www.shl.com" + m.group(1)
        urls.add(u if u.endswith("/") else u + "/")
    return urls

JOB_NAME_MARKERS = ("solution", "short form", "job focused assessment")

async def phase_seed_from_cdx(client, catalog, job_urls):
    """Recover individual assessments that exist as archived detail pages but never
    appeared in an archived listing page. Staged as provisional and only kept in
    finalize() if they carry a real test type and aren't job-solution-named."""
    vrows = await cdx_rows(client, "shl.com/solutions/products/product-catalog/view/*")
    all_view = set()
    for orig, ts, sc in vrows:
        all_view.add(orig.replace("http://", "https://").rstrip("/") + "/")
    extra = [u for u in all_view if u not in catalog and u not in job_urls]
    for u in extra:
        catalog[u] = {"name": None, "url": u, "remote_testing": None,
                      "adaptive_irt": None, "test_type": [], "_provisional": True}
    print(f"[seed] staged {len(extra)} provisional individuals (kept only if valid) "
          f"-> total {len(catalog)}", flush=True)

async def phase_detail(client, catalog):
    print("[detail] resolving latest snapshot per assessment via CDX ...", flush=True)
    rows = await cdx_rows(client, "shl.com/solutions/products/product-catalog/view/*")
    latest = {}
    for orig, ts, sc in rows:
        key = orig.rstrip("/") + "/"
        key = key.replace("http://", "https://")
        if key not in latest or ts > latest[key]:
            latest[key] = ts
    print(f"[detail] CDX knows {len(latest)} view URLs", flush=True)
    urls = list(catalog.keys())
    done = 0
    async def do(url):
        nonlocal done
        ts = latest.get(url) or latest.get(url.replace("https://", "http://"))
        if not ts:
            # fallback: availability API
            try:
                av = (await client.get(f"http://archive.org/wayback/available?url={url}",
                                       headers=UA, timeout=30)).json()
                ts = av["archived_snapshots"]["closest"]["timestamp"]
            except Exception:
                return
        htmltext = await fetch(client, wb_id(ts, url))
        done += 1
        if done % 25 == 0:
            print(f"[detail] {done}/{len(urls)} fetched", flush=True)
        if not htmltext:
            return
        catalog[url].update(parse_detail(htmltext))
    # process in chunks to keep memory/logging sane
    await asyncio.gather(*[do(u) for u in urls])
    print(f"[detail] enriched {done} pages", flush=True)

def finalize(catalog):
    items = []
    dropped = 0
    for url, r in sorted(catalog.items()):
        tt = r.get("test_type") or r.get("test_type_detail") or []
        tt = [t for t in tt if t in TEST_TYPE_NAMES]
        name = r.get("name") or r.get("name_detail") or ""
        low = name.lower()
        # Pre-packaged Job Solutions are out of scope. Names are unambiguous
        # ("... Solution", "... Short Form", "Job Focused Assessment"); drop them
        # even if a snapshot mis-filed one under the Individual table.
        if low.endswith("solution") or "short form" in low or "job focused assessment" in low:
            dropped += 1
            continue
        # provisional (CDX-only) items also need a real test type to be trusted
        if r.get("_provisional") and not tt:
            dropped += 1
            continue
        items.append({
            "name": r.get("name") or r.get("name_detail"),
            "url": url,
            "remote_testing": r.get("remote_testing"),
            "adaptive_irt": r.get("adaptive_irt"),
            "test_type": tt,
            "test_type_names": [TEST_TYPE_NAMES[t] for t in tt],
            "description": r.get("description", ""),
            "job_levels": r.get("job_levels", ""),
            "languages": r.get("languages", ""),
            "length_minutes": r.get("length_minutes"),
        })
    (DATA / "catalog.json").write_text(json.dumps(items, indent=2, ensure_ascii=False), encoding="utf-8")
    n_desc = sum(1 for i in items if i["description"])
    n_tt = sum(1 for i in items if i["test_type"])
    print(f"[final] wrote {len(items)} items | with description: {n_desc} | "
          f"with test_type: {n_tt} | dropped provisional: {dropped}")

async def main():
    phase = sys.argv[1] if len(sys.argv) > 1 else "all"
    async with httpx.AsyncClient() as client:
        individual, job = await phase_listing(client)
        if phase in ("all", "detail"):
            await phase_seed_from_cdx(client, individual, set(job.keys()))
            await phase_detail(client, individual)
        finalize(individual)

if __name__ == "__main__":
    t0 = time.time()
    asyncio.run(main())
    print(f"[done] {time.time()-t0:.1f}s")
