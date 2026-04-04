from __future__ import annotations

from typing import Any

from litellm import acompletion
from pydantic import ValidationError

from app.config import Settings
from app.schemas.job_fit import JobFitReportRequest, JobFitReportResponse


class JobFitReportGenerationError(RuntimeError):
    pass


class JobFitReportService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    async def generate(self, request: JobFitReportRequest) -> JobFitReportResponse:
        response = await acompletion(**self.build_request(request))
        content = self._extract_content(response)
        try:
            return JobFitReportResponse.model_validate_json(content)
        except ValidationError as exc:
            raise JobFitReportGenerationError("LiteLLM returned invalid job fit report JSON") from exc

    def build_request(self, request: JobFitReportRequest) -> dict[str, Any]:
        audience_instruction = (
            "Address the candidate directly in second person and focus on job suitability plus skill improvement advice."
            if request.audience == "candidate"
            else "Address the HR reviewer and summarize candidate-job fit objectively."
        )
        return {
            "model": self.settings.job_fit_report_model,
            "temperature": 0,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You generate structured job fit reports for an ATS. Return only valid JSON matching the schema. "
                        f"{audience_instruction}"
                    ),
                },
                {
                    "role": "user",
                    "content": (
                        "Generate a structured job fit report from this evaluation payload: "
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