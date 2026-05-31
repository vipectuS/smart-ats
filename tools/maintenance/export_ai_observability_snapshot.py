from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import urlopen


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Fetch /health/ai-observability and export JSON plus Markdown evidence artifacts."
    )
    parser.add_argument(
        "--endpoint",
        default="http://127.0.0.1:8000/health/ai-observability",
        help="AI-service observability endpoint.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path(__file__).resolve().parent / "output",
        help="Directory for generated evidence files.",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=float,
        default=5.0,
        help="HTTP timeout for the endpoint request.",
    )
    args = parser.parse_args()

    payload = fetch_snapshot(args.endpoint, args.timeout_seconds)
    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    json_path = output_dir / f"ai_observability_snapshot_{timestamp}.json"
    markdown_path = output_dir / f"ai_observability_snapshot_{timestamp}.md"

    json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    markdown_path.write_text(render_markdown(payload, args.endpoint, json_path.name), encoding="utf-8")

    print(
        json.dumps(
            {
                "endpoint": args.endpoint,
                "snapshotJson": str(json_path),
                "snapshotMarkdown": str(markdown_path),
            },
            ensure_ascii=False,
            indent=2,
        )
    )


def fetch_snapshot(endpoint: str, timeout_seconds: float) -> dict[str, Any]:
    try:
        with urlopen(endpoint, timeout=timeout_seconds) as response:
            status_code = getattr(response, "status", 200)
            if status_code >= 400:
                raise RuntimeError(f"Endpoint returned HTTP {status_code}")
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        raise RuntimeError(f"Failed to fetch AI observability snapshot: HTTP {exc.code}") from exc
    except URLError as exc:
        raise RuntimeError(f"Failed to fetch AI observability snapshot: {exc.reason}") from exc


def render_markdown(payload: dict[str, Any], endpoint: str, json_name: str) -> str:
    observability = payload.get("aiObservability", {}) if isinstance(payload, dict) else {}
    operations = observability.get("operations", {}) if isinstance(observability, dict) else {}
    embedding_fallbacks = observability.get("embeddingFallbacks", {}) if isinstance(observability, dict) else {}
    external_failures = observability.get("externalContentFailures", 0)
    external_warnings = observability.get("externalContentWarnings", 0)

    lines = [
        "# AI Observability Snapshot",
        "",
        f"- Endpoint: {endpoint}",
        f"- Service status: {payload.get('status', 'unknown')}",
        f"- Queue: {payload.get('queue', 'unknown')}",
        f"- Source JSON: {json_name}",
        f"- Last updated epoch seconds: {observability.get('lastUpdatedEpochSeconds', 'n/a')}",
        "",
        "## Headline Counters",
        "",
        f"- External content failures: {external_failures}",
        f"- External content warnings: {external_warnings}",
        f"- Embedding fallback reasons: {format_mapping(embedding_fallbacks)}",
        "",
        "## Operation Table",
        "",
        "| Operation | Requests | Successes | Failures | Retry Attempts | Retry Exhausted | Circuit Opened | Circuit Rejections | Avg Latency ms | P95 Latency ms | Last Error |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |",
    ]

    if not operations:
        lines.append("| n/a | 0 | 0 | 0 | 0 | 0 | 0 | 0 | n/a | n/a | n/a |")
    else:
        for operation, metrics in sorted(operations.items()):
            lines.append(
                "| {operation} | {requests} | {successes} | {failures} | {retry_attempts} | {retry_exhausted} | {circuit_opened} | {circuit_rejections} | {avg_latency} | {p95_latency} | {last_error} |".format(
                    operation=escape_markdown_cell(operation),
                    requests=metrics.get("requests", 0),
                    successes=metrics.get("successes", 0),
                    failures=metrics.get("failures", 0),
                    retry_attempts=metrics.get("retryAttempts", metrics.get("retry_attempts", 0)),
                    retry_exhausted=metrics.get("retryExhausted", metrics.get("retry_exhausted", 0)),
                    circuit_opened=metrics.get("circuitOpened", metrics.get("circuit_opened", 0)),
                    circuit_rejections=metrics.get("circuitOpenRejections", metrics.get("circuit_open_rejections", 0)),
                    avg_latency=format_number(metrics.get("latencyAvgMs", metrics.get("latency_avg_ms"))),
                    p95_latency=format_number(metrics.get("latencyP95Ms", metrics.get("latency_p95_ms"))),
                    last_error=escape_markdown_cell(str(metrics.get("lastError", metrics.get("last_error", "-")) or "-")),
                )
            )

    lines.extend(
        [
            "",
            "## Suggested Evidence Note",
            "",
            "该快照用于答辩或论文附录展示 AI 解析链路的请求量、重试、熔断、延迟与外部内容异常统计，可作为系统韧性与可观测性实现的运行时证据。",
            "",
        ]
    )
    return "\n".join(lines)


def format_mapping(values: dict[str, Any]) -> str:
    if not values:
        return "n/a"
    return ", ".join(f"{key}={values[key]}" for key in sorted(values))


def format_number(value: Any) -> str:
    if value is None:
        return "n/a"
    return f"{float(value):.2f}"


def escape_markdown_cell(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ").strip()


if __name__ == "__main__":
    main()