from __future__ import annotations

from dataclasses import dataclass
from html.parser import HTMLParser
import re
from urllib.parse import urlparse

import httpx

from app.config import Settings


class ContentExtractionError(RuntimeError):
    pass


class _PlainTextHtmlParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self._parts: list[str] = []
        self._title_parts: list[str] = []
        self._meta: dict[str, str] = {}
        self._in_title = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrs_map = {key.lower(): value for key, value in attrs}
        if tag.lower() == "title":
            self._in_title = True

        if tag.lower() != "meta":
            return

        name = (attrs_map.get("name") or attrs_map.get("property") or "").strip().lower()
        content = (attrs_map.get("content") or "").strip()
        if name and content:
            self._meta[name] = content

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() == "title":
            self._in_title = False

    def handle_data(self, data: str) -> None:
        text = data.strip()
        if text:
            self._parts.append(text)
            if self._in_title:
                self._title_parts.append(text)

    def get_text(self) -> str:
        return " ".join(self._parts)

    def get_title(self) -> str | None:
        title = " ".join(self._title_parts).strip()
        return title or None

    def get_meta(self) -> dict[str, str]:
        return dict(self._meta)


@dataclass
class ExtractedRemoteContent:
    source_type: str
    url: str
    plain_text: str
    title: str | None = None
    description: str | None = None
    github_owner: str | None = None
    github_repo: str | None = None
    github_readme_excerpt: str | None = None
    github_languages: list[str] | None = None
    github_recent_public_signals: list[str] | None = None


@dataclass
class RemoteContentExtractor:
    settings: Settings

    async def extract_content(self, source_type: str, url: str) -> ExtractedRemoteContent:
        parsed = urlparse(url)
        if parsed.scheme not in {"http", "https"}:
            raise ContentExtractionError("Only http/https URLs are supported")

        async with httpx.AsyncClient(timeout=self.settings.external_content_timeout_seconds) as client:
            response = await client.get(url)
            response.raise_for_status()

        content_type = response.headers.get("content-type", "")
        body = response.text

        if url.lower().endswith(".docx"):
            raise ContentExtractionError("DOCX extraction groundwork is defined but parser is not wired yet")

        if "html" not in content_type and not url.lower().endswith((".html", ".htm")):
            return ExtractedRemoteContent(source_type=source_type, url=url, plain_text=body)

        parser = _PlainTextHtmlParser()
        parser.feed(body)
        plain_text = parser.get_text()
        meta = parser.get_meta()
        title = parser.get_title() or meta.get("og:title")
        description = meta.get("description") or meta.get("og:description")

        if source_type == "github":
            return self._build_github_content(url, plain_text, title, description)

        return ExtractedRemoteContent(
            source_type=source_type,
            url=url,
            plain_text=plain_text,
            title=title,
            description=description,
        )

    async def extract_plain_text(self, url: str) -> str:
        extracted = await self.extract_content("generic", url)
        return extracted.plain_text

    def _build_github_content(
        self,
        url: str,
        plain_text: str,
        title: str | None,
        description: str | None,
    ) -> ExtractedRemoteContent:
        parsed = urlparse(url)
        path_parts = [part for part in parsed.path.split("/") if part]
        owner = path_parts[0] if len(path_parts) >= 1 else None
        repo = path_parts[1] if len(path_parts) >= 2 else None

        readme_excerpt = self._extract_github_readme_excerpt(plain_text, repo)
        languages = self._extract_github_languages(plain_text)
        recent_public_signals = self._extract_github_recent_public_signals(plain_text)

        return ExtractedRemoteContent(
            source_type="github",
            url=url,
            plain_text=plain_text,
            title=title,
            description=description,
            github_owner=owner,
            github_repo=repo,
            github_readme_excerpt=readme_excerpt,
            github_languages=languages,
            github_recent_public_signals=recent_public_signals,
        )

    def _extract_github_readme_excerpt(self, plain_text: str, repo: str | None) -> str | None:
        normalized = " ".join(plain_text.split())
        if not normalized:
            return None

        match = re.search(r"README\s+(.*)", normalized, flags=re.IGNORECASE)
        if match:
            return match.group(1).strip()[:500]

        if repo:
            repo_pattern = re.escape(repo.replace("-", " "))
            repo_match = re.search(repo_pattern + r"\s+(.*)", normalized, flags=re.IGNORECASE)
            if repo_match:
                return repo_match.group(1).strip()[:500]

        return normalized[:500]

    def _extract_github_languages(self, plain_text: str) -> list[str]:
        candidates = [
            "Python",
            "TypeScript",
            "JavaScript",
            "Kotlin",
            "Java",
            "Go",
            "Rust",
            "C#",
            "C++",
            "HTML",
            "CSS",
            "Vue",
            "Shell",
            "Dockerfile",
        ]

        found: list[str] = []
        for candidate in candidates:
            pattern = r"(?<![A-Za-z0-9#+])" + re.escape(candidate) + r"(?![A-Za-z0-9#+])"
            if re.search(pattern, plain_text, flags=re.IGNORECASE):
                found.append(candidate)
        return found[:6]

    def _extract_github_recent_public_signals(self, plain_text: str) -> list[str]:
        normalized = " ".join(plain_text.split())
        signal_patterns = [
            r"stars?\s+\d+[\d,]*",
            r"forks?\s+\d+[\d,]*",
            r"issues?\s+\d+[\d,]*",
            r"pull requests?\s+\d+[\d,]*",
            r"updated\s+(?:\d+\s+\w+|\w+)\s+ago",
            r"last commit\s+(?:\d+\s+\w+|\w+)\s+ago",
        ]

        signals: list[tuple[int, str]] = []
        for pattern in signal_patterns:
            for match in re.finditer(pattern, normalized, flags=re.IGNORECASE):
                signals.append((match.start(), match.group(0)))

        signals.sort(key=lambda item: item[0])

        deduped: list[str] = []
        seen = set()
        for _, signal in signals:
            lowered = signal.lower()
            if lowered in seen:
                continue
            seen.add(lowered)
            deduped.append(signal)
        return deduped[:5]