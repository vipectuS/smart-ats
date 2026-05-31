from decimal import Decimal
import logging

from fastapi.testclient import TestClient

from app.config import Settings
from app.main import app
from app.routers.job_fit_report import get_job_fit_report_service
from app.schemas.job_fit import JobFitReportRequest, JobFitReportResponse
from app.services.job_fit_report import JobFitReportService


class FakeCompletionClient:
    def __init__(self, response, assertion=None) -> None:
        self.response = response
        self.assertion = assertion

    async def complete(self, *, operation: str, **kwargs):
        assert operation == "job_fit_report"
        if self.assertion is not None:
            self.assertion(kwargs)
        return self.response


class StubJobFitReportService:
    async def generate(self, request: JobFitReportRequest) -> JobFitReportResponse:
        assert request.audience == "candidate"
        assert request.job_title == "Java Platform Engineer"
        return JobFitReportResponse(
            headline="Strong fit with clear Docker upside",
            fitBand="MEDIUM",
            summary="You currently match 82% of this role.",
            strengths=["Java and Spring Boot already align well"],
            risks=["Docker is still a visible gap"],
            improvementSuggestions=["Add one Dockerized backend project"],
            nextSteps=["Refresh resume bullets with deployment evidence"],
            narrative="岗位适应性报告与技能提升建议：你已经较强匹配该岗位，但补强 Docker 会进一步提升胜率。",
        )


def test_job_fit_report_endpoint_returns_expected_payload() -> None:
    app.dependency_overrides[get_job_fit_report_service] = lambda: StubJobFitReportService()

    try:
        with TestClient(app) as client:
            response = client.post(
                "/api/job-fit-report",
                json={
                    "audience": "candidate",
                    "candidateName": "Alice",
                    "jobTitle": "Java Platform Engineer",
                    "jobDescription": "Build Java services with Docker",
                    "jobRequirements": {"skills": ["Java", "Docker"]},
                    "matchScore": 82,
                    "semanticScore": 78,
                    "skillScore": 85,
                    "experienceScore": 80,
                    "educationScore": 70,
                    "matchedSkills": ["Java"],
                    "missingSkills": ["Docker"],
                },
            )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["fitBand"] == "MEDIUM"
    assert "Docker" in response.json()["narrative"]


def test_job_fit_report_endpoint_logs_validation_failure_details(caplog) -> None:
    with TestClient(app) as client:
        with caplog.at_level(logging.WARNING):
            response = client.post(
                "/api/job-fit-report",
                json={
                    "audience": "candidate",
                    "jobTitle": "Java Platform Engineer",
                    "matchScore": 82,
                },
            )

    assert response.status_code == 422
    assert "Request validation failed path=/api/job-fit-report" in caplog.text
    assert "candidateName" in caplog.text
    assert '"jobTitle": "Java Platform Engineer"' in caplog.text


def test_job_fit_report_service_uses_litellm_and_parses_json(monkeypatch) -> None:
    def assert_request(kwargs):
        assert kwargs["model"] == "gpt-4o-mini"
        assert kwargs["fallback_models"] == []
        assert kwargs["response_format"]["json_schema"]["name"] == "job_fit_report"

    service = JobFitReportService(
        Settings(
            JOB_FIT_REPORT_MODEL="gpt-4o-mini",
            DASHSCOPE_API_KEY="",
            DASHSCOPE_API_BASE="",
        ),
        completion_client=FakeCompletionClient(
            {
                "choices": [
                    {
                        "message": {
                            "content": (
                                '{"headline":"Strong fit with one clear gap","fitBand":"MEDIUM","summary":"You currently match 80% of this role.",' 
                                '"strengths":["Java and Spring Boot line up well"],"risks":["Docker is still missing"],' 
                                '"improvementSuggestions":["Build one Docker deployment example"],"nextSteps":["Refresh your resume with deployment metrics"],' 
                                '"narrative":"岗位适应性报告与技能提升建议：你已经较强匹配该岗位，但补强 Docker 会进一步提升胜率。"}'
                            )
                        }
                    }
                ]
            },
            assertion=assert_request,
        ),
    )
    response = __import__("asyncio").run(
        service.generate(
            JobFitReportRequest(
                audience="candidate",
                candidateName="Alice",
                jobTitle="Java Platform Engineer",
                jobDescription="Build Java services with Docker",
                jobRequirements={"skills": ["Java", "Docker"]},
                matchScore=Decimal("80"),
                semanticScore=Decimal("76"),
                skillScore=Decimal("85"),
                experienceScore=Decimal("78"),
                educationScore=Decimal("70"),
                matchedSkills=["Java", "Spring Boot"],
                missingSkills=["Docker"],
            )
        )
    )

    assert response.fit_band == "MEDIUM"
    assert response.improvement_suggestions == ["Build one Docker deployment example"]


def test_job_fit_report_service_passes_dashscope_openai_compatible_options() -> None:
    def assert_request(kwargs):
        assert kwargs["model"] == "openai/qwen-plus"
        assert kwargs["fallback_models"] == ["openai/qwen-turbo", "openai/qwen-max"]
        assert kwargs["api_key"] == "test-key"
        assert kwargs["api_base"] == "https://dashscope.aliyuncs.com/compatible-mode/v1"

    service = JobFitReportService(
        Settings(
            JOB_FIT_REPORT_MODEL="openai/qwen-plus",
            JOB_FIT_REPORT_FALLBACK_MODELS="openai/qwen-turbo, openai/qwen-max",
            DASHSCOPE_API_KEY="test-key",
            DASHSCOPE_API_BASE="https://dashscope.aliyuncs.com/compatible-mode/v1",
        ),
        completion_client=FakeCompletionClient(
            {
                "choices": [
                    {
                        "message": {
                            "content": (
                                '{"headline":"OK","fitBand":"HIGH","summary":"ok","strengths":[],"risks":[],'
                                '"improvementSuggestions":[],"nextSteps":[],"narrative":"ok"}'
                            )
                        }
                    }
                ]
            },
            assertion=assert_request,
        ),
    )
    response = __import__("asyncio").run(
        service.generate(
            JobFitReportRequest(
                audience="candidate",
                candidateName="Alice",
                jobTitle="Java Platform Engineer",
                jobDescription="Build Java services with Docker",
                jobRequirements={"skills": ["Java", "Docker"]},
                matchScore=Decimal("80"),
                semanticScore=Decimal("76"),
                skillScore=Decimal("85"),
                experienceScore=Decimal("78"),
                educationScore=Decimal("70"),
                matchedSkills=["Java", "Spring Boot"],
                missingSkills=["Docker"],
            )
        )
    )

    assert response.fit_band == "HIGH"