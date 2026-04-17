from datetime import datetime
from typing import Any, Literal
from uuid import UUID

from pydantic import Field

from app.schemas.common import CamelExtraIgnoreModel, CamelModel


class ExternalContentReference(CamelExtraIgnoreModel):
    source_type: str = Field(min_length=1)
    url: str = Field(min_length=1)


class ResumeParseMessage(CamelExtraIgnoreModel):
    resume_id: UUID
    raw_content_reference: str = Field(min_length=1)
    browser_preprocessed_payload: "BrowserResumePreprocessedPayload | None" = None
    external_content_references: list[ExternalContentReference] = Field(default_factory=list)
    requested_at: datetime


class BrowserResumePagePreview(CamelExtraIgnoreModel):
    page_number: int = Field(ge=1)
    width: int = Field(ge=1)
    height: int = Field(ge=1)
    image_data_url: str = Field(min_length=1)
    text_preview: str | None = None


class BrowserResumePreprocessedPayload(CamelExtraIgnoreModel):
    engine: str = Field(min_length=1)
    mode: str = Field(min_length=1)
    source_file_name: str = Field(min_length=1)
    source_mime_type: str = Field(min_length=1)
    source_file_size: int = Field(ge=1)
    derived_reference: str = Field(min_length=1)
    page_count: int = Field(ge=1)
    extracted_text_preview: str | None = None
    generated_at: datetime
    warnings: list[str] = Field(default_factory=list)
    page_previews: list[BrowserResumePagePreview] = Field(default_factory=list)


class BasicInfo(CamelModel):
    full_name: str = Field(min_length=1)
    email: str | None = None
    phone: str | None = None
    location: str | None = None
    headline: str | None = None
    summary: str | None = None


class WorkExperience(CamelModel):
    company: str = Field(min_length=1)
    title: str = Field(min_length=1)
    start_date: str = Field(min_length=1)
    end_date: str | None = None
    responsibilities: list[str] = Field(default_factory=list)
    achievements: list[str] = Field(default_factory=list)


class EducationExperience(CamelModel):
    school: str = Field(min_length=1)
    degree: str = Field(min_length=1)
    field_of_study: str | None = None
    start_date: str | None = None
    end_date: str | None = None


class Skill(CamelModel):
    name: str = Field(min_length=1)
    category: str | None = None
    proficiency: str | None = None
    evidence: str | None = None


class RadarScores(CamelModel):
    communication: int = Field(ge=0, le=100)
    technical_depth: int = Field(ge=0, le=100)
    problem_solving: int = Field(ge=0, le=100)
    collaboration: int = Field(ge=0, le=100)
    leadership: int = Field(ge=0, le=100)
    adaptability: int = Field(ge=0, le=100)


class TalentProfile(CamelModel):
    basic_info: BasicInfo
    work_experiences: list[WorkExperience] = Field(default_factory=list)
    education_experiences: list[EducationExperience] = Field(default_factory=list)
    skills: list[Skill] = Field(default_factory=list)
    radar_scores: RadarScores
    xai_reasoning: str = Field(min_length=1)


class ParsedResultPayload(CamelModel):
    parsed_data: dict[str, Any] = Field(min_length=1)

    @classmethod
    def from_profile(cls, profile: TalentProfile) -> "ParsedResultPayload":
        return cls(parsedData=profile.model_dump(mode="json", by_alias=True))


class ParseFailurePayload(CamelModel):
    status: Literal["PARSE_FAILED"] = "PARSE_FAILED"
    failure_reason: str = Field(min_length=1)