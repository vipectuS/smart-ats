from __future__ import annotations

from typing import Any

from pydantic import ValidationError

from app.config import Settings
from app.schemas.job_fit import JobFitReportRequest, JobFitReportResponse
from app.services.litellm_resilience import (
    LiteLLMCircuitOpenError,
    LiteLLMRetryExhaustedError,
    ResilientLiteLLMClient,
    get_shared_litellm_client,
)


class JobFitReportGenerationError(RuntimeError):
    pass


class JobFitReportService:
    def __init__(self, settings: Settings, completion_client: ResilientLiteLLMClient | None = None) -> None:
        self.settings = settings
        self.completion_client = completion_client or get_shared_litellm_client()

    async def generate(self, request: JobFitReportRequest) -> JobFitReportResponse:
        try:
            response = await self.completion_client.complete(
                operation="job_fit_report",
                **self.build_request(request),
            )
        except (LiteLLMRetryExhaustedError, LiteLLMCircuitOpenError) as exc:
            raise JobFitReportGenerationError(str(exc)) from exc
        content = self._extract_content(response)
        try:
            return JobFitReportResponse.model_validate_json(content)
        except ValidationError as exc:
            raise JobFitReportGenerationError("LiteLLM returned invalid job fit report JSON") from exc

    def build_request(self, request: JobFitReportRequest) -> dict[str, Any]:
        if request.audience == "candidate":
            audience_instruction = "Write for the candidate, in Simplified Chinese, focusing on job suitability and skill improvement advice."
        elif request.audience == "hr":
            audience_instruction = "Write for the HR reviewer, in Simplified Chinese, summarizing candidate-job fit objectively."
        else:
            audience_instruction = (
                "Write a shared fit report in Simplified Chinese that can be shown to both the candidate and HR. "
                "Keep the tone neutral and avoid second-person phrasing."
            )
        return {
            "model": self.settings.job_fit_report_model,
            "fallback_models": self.settings.job_fit_report_fallback_models,
            "temperature": 0,
            **self.settings.litellm_request_options(),
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You generate structured job fit reports for an ATS. Return only valid JSON matching the schema. "
                        "All human-readable string fields must be written in Simplified Chinese. "
                        f"{audience_instruction}"
                    ),
                },
                {
                    "role": "user",
                    "content": (
                        "Generate a structured job fit report from this evaluation payload. Every returned string field must be Simplified Chinese: "
                        f"{request.model_dump_json(by_alias=True)}"
                    ),
                },
            ],
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": "job_fit_report",
                    "schema": JobFitReportResponse.model_json_schema(by_alias=True),
                },
            },
        }

    def _extract_content(self, response: Any) -> str:
        if isinstance(response, dict):
            content = response["choices"][0]["message"]["content"]
        else:
            content = response.choices[0].message.content
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            return "".join(part.get("text", "") for part in content if isinstance(part, dict))
        raise JobFitReportGenerationError("LiteLLM response did not contain string content")