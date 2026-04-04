from decimal import Decimal

from fastapi.testclient import TestClient

from app.config import Settings
from app.main import app
from app.routers.job_fit_report import get_job_fit_report_service
from app.schemas.job_fit import JobFitReportRequest, JobFitReportResponse
from app.services.job_fit_report import JobFitReportService


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


def test_job_fit_report_service_uses_litellm_and_parses_json(monkeypatch) -> None:
    async def fake_acompletion(**kwargs):
        assert kwargs["model"] == "gpt-4o-mini"
        assert kwargs["response_format"]["json_schema"]["name"] == "job_fit_report"
        return {
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
        }

    monkeypatch.setattr("app.services.job_fit_report.acompletion", fake_acompletion)

    service = JobFitReportService(Settings())
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