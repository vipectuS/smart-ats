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
    internal_callback_api_key: str = Field(
        default="smart-ats-internal-callback-key",
        alias="INTERNAL_CALLBACK_API_KEY",
    )
    backend_callback_timeout_seconds: float = Field(default=10.0, alias="BACKEND_CALLBACK_TIMEOUT_SECONDS")
    postgres_host: str = Field(default="127.0.0.1", alias="POSTGRES_HOST")
    postgres_port: int = Field(default=5432, alias="POSTGRES_PORT")
    postgres_db: str = Field(default="smart_ats", alias="POSTGRES_DB")
    postgres_user: str = Field(default="postgres", alias="POSTGRES_USER")
    postgres_password: str = Field(default="talamh", alias="POSTGRES_PASSWORD")
    litellm_model: str = Field(default="gpt-4o-mini", alias="LITELLM_MODEL")
    job_fit_report_model: str = Field(default="gpt-4o-mini", alias="JOB_FIT_REPORT_MODEL")
    resume_parser_provider: str = Field(default="mock", alias="RESUME_PARSER_PROVIDER")
    embedding_model: str = Field(default="text-embedding-3-small", alias="EMBEDDING_MODEL")
    embedding_dimensions: int = Field(default=1536, alias="EMBEDDING_DIMENSIONS")
    external_content_timeout_seconds: float = Field(default=10.0, alias="EXTERNAL_CONTENT_TIMEOUT_SECONDS")

    @property
    def redis_url(self) -> str:
        auth_part = ""
        if self.redis_password:
            auth_part = f":{self.redis_password}@"
        return f"redis://{auth_part}{self.redis_host}:{self.redis_port}/{self.redis_db}"


@lru_cache
def get_settings() -> Settings:
    return Settings()
