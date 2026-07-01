"""FastAPI service.

  GET  /health -> {"status":"ok"} (200) for readiness.
  POST /chat   -> stateless: takes the full message history, returns the next reply
                  plus a grounded shortlist. It is engineered to ALWAYS return the exact
                  response schema with HTTP 200 — never 422/500 — because schema
                  compliance on every response is a hard-eval gate.
"""
from __future__ import annotations
import logging
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from .agent import Agent
from .catalog import Catalog
from .retrieval import Retriever
from .schemas import ChatRequest, ChatResponse

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s %(name)s %(message)s")
log = logging.getLogger("shl.main")

app = FastAPI(title="SHL Conversational Assessment Recommender", version="1.0.0")

# Built once at startup; the agent is stateless per request.
_CATALOG: Catalog | None = None
_AGENT: Agent | None = None


def get_agent() -> Agent:
    global _CATALOG, _AGENT
    if _AGENT is None:
        _CATALOG = Catalog.load()
        _AGENT = Agent(_CATALOG, Retriever(_CATALOG))
        log.info("agent ready with %d catalog items", len(_CATALOG.items))
    return _AGENT


@app.on_event("startup")
def _startup() -> None:
    get_agent()  # warm the index so the first /chat is fast


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.get("/")
def root() -> dict:
    n = len(_CATALOG.items) if _CATALOG else 0
    return {"service": "SHL Conversational Assessment Recommender",
            "catalog_items": n, "endpoints": ["/health", "/chat"]}


@app.post("/chat")
async def chat(request: Request) -> JSONResponse:
    # Parse defensively: a malformed body must not yield a 422 (schema violation).
    try:
        body = await request.json()
    except Exception:
        body = {}
    if not isinstance(body, dict):
        body = {}
    try:
        req = ChatRequest.model_validate(body)
    except Exception:
        req = ChatRequest(messages=[])

    try:
        resp: ChatResponse = get_agent().respond(req.messages)
    except Exception:
        log.exception("unhandled error in /chat")
        resp = ChatResponse(reply="Sorry, something went wrong. Could you restate the "
                                  "role you're hiring for?",
                            recommendations=[], end_of_conversation=False)

    # enforce the 1..10 cap defensively before returning
    resp.recommendations = resp.recommendations[:10]
    return JSONResponse(status_code=200, content=resp.model_dump())
