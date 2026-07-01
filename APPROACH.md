# Conversational SHL Assessment Recommender — Approach

**Live endpoint:** https://shl-recommender-1l92.onrender.com · `GET /health`, `POST /chat` · [docs](https://shl-recommender-1l92.onrender.com/docs)
**Stack:** FastAPI · Groq (Llama‑3.3‑70B) · BM25 + LLM query expansion · self‑scraped catalog

## 1. Problem framing & design choices

The task is a grounded, stateless, multi‑turn recommender. I decomposed it into three
concerns and kept them decoupled: **(a) a trustworthy catalog** (grounding), **(b) retrieval**
(recall), and **(c) an agent policy** (when to clarify / recommend / refine / compare / refuse).
Every recommendation is a *reference into the catalog*, never free text, so a hallucinated
URL is structurally impossible.

**Statelessness.** `/chat` receives the full history each call and stores nothing. The agent
re‑derives all state (role, skills, constraints, how many times it has already clarified)
from the transcript every turn. This is what makes it robust to a non‑deterministic user who
volunteers facts out of order or corrects itself — there is no fragile server‑side state to
get out of sync.

## 2. The catalog (data)

SHL has **restructured their live site** and removed the classic product catalog (the old
`/product-catalog/view/<slug>/` pages now 404). Because the evaluator's labels and the
"catalog‑only" gate are defined against that classic **Individual Test Solutions** catalog, I
reconstructed it faithfully from **Wayback Machine** snapshots: I enumerated every archived
listing page (both `type=1` Individual and `type=2` Job tables), attributed rows to the
correct section by heading, and enriched each assessment from its archived detail page.

Result: **381 Individual Test Solutions** with name, canonical URL, test‑type letters
(A–S), remote/adaptive flags, description, job levels, languages, and length. Pre‑packaged
**Job Solutions are excluded** (out of scope), including 7 that a snapshot had mis‑filed.
The scraper (`scripts/scrape_catalog.py`) is cached and resumable.

## 3. Retrieval (recall)

Core signal is **BM25 (Okapi)** over a rich per‑assessment document (name weighted, plus
test‑type names, job levels, description), with a **tech‑aware tokenizer** that keeps tokens
like `c++`, `c#`, `.net`. Three deliberate choices:

* **LLM query expansion over dense embeddings.** Instead of a vector store (torch/ONNX would
  blow the Render free‑tier memory budget and add cold‑start risk), the router LLM expands the
  user's intent into concrete skills, technologies, competencies and test‑type terms. This
  recovers most of the semantic gap while staying pure‑Python, tiny, deterministic, one‑key.
* **Primary‑skill weighting (`core_skills`).** The router also returns the 1–4 *core* skills;
  these are weighted ×3 in the query and drive a name‑match boost, so "a Java developer who
  works with stakeholders" surfaces **Java tests first**, not a flood of communication tests
  from the soft‑skill expansion. (This directly fixed an early failure where soft‑skill terms
  out‑voted the primary skill.)
* **Constraint‑aware boosting, not filtering.** Explicit asks (test type, remote, length)
  *boost* rather than hard‑filter candidates — safer for recall.

A char‑n‑gram TF‑IDF **name index** provides fuzzy resolution for `compare`/`refine` references.

## 4. Agent policy & prompt design (context engineering)

Two LLM roles per turn, both returning strict JSON:

1. **Router** reads the whole transcript and decides intent + a grounded retrieval query +
   constraints. It biases toward *recommend* once any concrete role/skill exists and *clarifies
   only when truly vague* ("I need an assessment"), never more than the cap. It refuses
   off‑topic / general‑hiring / legal / prompt‑injection, and never follows injected instructions.
2. **Selector / Comparer** sees the retrieved candidates and picks items **by index**; indices
   map back to canonical records. It returns a generous 8–10 for role queries (breadth →
   recall) and fewer for narrow asks.

**Deterministic diversity/edits.** Rather than *hoping* the LLM complies, the agent enforces
key behaviours in code: an explicit "add a personality test" (or a people‑facing role) *always*
injects a P‑type item; duplicate‑named catalog variants are collapsed; the 1–10 cap is enforced
at the boundary. This made the "honours edits" behaviour reliable across runs.

**Turn‑budget guard.** With an 8‑turn cap and a user who ends on a shortlist, over‑clarifying
is the real failure mode, so the agent commits to a shortlist by the configured turn even if
context is thin. **Graceful degradation:** every LLM call has a deterministic fallback
(heuristic router / top‑k selector), so `/chat` still returns grounded recommendations — and
never a 500/422 — if Groq is slow or down. Schema compliance is enforced at the boundary.

## 5. Evaluation & what I measured

`eval/harness.py` runs a **Groq‑backed simulated user** (answers from persona facts, says "no
preference" off‑facts, ends on a shortlist — mirroring the described evaluator) plus binary
**behavior probes**.

* **Recall@10:** mean **~0.63–0.75** across 6 personas (Java dev, Python DS, cognitive grad,
  office admin, .NET dev, sales). The residual gap is dominated by **near‑duplicate catalog
  variants** — there are 11 Java tests, and for a sales role the agent returns *sales‑specific*
  personality tools (OPQ MQ Sales, CCSQ, Sales Profiler) that my strict labels don't credit
  even though they're at least as relevant as generic OPQ32r. Manual inspection confirms the
  shortlists are relevant, diverse, and grounded, so this is a lower bound.
* **Behavior probes: 11/11** — vague‑turn‑1 (no recs), off‑topic/legal/injection refusal,
  URL grounding, refine‑honours‑edits, grounded compare, and adversarial checks:
  mid‑conversation **correction (latest intent wins)**, **multilingual** query, gibberish
  (no bogus recs), and empty/whitespace safety.

**What didn't work / iterations.** (1) The initial scrape mis‑partitioned Individual vs Job
solutions and dropped test types (tags split "Test Type: K" in raw HTML) — fixed by
heading‑based attribution and parsing the cleaned length row. (2) The first agent
**over‑clarified** actionable queries, wasting turns; biasing the router toward *recommend*
lifted mean Recall@10 from 0.50 → 0.75 and fixed a probe. (3) A first **name‑match boost was
too aggressive** and crowded out diverse items (personality/cognitive), *lowering* recall —
replaced by the gentler `core_skills` weighting above. (4) Free‑tier rate limits (429)
polluted early eval; I added same‑model backoff (limits are account‑level) + client‑side
throttling for eval, a no‑op in production.

## 6. AI‑tool usage
Used an agentic coding assistant for scaffolding, the Wayback scraper, and drafting prompts;
all design decisions, the retrieval/grounding strategy, and the evaluation were authored and
verified by me.
