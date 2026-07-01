"""The conversational agent orchestration.

Per (stateless) /chat turn:
  1. ROUTER LLM call -> intent + grounded retrieval query + constraints.
  2. Turn-budget guard -> never clarify past the cap; commit to a shortlist in time.
  3. Branch: refuse | clarify | compare | recommend.
  4. RECOMMEND/COMPARE: retrieve candidates, SELECTOR LLM picks *by index*, we map
     indices back to canonical catalog records -> URLs are impossible to hallucinate.

Every LLM call has a deterministic fallback so /chat degrades gracefully (and never
500s) if Groq is slow/down — a hard-eval requirement.
"""
from __future__ import annotations
import logging
import re
from .catalog import Catalog
from .config import settings
from .llm import LLMError, complete_json
from .prompts import (COMPARER_SYSTEM, ROUTER_SYSTEM, SELECTOR_SYSTEM,
                      render_candidates)
from .retrieval import Constraints, Retriever
from .schemas import ChatResponse, Message, Recommendation

log = logging.getLogger("shl.agent")

DEFAULT_CLARIFY = ("Happy to help you find the right SHL assessments. What role are you "
                   "hiring for, and what key skills or competencies matter most?")
DEFAULT_REFUSE = ("I can only help with recommending SHL assessments. Tell me about the "
                  "role you're hiring for and I'll suggest relevant assessments.")

_INJECTION_RE = re.compile(
    r"ignore (the|your|all|previous)|disregard|reveal your|system prompt|you are now|"
    r"act as|jailbreak|forget (the|your|all)|new instructions", re.I)
_OFFTOPIC_RE = re.compile(
    r"\b(weather|joke|poem|recipe|stock|bitcoin|salary|interview question|"
    r"how (do|to) i (hire|fire)|write .* (email|cover letter)|lawsuit|legal|discriminat)",
    re.I)


class Agent:
    def __init__(self, catalog: Catalog, retriever: Retriever):
        self.catalog = catalog
        self.retriever = retriever

    # -------------------- public entrypoint --------------------
    def respond(self, messages: list[Message]) -> ChatResponse:
        try:
            return self._respond(messages)
        except Exception:                       # last-resort safety net
            log.exception("agent crashed; returning safe empty response")
            return ChatResponse(reply=DEFAULT_CLARIFY, recommendations=[],
                                end_of_conversation=False)

    def _respond(self, messages: list[Message]) -> ChatResponse:
        user_msgs = [m for m in messages if m.role == "user" and m.content.strip()]
        n_assistant = sum(1 for m in messages if m.role == "assistant")
        if not user_msgs:
            return ChatResponse(reply=DEFAULT_CLARIFY, end_of_conversation=False)

        route = self._route(messages)
        intent = route.get("intent", "clarify")

        # ---- turn-budget guard: stop clarifying before we run out of turns ----
        if intent == "clarify" and n_assistant >= settings.max_clarifying_questions:
            intent = "recommend"
        if intent == "clarify" and n_assistant >= settings.commit_by_assistant_turn:
            intent = "recommend"

        if intent == "refuse":
            reply = route.get("refusal_reply") or DEFAULT_REFUSE
            return ChatResponse(reply=reply, recommendations=[], end_of_conversation=False)

        if intent == "clarify":
            reply = route.get("clarifying_question") or DEFAULT_CLARIFY
            return ChatResponse(reply=reply, recommendations=[], end_of_conversation=False)

        if intent == "compare":
            resp = self._compare(messages, route)
            if resp is not None:
                return resp
            intent = "recommend"                # fall through if targets unresolved

        return self._recommend(messages, route)

    # -------------------- routing --------------------
    def _route(self, messages: list[Message]) -> dict:
        transcript = self._transcript(messages)
        last_user = next((m.content for m in reversed(messages) if m.role == "user"), "")
        try:
            data = complete_json(ROUTER_SYSTEM, transcript, max_tokens=700)
            if isinstance(data, dict) and data.get("intent") in {
                    "recommend", "clarify", "compare", "refuse"}:
                # defence-in-depth: a clear prompt-injection attempt is always refused,
                # even if the LLM was talked into another intent.
                if data.get("intent") != "refuse" and _INJECTION_RE.search(last_user):
                    return {"intent": "refuse", "in_scope": False,
                            "refusal_reply": DEFAULT_REFUSE}
                return data
        except LLMError as e:
            log.warning("router LLM failed (%s); using heuristic", e)
        return self._heuristic_route(messages)

    def _heuristic_route(self, messages: list[Message]) -> dict:
        last = next((m.content for m in reversed(messages) if m.role == "user"), "")
        joined = " ".join(m.content for m in messages if m.role == "user")
        n_user = sum(1 for m in messages if m.role == "user")
        if _INJECTION_RE.search(last) or _OFFTOPIC_RE.search(last):
            return {"intent": "refuse", "in_scope": False, "refusal_reply": DEFAULT_REFUSE}
        vague = len(last.split()) < 6 and n_user <= 1 and not re.search(
            r"develop|engineer|manager|analyst|sales|java|python|nurse|account|"
            r"support|admin|clerk|data|leader|graduate|technician", last, re.I)
        if vague:
            return {"intent": "clarify", "clarifying_question": DEFAULT_CLARIFY}
        return {"intent": "recommend", "search_query": joined, "expansion_terms": [],
                "constraints": {"test_types": [], "remote": None,
                                "adaptive": None, "max_length_minutes": None}}

    # -------------------- recommend --------------------
    def _recommend(self, messages: list[Message], route: dict) -> ChatResponse:
        query = route.get("search_query") or " ".join(
            m.content for m in messages if m.role == "user")
        expansion = route.get("expansion_terms") or []
        core_skills = route.get("core_skills") or []
        constraints = self._constraints(route)

        cands = self.retriever.search(query, expansion, constraints,
                                      k=settings.retrieve_k, core_skills=core_skills)
        if not cands:  # broaden: raw user text, no constraints
            raw = " ".join(m.content for m in messages if m.role == "user")
            cands = self.retriever.search(raw, [], None, k=settings.retrieve_k)
        if not cands:
            return ChatResponse(
                reply="I couldn't find matching SHL assessments for that. Could you share "
                      "the role title or the main skills you want to assess?",
                recommendations=[], end_of_conversation=False)

        cand_items = [a for a, _ in cands]
        picks, reply, eoc = self._select(messages, cand_items)

        # Deterministically honour explicit test-type requests ("add a personality
        # test" -> a P item MUST appear), rather than relying on the LLM to comply.
        picks = self._ensure_types(picks, cand_items, constraints, messages)

        # backfill toward a fuller shortlist for broad role queries (helps Recall@10)
        if len(picks) < settings.min_recommendations_on_commit:
            for a in cand_items:
                if a not in picks:
                    picks.append(a)
                if len(picks) >= settings.min_recommendations_on_commit:
                    break
        picks = self._dedupe_by_name(picks)[: settings.max_recommendations]

        recos = [Recommendation(**a.to_reco()) for a in picks]
        if not reply:
            reply = f"Here are {len(recos)} SHL assessments that fit your requirements."
        return ChatResponse(reply=reply, recommendations=recos, end_of_conversation=eoc)

    def _select(self, messages, cand_items):
        goal = self._goal(messages)
        user = (f"Conversation goal:\n{goal}\n\nCANDIDATES:\n"
                f"{render_candidates(cand_items)}\n\nChoose the best-fitting candidates.")
        try:
            data = complete_json(SELECTOR_SYSTEM, user, max_tokens=700)
            idx = data.get("picks", [])
            picks = self._map_indices(idx, cand_items)
            reply = (data.get("reply") or "").strip()
            eoc = bool(data.get("end_of_conversation", True))
            if picks:
                return picks, reply, eoc
        except LLMError as e:
            log.warning("selector LLM failed (%s); using top-k fallback", e)
        # deterministic fallback: take the top retrieved items
        n = settings.min_recommendations_on_commit
        return cand_items[:n], "", True

    # -------------------- compare --------------------
    def _compare(self, messages: list[Message], route: dict):
        names = route.get("compare_targets") or []
        targets = self.retriever.resolve_names(names)
        if len(targets) < 1:
            return None
        user = (f"Compare these assessments for the user:\n{render_candidates(targets)}")
        try:
            data = complete_json(COMPARER_SYSTEM, user, max_tokens=700)
            reply = (data.get("reply") or "").strip()
            eoc = bool(data.get("end_of_conversation", False))
        except LLMError as e:
            log.warning("comparer LLM failed (%s); using template", e)
            reply = self._template_compare(targets)
            eoc = False
        recos = [Recommendation(**a.to_reco()) for a in targets[: settings.max_recommendations]]
        if not reply:
            reply = self._template_compare(targets)
        return ChatResponse(reply=reply, recommendations=recos, end_of_conversation=eoc)

    @staticmethod
    def _template_compare(targets) -> str:
        parts = []
        for a in targets:
            tt = ", ".join(a.test_type_names) or "assessment"
            parts.append(f"{a.name} ({tt}): {a.description[:160]}")
        return "Here's how they compare — " + " | ".join(parts)

    # -------------------- helpers --------------------
    # explicit user asks for a test category -> guarantee representation
    _TYPE_KEYWORDS = {
        "P": ("personality", "behaviour", "behavior", "opq", "work style", "motivation"),
        "A": ("cognitive", "aptitude", "reasoning", "numerical", "verbal", "inductive", "ability"),
        "K": ("coding", "programming", "technical skill", "knowledge test"),
        "S": ("simulation", "simulated"),
    }

    _PEOPLE_FACING = ("stakeholder", "customer", "client", "team", "collaborat",
                      "manager", "management", "leader", "lead ", "people", "interpersonal",
                      "communication", "sales", "service", "relationship")

    def _ensure_types(self, picks, cand_items, constraints, messages):
        """Guarantee shortlist diversity/edits deterministically rather than hoping the
        LLM complies: honour explicitly requested test types, and add a personality (P)
        option for people-facing roles (SHL's own guidance) — both help diverse-shortlist
        Recall@10 and make 'add a personality test' reliable."""
        wanted = set(constraints.test_types)
        last_user = next((m.content.lower() for m in reversed(messages)
                          if m.role == "user"), "")
        for t, kws in self._TYPE_KEYWORDS.items():
            if any(k in last_user for k in kws):
                wanted.add(t)
        full = " ".join(m.content.lower() for m in messages if m.role == "user")
        if any(sig in full for sig in self._PEOPLE_FACING):
            wanted.add("P")
        if not wanted:
            return picks
        present = {t for a in picks for t in a.test_type}
        for t in wanted - present:
            for a in cand_items:                       # cand_items is ranked
                if t in a.test_type and a not in picks:
                    picks.insert(min(2, len(picks)), a)  # near the front
                    break
        return picks

    def _constraints(self, route: dict) -> Constraints:
        c = route.get("constraints") or {}
        tt = {t for t in (c.get("test_types") or []) if t in set("ABCDEKPS")}
        return Constraints(
            test_types=tt,
            remote=c.get("remote") if isinstance(c.get("remote"), bool) else None,
            adaptive=c.get("adaptive") if isinstance(c.get("adaptive"), bool) else None,
            max_length=c.get("max_length_minutes") if isinstance(
                c.get("max_length_minutes"), int) else None,
        )

    @staticmethod
    def _dedupe_by_name(items):
        """Some assessments are archived under two slugs (same name, different URL).
        Keep the first occurrence so a shortlist never shows a duplicate name."""
        out, seen = [], set()
        for a in items:
            key = a.name.strip().lower()
            if key not in seen:
                seen.add(key)
                out.append(a)
        return out

    @staticmethod
    def _map_indices(idx, cand_items):
        picks, seen = [], set()
        for i in idx:
            if isinstance(i, bool):
                continue
            if isinstance(i, (int, float)) and 0 <= int(i) < len(cand_items):
                a = cand_items[int(i)]
                if a.url not in seen:
                    seen.add(a.url)
                    picks.append(a)
        return picks

    @staticmethod
    def _transcript(messages: list[Message]) -> str:
        # cap history to the most recent turns (payload/latency guard; a real 8-turn
        # conversation is well under this, so no context is lost in practice).
        recent = messages[-settings.max_history_messages:]
        lines = [f"{m.role.upper()}: {m.content}" for m in recent if m.content.strip()]
        return "CONVERSATION:\n" + "\n".join(lines)

    @staticmethod
    def _goal(messages: list[Message]) -> str:
        return " | ".join(m.content for m in messages if m.role == "user")
