"""Central configuration. All tunables come from env vars so nothing is baked in."""
from __future__ import annotations
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # --- LLM (Groq, OpenAI-compatible endpoint) ---
    groq_api_key: str = ""
    groq_base_url: str = "https://api.groq.com/openai/v1"
    # Primary + fallback models. If the primary is decommissioned/ratelimited we retry the next.
    llm_model: str = "llama-3.3-70b-versatile"
    llm_fallback_models: str = "openai/gpt-oss-120b,llama-3.1-8b-instant"
    llm_timeout_s: float = 12.0          # per call; 2 calls/turn keeps us < 30s eval cap
    llm_max_retries: int = 2
    llm_temperature: float = 0.0
    llm_max_backoff_s: float = 10.0      # cumulative 429/5xx sleep cap per call
    llm_min_interval_s: float = 0.0      # client-side spacing (eval only; 0 = off)

    # --- Retrieval ---
    catalog_path: str = str(Path(__file__).resolve().parent.parent / "data" / "catalog.json")
    retrieve_k: int = 40                 # candidates handed to the selector LLM
    max_recommendations: int = 10        # schema hard cap
    min_recommendations_on_commit: int = 8  # backfill floor for broad role queries

    # --- Agent behaviour ---
    max_clarifying_questions: int = 2    # never loop clarification past this
    commit_by_assistant_turn: int = 3    # by the Nth assistant turn, stop clarifying and recommend

    @property
    def fallback_models(self) -> list[str]:
        return [m.strip() for m in self.llm_fallback_models.split(",") if m.strip()]


settings = Settings()
