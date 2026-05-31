from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    ai_service_host: str = Field(default="0.0.0.0", alias="AI_SERVICE_HOST")
    ai_service_port: int = Field(default=8000, alias="AI_SERVICE_PORT")
    redis_host: str = Field(default="127.0.0.1", alias="REDIS_HOST")
    redis_port: int = Field(default=6379, alias="REDIS_PORT")
    redis_db: int = Field(default=0, alias="REDIS_DB")
    redis_password: str | None = Field(default=None, alias="REDIS_PASSWORD")
    resume_queue_name: str = Field(default="resume_parsing_queue", alias="RESUME_QUEUE_NAME")
    backend_queue_channel: str = Field(default="resume.parse.requests", alias="BACKEND_QUEUE_CHANNEL")
    backend_base_url: str = Field(default="http://127.0.0.1:18080", alias="BACKEND_BASE_URL")
    backend_parsed_result_path: str = Field(
        default="/internal/api/resumes/{resumeId}/parsed-result",
        alias="BACKEND_PARSED_RESULT_PATH",
    )
    backend_parse_failed_path: str | None = Field(default=None, alias="BACKEND_PARSE_FAILED_PATH")
    internal_callback_header_name: str = Field(
        default="X-Internal-Api-Key",
        alias="INTERNAL_CALLBACK_HEADER_NAME",
    )
    internal_callback_api_key: str = Field(default="change-me-internal-callback-key", alias="INTERNAL_CALLBACK_API_KEY")
    backend_callback_timeout_seconds: float = Field(default=10.0, alias="BACKEND_CALLBACK_TIMEOUT_SECONDS")
    postgres_host: str = Field(default="127.0.0.1", alias="POSTGRES_HOST")
    postgres_port: int = Field(default=5432, alias="POSTGRES_PORT")
    postgres_db: str = Field(default="smart_ats", alias="POSTGRES_DB")
    postgres_user: str = Field(default="postgres", alias="POSTGRES_USER")
    postgres_password: str = Field(default="change-me-postgres-password", alias="POSTGRES_PASSWORD")
    dashscope_api_key: str | None = Field(default=None, alias="DASHSCOPE_API_KEY")
    dashscope_api_base: str | None = Field(
        default="https://dashscope.aliyuncs.com/compatible-mode/v1",
        alias="DASHSCOPE_API_BASE",
    )

    litellm_model: str = Field(default="gpt-4o-mini", alias="LITELLM_MODEL")
    ai_completion_fallback_models_raw: str = Field(default="", alias="AI_COMPLETION_FALLBACK_MODELS")
    job_fit_report_model: str = Field(default="gpt-4o-mini", alias="JOB_FIT_REPORT_MODEL")
    job_fit_report_fallback_models_raw: str = Field(default="", alias="JOB_FIT_REPORT_FALLBACK_MODELS")
    resume_parser_provider: str = Field(default="mock", alias="RESUME_PARSER_PROVIDER")
    ai_completion_max_attempts: int = Field(default=3, alias="AI_COMPLETION_MAX_ATTEMPTS")
    ai_completion_retry_base_seconds: float = Field(default=0.5, alias="AI_COMPLETION_RETRY_BASE_SECONDS")
    ai_completion_retry_backoff_multiplier: float = Field(default=2.0, alias="AI_COMPLETION_RETRY_BACKOFF_MULTIPLIER")
    ai_completion_retry_max_delay_seconds: float = Field(default=4.0, alias="AI_COMPLETION_RETRY_MAX_DELAY_SECONDS")
    ai_completion_circuit_breaker_threshold: int = Field(default=3, alias="AI_COMPLETION_CIRCUIT_BREAKER_THRESHOLD")
    ai_completion_circuit_breaker_reset_seconds: float = Field(default=30.0, alias="AI_COMPLETION_CIRCUIT_BREAKER_RESET_SECONDS")
    embedding_model: str = Field(default="text-embedding-3-small", alias="EMBEDDING_MODEL")
    embedding_dimensions: int = Field(default=1536, alias="EMBEDDING_DIMENSIONS")
    external_content_timeout_seconds: float = Field(default=10.0, alias="EXTERNAL_CONTENT_TIMEOUT_SECONDS")
    synthetic_dataset_dir: str = Field(default="../doc/synthetic-dataset", alias="SYNTHETIC_DATASET_DIR")

    @property
    def redis_url(self) -> str:
        auth_part = ""
        if self.redis_password:
            auth_part = f":{self.redis_password}@"
        return f"redis://{auth_part}{self.redis_host}:{self.redis_port}/{self.redis_db}"

    def litellm_request_options(self) -> dict[str, str]:
        options: dict[str, str] = {}
        if self.dashscope_api_key:
            options["api_key"] = self.dashscope_api_key
        if self.dashscope_api_base:
            options["api_base"] = self.dashscope_api_base
        return options

    @property
    def ai_completion_fallback_models(self) -> list[str]:
        return self._parse_model_list(self.ai_completion_fallback_models_raw)

    @property
    def job_fit_report_fallback_models(self) -> list[str]:
        return self._merge_model_lists(
            self._parse_model_list(self.job_fit_report_fallback_models_raw),
            self.ai_completion_fallback_models,
        )

    def _parse_model_list(self, raw: str) -> list[str]:
        return [item.strip() for item in raw.split(",") if item.strip()]

    def _merge_model_lists(self, *groups: list[str]) -> list[str]:
        merged: list[str] = []
        for group in groups:
            for model in group:
                if model not in merged:
                    merged.append(model)
        return merged


@lru_cache
def get_settings() -> Settings:
    return Settings()
