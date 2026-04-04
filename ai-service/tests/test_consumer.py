from __future__ import annotations

import json
from uuid import uuid4

import httpx
import pytest

from app.config import Settings
from app.consumer import ResumeQueueListener
from app.schemas.resume import BasicInfo, RadarScores, TalentProfile
from app.services.callbacks import BackendCallbackClient, BackendCallbackError
from app.services.parser import LiteLLMResumeParser


def build_settings() -> Settings:
    settings = Settings()
    settings.backend_base_url = "http://backend.test"
    settings.internal_callback_api_key = "test-key"
    settings.backend_parse_failed_path = "/internal/api/resumes/{resumeId}/parse-failed"
    return settings


def build_payload() -> str:
    return json.dumps(
        {
            "resumeId": str(uuid4()),
            "rawContentReference": "resumes/jane_doe.pdf",
            "requestedAt": "2026-03-24T12:00:00Z",
        }
    )


def build_profile() -> TalentProfile:
    return TalentProfile(
        basicInfo=BasicInfo(fullName="Jane Doe", email="jane@example.com"),
        skills=[{"name": "Python", "category": "Programming"}],
        radarScores=RadarScores(
            communication=80,
            technicalDepth=88,
            problemSolving=90,
            collaboration=84,
            leadership=72,
            adaptability=86,
        ),
        xaiReasoning="Consistent backend and integration depth across resume evidence.",
    )


class SuccessfulParser:
    async def parse(self, message):
        return build_profile()


class FailingParser:
    async def parse(self, message):
        raise RuntimeError("model timeout")


class RecordingCallbackClient:
    def __init__(self) -> None:
        self.submitted: list[tuple[str, TalentProfile]] = []
        self.failures: list[tuple[str, str]] = []
        self.raise_on_submit = False

    async def submit_parsed_result(self, resume_id, profile: TalentProfile) -> None:
        if self.raise_on_submit:
            raise BackendCallbackError("upload failed")
        self.submitted.append((str(resume_id), profile))

    async def report_failure(self, resume_id, reason: str) -> None:
        self.failures.append((str(resume_id), reason))

    async def close(self) -> None:
        return None


@pytest.mark.anyio
async def test_backend_callback_client_posts_parsed_result_with_internal_api_key() -> None:
    settings = build_settings()
    captured: dict[str, object] = {}

    async def handler(request: httpx.Request) -> httpx.Response:
        captured["path"] = request.url.path
        captured["header"] = request.headers.get("X-Internal-Api-Key")
        captured["body"] = json.loads(request.content.decode())
        return httpx.Response(200, json={"status": 200})

    transport = httpx.MockTransport(handler)
    async with httpx.AsyncClient(base_url=settings.backend_base_url, transport=transport) as http_client:
        client = BackendCallbackClient(settings, http_client=http_client)
        await client.submit_parsed_result(uuid4(), build_profile())

    assert captured["path"].startswith("/internal/api/resumes/")
    assert captured["path"].endswith("/parsed-result")
    assert captured["header"] == "test-key"
    assert captured["body"]["parsedData"]["basicInfo"]["fullName"] == "Jane Doe"
    assert captured["body"]["parsedData"]["radarScores"]["technicalDepth"] == 88


@pytest.mark.anyio
async def test_resume_queue_listener_submits_parsed_result_when_parsing_succeeds() -> None:
    callback_client = RecordingCallbackClient()
    listener = ResumeQueueListener(
        build_settings(),
        parser=SuccessfulParser(),
        callback_client=callback_client,
    )

    payload = build_payload()
    resume_id = json.loads(payload)["resumeId"]
    await listener._handle_payload(payload)

    assert [entry[0] for entry in callback_client.submitted] == [resume_id]
    assert callback_client.failures == []


@pytest.mark.anyio
async def test_resume_queue_listener_reports_failure_when_parser_raises() -> None:
    callback_client = RecordingCallbackClient()
    listener = ResumeQueueListener(
        build_settings(),
        parser=FailingParser(),
        callback_client=callback_client,
    )

    payload = build_payload()
    resume_id = json.loads(payload)["resumeId"]
    await listener._handle_payload(payload)

    assert callback_client.submitted == []
    assert callback_client.failures == [(resume_id, "model timeout")]


@pytest.mark.anyio
async def test_resume_queue_listener_reports_failure_when_upload_raises() -> None:
    callback_client = RecordingCallbackClient()
    callback_client.raise_on_submit = True
    listener = ResumeQueueListener(
        build_settings(),
        parser=SuccessfulParser(),
        callback_client=callback_client,
    )

    payload = build_payload()
    resume_id = json.loads(payload)["resumeId"]
    await listener._handle_payload(payload)

    assert callback_client.submitted == []
    assert callback_client.failures == [(resume_id, "upload failed")]


def test_litellm_parser_request_uses_json_schema_contract() -> None:
    parser = LiteLLMResumeParser(build_settings())

    request = parser.build_request("resumes/jane_doe.pdf")

    assert request["model"] == build_settings().litellm_model
    assert request["response_format"]["type"] == "json_schema"
    assert request["response_format"]["json_schema"]["name"] == "talent_profile"
    assert "basicInfo" in request["response_format"]["json_schema"]["schema"]["properties"]