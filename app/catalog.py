"""Loads the scraped SHL Individual Test Solutions catalog and exposes it as
immutable records plus the retrieval documents built from them.

The catalog is the single source of truth for grounding: every recommendation the
agent emits is a reference into `Catalog.items`, so a URL can never be hallucinated."""
from __future__ import annotations
import json
import logging
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional
from .config import settings

log = logging.getLogger("shl.catalog")

TEST_TYPE_NAMES = {
    "A": "Ability & Aptitude", "B": "Biodata & Situational Judgement",
    "C": "Competencies", "D": "Development & 360", "E": "Assessment Exercises",
    "K": "Knowledge & Skills", "P": "Personality & Behavior", "S": "Simulations",
}


@dataclass
class Assessment:
    name: str
    url: str
    test_type: list[str] = field(default_factory=list)
    test_type_names: list[str] = field(default_factory=list)
    description: str = ""
    job_levels: str = ""
    languages: str = ""
    length_minutes: Optional[int] = None
    remote_testing: Optional[bool] = None
    adaptive_irt: Optional[bool] = None

    @property
    def primary_type(self) -> str:
        """Single-letter type for the response schema (matches assignment example)."""
        return self.test_type[0] if self.test_type else ""

    def doc_text(self) -> str:
        """Rich text used for lexical retrieval. Name is weighted by repetition."""
        parts = [
            self.name, self.name,                     # boost the title
            " ".join(self.test_type_names),
            self.job_levels,
            self.description,
        ]
        return " ".join(p for p in parts if p)

    def to_reco(self) -> dict:
        return {"name": self.name, "url": self.url, "test_type": self.primary_type}


class Catalog:
    def __init__(self, items: list[Assessment]):
        self.items = items
        self.by_url = {a.url: a for a in items}

    @classmethod
    def load(cls, path: str | None = None) -> "Catalog":
        p = Path(path or settings.catalog_path)
        raw: list[dict] = []
        if p.exists():
            raw = json.loads(p.read_text(encoding="utf-8"))
        else:
            # Dev fallback: partial listing-only file if the detail scrape isn't done.
            alt = p.parent / "individual_listing.json"
            if alt.exists():
                raw = json.loads(alt.read_text(encoding="utf-8"))
                log.warning("catalog.json missing; loaded partial %s", alt.name)
            else:
                log.error("no catalog file at %s", p)
        items = []
        for r in raw:
            name = (r.get("name") or "").strip()
            url = (r.get("url") or "").strip()
            if not name or not url:
                continue
            tt = [t for t in (r.get("test_type") or []) if t in TEST_TYPE_NAMES]
            items.append(Assessment(
                name=name, url=url, test_type=tt,
                test_type_names=r.get("test_type_names") or [TEST_TYPE_NAMES[t] for t in tt],
                description=r.get("description") or "",
                job_levels=r.get("job_levels") or "",
                languages=r.get("languages") or "",
                length_minutes=r.get("length_minutes"),
                remote_testing=r.get("remote_testing"),
                adaptive_irt=r.get("adaptive_irt"),
            ))
        # de-dup by url, keep the richer record
        dedup: dict[str, Assessment] = {}
        for a in items:
            cur = dedup.get(a.url)
            if cur is None or len(a.description) > len(cur.description):
                dedup[a.url] = a
        out = sorted(dedup.values(), key=lambda a: a.name.lower())
        log.info("catalog loaded: %d assessments", len(out))
        return cls(out)
