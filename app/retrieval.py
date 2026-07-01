"""Hybrid-ready retrieval over the catalog.

Core signal is BM25 (Okapi) over rich per-assessment documents — pure-Python, tiny
memory, deterministic, and defensible. Lexical recall is widened by LLM-generated
expansion terms (see agent.py) so vague intents ("works with stakeholders") still
reach the right assessments. A char-n-gram TF-IDF index over names provides fuzzy
name resolution for `compare` targets and `refine` references.

Constraint-aware boosting nudges items that match hard filters (test type, remote)
without hard-excluding — safer for recall than filtering."""
from __future__ import annotations
import re
import logging
from dataclasses import dataclass
import numpy as np
from rank_bm25 import BM25Okapi
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from .catalog import Assessment, Catalog

log = logging.getLogger("shl.retrieval")

_STOP = {
    "the", "a", "an", "and", "or", "for", "to", "of", "in", "on", "with", "is",
    "are", "be", "who", "that", "this", "we", "i", "need", "want", "looking",
    "hire", "hiring", "role", "someone", "test", "tests", "assessment", "assessments",
}
# keep tech tokens intact: c++, c#, .net, node.js, java8
_TOKEN_RE = re.compile(r"[a-z0-9][a-z0-9+#.]*")


def tokenize(text: str) -> list[str]:
    text = (text or "").lower()
    toks = []
    for t in _TOKEN_RE.findall(text):
        t = t.rstrip(".")                      # "development." -> "development"; keeps ".net"
        if len(t) < 2 and t not in {"c", "r"}:  # keep languages C, R
            continue
        if t in _STOP:
            continue
        toks.append(t)
    return toks


@dataclass
class Constraints:
    test_types: set[str]
    remote: bool | None = None
    adaptive: bool | None = None
    max_length: int | None = None


class Retriever:
    def __init__(self, catalog: Catalog):
        self.catalog = catalog
        self.items = catalog.items
        self._doc_tokens = [tokenize(a.doc_text()) for a in self.items]
        self.bm25 = BM25Okapi(self._doc_tokens) if self.items else None
        # fuzzy name index
        names = [a.name for a in self.items] or ["__none__"]
        self._name_vec = TfidfVectorizer(analyzer="char_wb", ngram_range=(2, 4))
        self._name_mat = self._name_vec.fit_transform(names)

    # ---- primary retrieval ----
    def search(self, query: str, expansion_terms: list[str] | None = None,
               constraints: Constraints | None = None, k: int = 40,
               core_skills: list[str] | None = None) -> list[tuple[Assessment, float]]:
        if not self.bm25:
            return []
        core_tokens = [t for s in (core_skills or []) for t in tokenize(s)]
        # Weight the primary skill heavily so it dominates the broad soft-skill
        # expansion (a "Java developer who works with stakeholders" must surface Java
        # tests first, not a pile of communication tests).
        q_tokens = core_tokens * 3 + tokenize(query)
        for term in (expansion_terms or []):
            q_tokens.extend(tokenize(term))
        if not q_tokens:
            return []
        scores = np.asarray(self.bm25.get_scores(q_tokens), dtype=float)

        if scores.max() > 0:
            scores = scores / scores.max()      # normalise to 0..1 for stable boosting
        # sharpen named-skill queries: reward items whose NAME contains the PRIMARY
        # skill tokens (not the whole query), so "Core Java"/"Java 8" outrank tangential
        # hits. Additive, never a filter -> can't hurt recall.
        name_terms = (set(core_tokens) or (set(tokenize(query)) - _STOP))
        if name_terms:
            scores = scores + self._name_overlap_boost(name_terms)
        if constraints:
            scores = scores + self._boost(constraints)
        order = np.argsort(-scores)[: max(k, 1)]
        return [(self.items[i], float(scores[i])) for i in order if scores[i] > 0]

    def _name_overlap_boost(self, terms: set[str]) -> np.ndarray:
        # Gentle nudge only: enough to lift an exact-named skill test a few ranks,
        # NOT enough to crowd out diverse items (personality/cognitive) that a role's
        # shortlist also needs — over-boosting here measurably hurt Recall@10.
        b = np.zeros(len(self.items), dtype=float)
        for i, a in enumerate(self.items):
            overlap = len(terms & set(tokenize(a.name)))
            if overlap:
                b[i] += 0.15 * min(overlap, 2)
        return b

    def _boost(self, c: Constraints) -> np.ndarray:
        b = np.zeros(len(self.items), dtype=float)
        for i, a in enumerate(self.items):
            if c.test_types and (set(a.test_type) & c.test_types):
                b[i] += 0.15
            if c.remote is True and a.remote_testing is True:
                b[i] += 0.05
            if c.adaptive is True and a.adaptive_irt is True:
                b[i] += 0.05
            if c.max_length and a.length_minutes and a.length_minutes <= c.max_length:
                b[i] += 0.05
        return b

    # ---- fuzzy name resolution (compare / refine) ----
    def resolve_names(self, names: list[str], threshold: float = 0.35) -> list[Assessment]:
        out, seen = [], set()
        for name in names:
            a = self._best_name(name, threshold)
            if a and a.url not in seen:
                seen.add(a.url)
                out.append(a)
        return out

    def _best_name(self, name: str, threshold: float) -> Assessment | None:
        if not name or not self.items:
            return None
        # exact / substring first
        low = name.strip().lower()
        for a in self.items:
            if a.name.lower() == low:
                return a
        vec = self._name_vec.transform([name])
        sims = cosine_similarity(vec, self._name_mat)[0]
        j = int(np.argmax(sims))
        if sims[j] >= threshold:
            return self.items[j]
        # loose substring fallback (e.g. "OPQ" -> "OPQ32r")
        for a in self.items:
            if low in a.name.lower() or a.name.lower() in low:
                return a
        return None
