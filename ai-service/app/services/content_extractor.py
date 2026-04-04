from __future__ import annotations

from dataclasses import dataclass
from html.parser import HTMLParser
from urllib.parse import urlparse

import httpx

from app.config import Settings


class ContentExtractionError(RuntimeError):
    pass


class _PlainTextHtmlParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self._parts: list[str] = []

    def handle_data(self, data: str) -> None:
        text = data.strip()
        if text:
            self._parts.append(text)

    def get_text(self) -> str:
        return " ".join(self._parts)


@dataclass
class RemoteContentExtractor:
    settings: Settings

    async def extract_plain_text(self, url: str) -> str:
        parsed = urlparse(url)
        if parsed.scheme not in {"http", "https"}:
            raise ContentExtractionError("Only http/https URLs are supported")

        async with httpx.AsyncClient(timeout=self.settings.external_content_timeout_seconds) as client:
            response = await client.get(url)
            response.raise_for_status()

        content_type = response.headers.get("content-type", "")
        body = response.text
        if "html" in content_type or url.lower().endswith((".html", ".htm")):
            parser = _PlainTextHtmlParser()
            parser.feed(body)
            return parser.get_text()

        if url.lower().endswith(".docx"):
            raise ContentExtractionError("DOCX extraction groundwork is defined but parser is not wired yet")

        return body