# SHL Conversational Assessment Recommender

## 🔴 Live demo

| | URL |
|---|---|
| **API base** | https://shl-recommender-1s8i.onrender.com |
| **Interactive docs (Swagger UI)** | **https://shl-recommender-1s8i.onrender.com/docs** |
| **Health check** | https://shl-recommender-1s8i.onrender.com/health |
| **Chat** | `POST https://shl-recommender-1s8i.onrender.com/chat` |

> Try it in the browser via **/docs** → `POST /chat` → *Try it out*. On Render's free
> tier the first request after idle may take ~50s to wake (cold start).

---

A stateless conversational agent that takes a recruiter from a vague intent
("I'm hiring a Java developer") to a grounded shortlist of **SHL Individual Test
Solutions** through dialogue. It clarifies vague queries, recommends 1–10
assessments, refines on constraint changes, compares assessments, and refuses
anything out of scope — always grounded in a scraped catalog (no hallucinated URLs).

> 📦 **Also in this repo:** [`teamflow/`](teamflow/) — a separate Java 21 + Quarkus +
> MySQL + Docker backend project (REST API with layered architecture, validation,
> and container-first deployment). See [teamflow/README.md](teamflow/README.md).

## API

`GET /health` → `{"status":"ok"}` (200)

`POST /chat` (stateless — send the full history every call)
```json
{"messages":[{"role":"user","content":"Hiring a Java developer who works with stakeholders"}]}
```
→
```json
{"reply":"...","recommendations":[{"name":"Java 8 (New)","url":"https://www.shl.com/...","test_type":"K"}],"end_of_conversation":false}
```
`recommendations` is empty while clarifying or refusing, and a 1–10 item shortlist
once the agent commits. The response schema is always exactly this shape (the
service is engineered to never return 422/500).

## Architecture

```
scripts/scrape_catalog.py   Wayback-sourced scraper -> data/catalog.json (381 items)
app/catalog.py              catalog records + retrieval documents (source of grounding)
app/retrieval.py            BM25 (tech-aware tokenizer) + char-ngram fuzzy name index
app/prompts.py              router / selector / comparer prompts (context engineering)
app/agent.py                orchestration: route -> retrieve -> select (by index)
app/llm.py                  Groq client (OpenAI-compatible), timeouts+retries+fallback
app/main.py                 FastAPI: /health, /chat (defensive, always valid schema)
eval/                       simulated-user replay, Recall@10, behavior probes
```

**Grounding.** The selector LLM only picks candidates *by index* from retrieved
catalog items; indices are mapped back to canonical records, so a URL can never be
invented. **Robustness.** Every LLM call has a deterministic fallback, so `/chat`
degrades gracefully (still returns grounded recs) if Groq is slow or down.

## Run locally

```bash
pip install -r requirements.txt
cp .env.example .env          # add your free GROQ_API_KEY (console.groq.com/keys)
uvicorn app.main:app --reload
```

## Evaluate

```bash
python -m eval.harness        # Recall@10 across personas + behavior-probe pass rate
```

## Rebuild the catalog (optional)

```bash
python scripts/scrape_catalog.py all   # re-scrapes; cached, resumable
```

## Deploy (Render)

Push to GitHub → Render → New → Blueprint (uses `render.yaml`) → set `GROQ_API_KEY`.
`/health` has a 2-minute cold-start grace. A `Dockerfile` is provided as an alternative.

## Stack rationale

- **Groq (Llama-3.3-70B)** — free tier, sub-second latency (comfortably inside the
  30s/turn eval cap).
- **BM25 + LLM query expansion** — pure-Python, tiny memory (fits Render free tier),
  deterministic and defensible; the LLM widens recall by expanding intent into
  concrete skills/competencies/test-type terms.
- **FastAPI** — matches the required contract; async, minimal.
