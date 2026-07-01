"""Minimal, dependency-light LLM client for Groq's OpenAI-compatible API.

Why raw httpx instead of an SDK: full control over timeouts/retries (critical for
the 30s eval cap), trivial provider swap (any OpenAI-compatible base_url works),
and no SDK-version surprises at deploy time.

`complete_json` guarantees a dict back or raises LLMError; the agent catches that
and falls back to a deterministic path so /chat never 500s."""
from __future__ import annotations
import json
import logging
import time
import httpx
from .config import settings

log = logging.getLogger("shl.llm")


class LLMError(RuntimeError):
    pass


def _extract_json(text: str) -> dict:
    """Robustly pull a JSON object out of a model response."""
    text = (text or "").strip()
    if not text:
        raise LLMError("empty completion")
    try:
        return json.loads(text)
    except Exception:
        pass
    # strip code fences / prose around the object
    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        try:
            return json.loads(text[start:end + 1])
        except Exception:
            pass
    raise LLMError(f"could not parse JSON from completion: {text[:200]!r}")


_last_call = [0.0]  # module-level; single-worker service + eval only


def _throttle() -> None:
    """Optional client-side spacing to stay under free-tier RPM during burst eval.
    No-op in production (llm_min_interval_s defaults to 0)."""
    gap = settings.llm_min_interval_s
    if gap <= 0:
        return
    now = time.monotonic()
    wait = _last_call[0] + gap - now
    if wait > 0:
        time.sleep(wait)
    _last_call[0] = time.monotonic()


def _models_to_try() -> list[str]:
    seen, out = set(), []
    for m in [settings.llm_model, *settings.fallback_models]:
        if m and m not in seen:
            seen.add(m)
            out.append(m)
    return out


def complete_json(system: str, user: str, *, temperature: float | None = None,
                  max_tokens: int = 1024) -> dict:
    """Chat completion constrained to a JSON object. Tries primary then fallback
    models; retries transient errors within the per-call time budget."""
    if not settings.groq_api_key:
        raise LLMError("GROQ_API_KEY not configured")

    headers = {"Authorization": f"Bearer {settings.groq_api_key}",
               "Content-Type": "application/json"}
    body = {
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        "temperature": settings.llm_temperature if temperature is None else temperature,
        "max_tokens": max_tokens,
        "response_format": {"type": "json_object"},
    }
    last_err: Exception | None = None
    url = f"{settings.groq_base_url.rstrip('/')}/chat/completions"
    _throttle()
    # Rate limits (429) are account-level, so switching models doesn't help — we back
    # off and retry the SAME model. A cumulative sleep cap keeps us under the 30s cap.
    slept = 0.0
    with httpx.Client(timeout=settings.llm_timeout_s) as client:
        for model in _models_to_try():
            payload = dict(body, model=model)
            for attempt in range(settings.llm_max_retries + 3):
                try:
                    r = client.post(url, headers=headers, json=payload)
                    if r.status_code == 200:
                        content = r.json()["choices"][0]["message"]["content"]
                        return _extract_json(content)
                    if r.status_code == 400 and "response_format" in r.text:
                        payload.pop("response_format", None)
                        continue
                    if r.status_code == 429:
                        wait = _retry_after(r, attempt)
                        if slept + wait > settings.llm_max_backoff_s:
                            last_err = LLMError(f"{model} -> 429 (backoff budget exhausted)")
                            break  # try next model, then give up
                        time.sleep(wait); slept += wait
                        continue  # retry SAME model
                    if r.status_code in (500, 502, 503):
                        wait = min(1.5 * (attempt + 1), 5.0)
                        if slept + wait <= settings.llm_max_backoff_s:
                            time.sleep(wait); slept += wait
                        last_err = LLMError(f"{model} -> {r.status_code}")
                        continue
                    last_err = LLMError(f"{model} -> {r.status_code}: {r.text[:160]}")
                    break  # non-retryable for this model; try next model
                except (httpx.TimeoutException, httpx.TransportError) as e:
                    last_err = e
                    continue
    raise LLMError(f"all models failed: {last_err}")


def _retry_after(r: httpx.Response, attempt: int) -> float:
    """Seconds to wait on a 429: honour Retry-After if present, else exp backoff."""
    ra = r.headers.get("retry-after")
    if ra:
        try:
            return min(float(ra) + 0.2, 10.0)
        except ValueError:
            pass
    return min(1.5 * (2 ** attempt), 8.0)
