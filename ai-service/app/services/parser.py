from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any

from litellm import acompletion
from pydantic import ValidationError

from app.config import Settings
from app.schemas.resume import (
    BasicInfo,
    EducationExperience,
    RadarScores,
    ResumeParseMessage,
    Skill,
    TalentProfile,
    WorkExperience,
)


class ResumeParsingError(RuntimeError):
    pass


class BaseResumeParser(ABC):
    @abstractmethod
    async def parse(self, message: ResumeParseMessage) -> TalentProfile:
        raise NotImplementedError


class MockResumeParser(BaseResumeParser):
    async def parse(self, message: ResumeParseMessage) -> TalentProfile:
        candidate_name = self._build_candidate_name(message.raw_content_reference)
        return TalentProfile(
            basicInfo=BasicInfo(
                fullName=candidate_name,
                email="candidate@example.com",
                phone="13800000000",
                location="Shanghai",
                headline="Full Stack Engineer",
                summary="Structured mock profile generated while LiteLLM credentials are not configured.",
            ),
            workExperiences=[
                WorkExperience(
                    company="Smart ATS Labs",
                    title="Senior Software Engineer",
                    startDate="2021-03",
                    endDate="Present",
                    responsibilities=[
                        "Led resume parsing pipeline integration",
                        "Built API callback and failure recovery flows",
                    ],
                    achievements=[
                        "Reduced parsing turnaround time via async processing",
                    ],
                ),
            ],
            educationExperiences=[
                EducationExperience(
                    school="Zhejiang University",
                    degree="Bachelor of Engineering",
                    fieldOfStudy="Computer Science",
                    startDate="2016-09",
                    endDate="2020-06",
                ),
            ],
            skills=[
                Skill(name="Python", category="Programming", proficiency="Advanced", evidence="Backend orchestration"),
                Skill(name="FastAPI", category="Framework", proficiency="Advanced", evidence="AI microservice APIs"),
                Skill(name="Redis", category="Infrastructure", proficiency="Intermediate", evidence="Async queue consumption"),
            ],
            radarScores=RadarScores(
                communication=82,
                technicalDepth=88,
                problemSolving=90,
                collaboration=84,
                leadership=76,
                adaptability=87,
            ),
            xaiReasoning=(
                "The candidate shows strong backend implementation depth, demonstrated by asynchronous queue "
                "handling, schema-first API integration, and reliable callback design."
            ),
        )

    def _build_candidate_name(self, raw_content_reference: str) -> str:
        stem = Path(raw_content_reference).stem.replace("_", " ").replace("-", " ").strip()
        if not stem:
            return "Unknown Candidate"
        return " ".join(part.capitalize() for part in stem.split())


class LiteLLMResumeParser(BaseResumeParser):
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    async def parse(self, message: ResumeParseMessage) -> TalentProfile:
        request_payload = self.build_request(message.raw_content_reference)
        response = await acompletion(**request_payload)
        content = self._extract_content(response)

        try:
            return TalentProfile.model_validate_json(content)
        except ValidationError as exc:
            raise ResumeParsingError("LiteLLM returned invalid talent profile JSON") from exc

    def build_request(self, raw_content_reference: str) -> dict[str, Any]:
        return {
            "model": self.settings.litellm_model,
            "temperature": 0,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are a resume parsing engine. Return only JSON matching the provided schema. "
                        "Extract basic info, work history, education, skills, radar scores, and XAI reasoning."
                    ),
                },
                {
                    "role": "user",
                    "content": (
                        "Parse the resume referenced by this backend content identifier and emit only valid JSON: "
                        f"{raw_content_reference}"
                    ),
                },
            ],
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": "talent_profile",
                    "schema": TalentProfile.model_json_schema(by_alias=True),
                },
            },
        }

    def _extract_content(self, response: Any) -> str:
        content = response.choices[0].message.content
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            return "".join(part.get("text", "") for part in content if isinstance(part, dict))
        raise ResumeParsingError("LiteLLM response did not contain string content")


def build_resume_parser(settings: Settings) -> BaseResumeParser:
    provider = settings.resume_parser_provider.strip().lower()
    if provider == "litellm":
        return LiteLLMResumeParser(settings)
    if provider == "mock":
        return MockResumeParser()
    raise ValueError(f"Unsupported resume parser provider: {settings.resume_parser_provider}")