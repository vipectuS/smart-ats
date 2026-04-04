from __future__ import annotations

from uuid import UUID

import httpx

from app.config import Settings
from app.schemas.resume import ParseFailurePayload, ParsedResultPayload, TalentProfile


class BackendCallbackError(RuntimeError):
    pass


class BackendCallbackClient:
    def __init__(self, settings: Settings, http_client: httpx.AsyncClient | None = None) -> None:
        self.settings = settings
        self._owns_client = http_client is None
        self._client = http_client or httpx.AsyncClient(
            base_url=self.settings.backend_base_url.rstrip("/"),
            timeout=self.settings.backend_callback_timeout_seconds,
        )

    async def close(self) -> None:
        if self._owns_client:
            await self._client.aclose()

    async def submit_parsed_result(self, resume_id: UUID, profile: TalentProfile) -> None:
        payload = ParsedResultPayload.from_profile(profile)
        path = self.settings.backend_parsed_result_path.format(resumeId=resume_id)
        await self._post(path, payload.model_dump(mode="json", by_alias=True))

    async def report_failure(self, resume_id: UUID, reason: str) -> None:
        if not self.settings.backend_parse_failed_path:
            raise BackendCallbackError("BACKEND_PARSE_FAILED_PATH is not configured")

        payload = ParseFailurePayload(failureReason=reason)
        path = self.settings.backend_parse_failed_path.format(resumeId=resume_id)
        await self._post(path, payload.model_dump(mode="json", by_alias=True))

    async def _post(self, path: str, payload: dict[str, object]) -> None:
        response = await self._client.post(path, headers=self._build_headers(), json=payload)
        try:
            response.raise_for_status()
        except httpx.HTTPStatusError as exc:
            raise BackendCallbackError(
                f"Callback failed with status={exc.response.status_code}, body={exc.response.text}"
            ) from exc

    def _build_headers(self) -> dict[str, str]:
        return {
            self.settings.internal_callback_header_name: self.settings.internal_callback_api_key,
        }