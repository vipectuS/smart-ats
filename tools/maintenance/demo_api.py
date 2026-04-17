from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any
from urllib import error, parse, request


DEFAULT_TIMEOUT_SECONDS = 20


@dataclass
class ApiError(RuntimeError):
    status: int
    body: str

    def __str__(self) -> str:
        return f"HTTP {self.status}: {self.body}"


class JsonApiClient:
    def __init__(self, base_url: str, token: str | None = None, timeout: int = DEFAULT_TIMEOUT_SECONDS) -> None:
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.timeout = timeout

    def with_token(self, token: str) -> "JsonApiClient":
        return JsonApiClient(self.base_url, token=token, timeout=self.timeout)

    def get(self, path: str, params: dict[str, Any] | None = None) -> Any:
        return self.request("GET", path, payload=None, params=params)

    def post(self, path: str, payload: dict[str, Any] | None = None) -> Any:
        return self.request("POST", path, payload=payload)

    def put(self, path: str, payload: dict[str, Any] | None = None) -> Any:
        return self.request("PUT", path, payload=payload)

    def request(
        self,
        method: str,
        path: str,
        payload: dict[str, Any] | None = None,
        params: dict[str, Any] | None = None,
    ) -> Any:
        url = f"{self.base_url}{path}"
        if params:
            url = f"{url}?{parse.urlencode(params)}"

        headers = {"Content-Type": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"

        body = None if payload is None else json.dumps(payload).encode("utf-8")
        req = request.Request(url, data=body, headers=headers, method=method)

        try:
            with request.urlopen(req, timeout=self.timeout) as response:
                raw = response.read().decode("utf-8")
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8")
            raise ApiError(exc.code, detail) from exc

        if not raw:
            return None

        parsed = json.loads(raw)
        if isinstance(parsed, dict) and "data" in parsed:
            return parsed["data"]
        return parsed


def login(base_url: str, username: str, password: str) -> JsonApiClient:
    client = JsonApiClient(base_url)
    data = client.post(
        "/api/v1/auth/login",
        {
            "username": username,
            "password": password,
        },
    )
    return client.with_token(data["accessToken"])


def ensure_user(
    base_url: str,
    username: str,
    email: str,
    password: str,
    role: str,
) -> tuple[JsonApiClient, bool]:
    client = JsonApiClient(base_url)
    created = False
    try:
        client.post(
            "/api/v1/auth/register",
            {
                "username": username,
                "email": email,
                "password": password,
                "role": role,
            },
        )
        created = True
    except ApiError as exc:
        if exc.status != 409:
            raise

    return login(base_url, username, password), created