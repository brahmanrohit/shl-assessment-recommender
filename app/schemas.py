"""Request/response schemas. The response schema is *non-negotiable* per the
assignment, so we model it exactly and validate hard.

Request tolerance is deliberately generous: the evaluator drives a real LLM user,
so we accept extra keys, odd roles, and empty content without 4xx/5xx — a crash
here is a hard-eval failure."""
from __future__ import annotations
from typing import List, Literal, Optional
from pydantic import BaseModel, Field, field_validator


class Message(BaseModel):
    model_config = {"extra": "ignore"}
    role: str = "user"
    content: str = ""

    @field_validator("content", mode="before")
    @classmethod
    def _coerce_content(cls, v):
        # Some clients send content as a list of parts or null; flatten to text.
        if v is None:
            return ""
        if isinstance(v, list):
            parts = []
            for p in v:
                if isinstance(p, dict):
                    parts.append(str(p.get("text", "")))
                else:
                    parts.append(str(p))
            return " ".join(parts).strip()
        return str(v)


class ChatRequest(BaseModel):
    model_config = {"extra": "ignore"}
    messages: List[Message] = Field(default_factory=list)


class Recommendation(BaseModel):
    name: str
    url: str
    test_type: str  # single-letter primary type, matching the assignment example ("K", "P", ...)


class ChatResponse(BaseModel):
    reply: str
    recommendations: List[Recommendation] = Field(default_factory=list)
    end_of_conversation: bool = False
