from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from html import escape
from pathlib import Path
from typing import Any


PRIMARY_METRICS = ["recall@1", "recall@3", "mrr", "ndcg@10"]
FULL_METRICS = ["recall@1", "recall@3", "recall@5", "recall@10", "mrr", "ndcg@1", "ndcg@3", "ndcg@5", "ndcg@10"]
WITHOUT_COLOR = "#C65D3A"
WITH_COLOR = "#1F6B5B"
DELTA_COLOR = "#D89B2B"
GRID_COLOR = "#D8D3C8"
TEXT_COLOR = "#1D2433"
SUBTEXT_COLOR = "#5C667A"
BG_COLOR = "#FAF7F0"


@dataclass(frozen=True)
class SummaryPayload:
    summary_path: Path
    generated_at: str
    experiment_label: str
    candidate_count: int
    job_count: int
    dataset_summary: dict[str, Any]
    cohort_breakdowns: dict[str, Any]
    comparison_to_previous_experiment: dict[str, Any] | None
    artifacts: dict[str, str]
    without_dictionary: dict[str, float]
    with_dictionary: dict[str, float]

    @property
    def stem_suffix(self) -> str:
        return self.summary_path.stem.removeprefix("offline_eval_summary_")


@dataclass(frozen=True)
class ExperimentHistoryEntry:
    generated_at: str
    experiment_label: str
    candidate_count: int
    job_count: int
    manifest_hash: str
    with_dictionary: dict[str, float]
    without_dictionary: dict[str, float]
    highlights: list[str]


def main() -> None:
    parser = argparse.ArgumentParser(description="Render SVG and Markdown artifacts from offline evaluation summary JSON.")
    parser.add_argument(
        "--summary-file",
        type=Path,
        default=None,
        help="Optional summary JSON path. Defaults to the newest offline_eval_summary_*.json in tools/maintenance/output.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path(__file__).resolve().parent / "output",
        help="Directory for generated SVG and Markdown artifacts.",
    )
    args = parser.parse_args()

    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    summary_path = resolve_summary_path(args.summary_file, output_dir)
    payload = load_summary(summary_path)

    metrics_svg = output_dir / f"offline_eval_metrics_{payload.stem_suffix}.svg"
    delta_svg = output_dir / f"offline_eval_delta_{payload.stem_suffix}.svg"
    report_md = output_dir / f"offline_eval_report_{payload.stem_suffix}.md"
    history_md = output_dir / f"offline_eval_history_{payload.stem_suffix}.md"
    history_csv = output_dir / f"offline_eval_history_{payload.stem_suffix}.csv"
    thesis_digest_md = output_dir / f"offline_eval_thesis_digest_{payload.stem_suffix}.md"
    thesis_digest_csv = output_dir / f"offline_eval_thesis_digest_{payload.stem_suffix}.csv"
    history_entries = load_experiment_history(output_dir / "offline_eval_experiments.json")
    thesis_entries = select_thesis_digest_entries(history_entries)

    metrics_svg.write_text(render_primary_metrics_svg(payload), encoding="utf-8")
    delta_svg.write_text(render_delta_svg(payload), encoding="utf-8")
    report_md.write_text(render_markdown_report(payload, metrics_svg.name, delta_svg.name), encoding="utf-8")
    history_md.write_text(render_experiment_history_markdown(history_entries), encoding="utf-8")
    history_csv.write_text(render_experiment_history_csv(history_entries), encoding="utf-8")
    thesis_digest_md.write_text(render_thesis_digest_markdown(payload, thesis_entries), encoding="utf-8")
    thesis_digest_csv.write_text(render_thesis_digest_csv(thesis_entries), encoding="utf-8")

    print(
        json.dumps(
            {
                "summary": str(summary_path),
                "metricsSvg": str(metrics_svg),
                "deltaSvg": str(delta_svg),
                "reportMarkdown": str(report_md),
                "historyMarkdown": str(history_md),
                "historyCsv": str(history_csv),
                "thesisDigestMarkdown": str(thesis_digest_md),
                "thesisDigestCsv": str(thesis_digest_csv),
            },
            ensure_ascii=False,
            indent=2,
        )
    )


def resolve_summary_path(explicit_path: Path | None, output_dir: Path) -> Path:
    if explicit_path is not None:
        return explicit_path.resolve()
    candidates = sorted(output_dir.glob("offline_eval_summary_*.json"))
    if not candidates:
        raise FileNotFoundError("No offline_eval_summary_*.json files were found in the output directory.")
    return candidates[-1]


def load_summary(path: Path) -> SummaryPayload:
    raw = json.loads(path.read_text(encoding="utf-8"))
    modes = raw["modes"]
    return SummaryPayload(
        summary_path=path,
        generated_at=raw["generatedAt"],
        experiment_label=str(raw.get("experimentLabel", "baseline")),
        candidate_count=int(raw["candidateCount"]),
        job_count=int(raw["jobCount"]),
        dataset_summary=raw.get("datasetSummary", {}),
        cohort_breakdowns=raw.get("cohortBreakdowns", {}),
        comparison_to_previous_experiment=raw.get("comparisonToPreviousExperiment"),
        artifacts=raw.get("artifacts", {}),
        without_dictionary={key: float(value) for key, value in modes["without_dictionary"].items()},
        with_dictionary={key: float(value) for key, value in modes["with_dictionary"].items()},
    )


def load_experiment_history(path: Path) -> list[ExperimentHistoryEntry]:
    if not path.exists():
        return []

    raw = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, list):
        return []

    entries: list[ExperimentHistoryEntry] = []
    for item in sorted(raw, key=lambda candidate: candidate.get("generatedAt", ""), reverse=True):
        modes = item.get("modes", {})
        without_dictionary = modes.get("without_dictionary", {})
        with_dictionary = modes.get("with_dictionary", {})
        comparison = item.get("comparisonToPreviousExperiment") or {}
        highlights = comparison.get("highlights", []) if isinstance(comparison, dict) else []
        entries.append(
            ExperimentHistoryEntry(
                generated_at=str(item.get("generatedAt", "")),
                experiment_label=str(item.get("experimentLabel", "baseline")),
                candidate_count=int(item.get("candidateCount", 0)),
                job_count=int(item.get("jobCount", 0)),
                manifest_hash=str(item.get("datasetSummary", {}).get("manifestHash", "n/a")),
                without_dictionary={key: float(value) for key, value in without_dictionary.items()},
                with_dictionary={key: float(value) for key, value in with_dictionary.items()},
                highlights=[str(value) for value in highlights],
            )
        )
    return entries


def render_primary_metrics_svg(payload: SummaryPayload) -> str:
    width = 980
    height = 560
    margin_left = 90
    margin_right = 70
    margin_top = 90
    margin_bottom = 110
    chart_width = width - margin_left - margin_right
    chart_height = height - margin_top - margin_bottom
    group_width = chart_width / len(PRIMARY_METRICS)
    bar_width = 42
    gap = 10

    parts: list[str] = [svg_header(width, height)]
    parts.append(rect(0, 0, width, height, BG_COLOR))
    parts.append(text(width / 2, 40, "离线评测核心指标对比", 28, TEXT_COLOR, anchor="middle", weight="700"))
    parts.append(
        text(
            width / 2,
            68,
            f"实验 {escape(payload.experiment_label)} | 样本 {payload.candidate_count} × 岗位 {payload.job_count} | 来源 {escape(payload.summary_path.name)} | 生成 {escape(payload.generated_at)}",
            13,
            SUBTEXT_COLOR,
            anchor="middle",
        )
    )

    for grid_index in range(6):
        ratio = grid_index / 5
        y = margin_top + chart_height - ratio * chart_height
        label = f"{ratio:.1f}"
        parts.append(line(margin_left, y, width - margin_right, y, GRID_COLOR, 1))
        parts.append(text(margin_left - 18, y + 5, label, 12, SUBTEXT_COLOR, anchor="end"))

    parts.append(line(margin_left, margin_top, margin_left, margin_top + chart_height, TEXT_COLOR, 1.5))
    parts.append(line(margin_left, margin_top + chart_height, width - margin_right, margin_top + chart_height, TEXT_COLOR, 1.5))

    for index, metric in enumerate(PRIMARY_METRICS):
        group_left = margin_left + index * group_width
        center = group_left + group_width / 2
        without_value = payload.without_dictionary[metric]
        with_value = payload.with_dictionary[metric]
        without_height = without_value * chart_height
        with_height = with_value * chart_height
        without_x = center - gap / 2 - bar_width
        with_x = center + gap / 2
        without_y = margin_top + chart_height - without_height
        with_y = margin_top + chart_height - with_height

        parts.append(rect(without_x, without_y, bar_width, without_height, WITHOUT_COLOR, rx=8))
        parts.append(rect(with_x, with_y, bar_width, with_height, WITH_COLOR, rx=8))
        parts.append(text(without_x + bar_width / 2, without_y - 8, format_metric(without_value), 12, TEXT_COLOR, anchor="middle"))
        parts.append(text(with_x + bar_width / 2, with_y - 8, format_metric(with_value), 12, TEXT_COLOR, anchor="middle"))
        parts.append(text(center, margin_top + chart_height + 28, metric.upper(), 13, TEXT_COLOR, anchor="middle", weight="600"))

    legend_y = height - 34
    parts.append(rect(220, legend_y - 12, 18, 18, WITHOUT_COLOR, rx=4))
    parts.append(text(248, legend_y + 2, "without_dictionary", 13, TEXT_COLOR))
    parts.append(rect(450, legend_y - 12, 18, 18, WITH_COLOR, rx=4))
    parts.append(text(478, legend_y + 2, "with_dictionary", 13, TEXT_COLOR))
    parts.append("</svg>\n")
    return "".join(parts)


def render_delta_svg(payload: SummaryPayload) -> str:
    deltas = [(metric, payload.with_dictionary[metric] - payload.without_dictionary[metric]) for metric in FULL_METRICS]
    deltas = [item for item in deltas if abs(item[1]) > 1e-9]
    width = 980
    row_height = 38
    height = 130 + row_height * len(deltas)
    margin_left = 190
    margin_right = 80
    margin_top = 80
    chart_width = width - margin_left - margin_right
    max_delta = max(value for _, value in deltas) if deltas else 0.1

    parts: list[str] = [svg_header(width, height)]
    parts.append(rect(0, 0, width, height, BG_COLOR))
    parts.append(text(width / 2, 38, "词典标准化带来的绝对增益", 26, TEXT_COLOR, anchor="middle", weight="700"))
    parts.append(text(width / 2, 64, "横轴为 with_dictionary - without_dictionary 的绝对提升值", 13, SUBTEXT_COLOR, anchor="middle"))

    for index, (metric, delta) in enumerate(deltas):
        y = margin_top + index * row_height
        bar_width = 0 if max_delta <= 0 else chart_width * (delta / max_delta)
        parts.append(text(margin_left - 18, y + 20, metric.upper(), 13, TEXT_COLOR, anchor="end", weight="600"))
        parts.append(rect(margin_left, y + 6, chart_width, 16, "#EFE7D8", rx=8))
        parts.append(rect(margin_left, y + 6, bar_width, 16, DELTA_COLOR, rx=8))
        parts.append(text(margin_left + bar_width + 10, y + 20, f"+{delta:.4f}", 12, TEXT_COLOR))

    parts.append("</svg>\n")
    return "".join(parts)


def render_markdown_report(payload: SummaryPayload, metrics_svg_name: str, delta_svg_name: str) -> str:
    recall1_delta = payload.with_dictionary["recall@1"] - payload.without_dictionary["recall@1"]
    recall3_delta = payload.with_dictionary["recall@3"] - payload.without_dictionary["recall@3"]
    mrr_delta = payload.with_dictionary["mrr"] - payload.without_dictionary["mrr"]
    ndcg10_delta = payload.with_dictionary["ndcg@10"] - payload.without_dictionary["ndcg@10"]
    lines = [
        "# Offline Evaluation Visual Summary",
        "",
        f"- Summary source: {payload.summary_path.name}",
        f"- Experiment label: {payload.experiment_label}",
        f"- Dataset scale: {payload.candidate_count} candidates x {payload.job_count} jobs",
        f"- Generated at: {payload.generated_at}",
        "",
        "## Dataset Snapshot",
        "",
        f"- Fit levels: {format_count_map(payload.dataset_summary.get('fitLevelCounts', {}))}",
        f"- Target directions: {format_count_map(payload.dataset_summary.get('targetDirectionCounts', {}))}",
        f"- Scenario cohorts: {format_count_map(payload.dataset_summary.get('scenarioCohortCounts', {}))}",
        (
            "- Expected top-job labels per candidate: "
            f"min={payload.dataset_summary.get('expectedTopJobStats', {}).get('min', 0)}, "
            f"avg={payload.dataset_summary.get('expectedTopJobStats', {}).get('average', 0.0)}, "
            f"max={payload.dataset_summary.get('expectedTopJobStats', {}).get('max', 0)}"
        ),
        f"- Manifest hash: {payload.dataset_summary.get('manifestHash', 'n/a')}",
        f"- Jobs hash: {payload.dataset_summary.get('jobsHash', 'n/a')}",
        f"- Summary alias: {payload.artifacts.get('summary', payload.summary_path.name)}",
        "",
        "## Key Gains",
        "",
    ]
    for metric in PRIMARY_METRICS:
        without_value = payload.without_dictionary[metric]
        with_value = payload.with_dictionary[metric]
        delta = with_value - without_value
        lines.append(f"- {metric.upper()}: {without_value:.4f} -> {with_value:.4f} ({delta:+.4f})")

    lines.extend(
        [
            "",
            "## Scenario Cohorts",
            "",
        ]
    )

    if payload.cohort_breakdowns:
        for cohort_key, cohort in payload.cohort_breakdowns.items():
            without_metrics = cohort.get("modes", {}).get("without_dictionary", {})
            with_metrics = cohort.get("modes", {}).get("with_dictionary", {})
            if not without_metrics or not with_metrics:
                continue
            recall1_before = float(without_metrics.get("recall@1", 0.0))
            recall1_after = float(with_metrics.get("recall@1", 0.0))
            mrr_before = float(without_metrics.get("mrr", 0.0))
            mrr_after = float(with_metrics.get("mrr", 0.0))
            ndcg10_before = float(without_metrics.get("ndcg@10", 0.0))
            ndcg10_after = float(with_metrics.get("ndcg@10", 0.0))
            lines.extend(
                [
                    (
                        f"- {cohort.get('label', cohort_key)} (n={cohort.get('sampleCount', 0)}): "
                        f"Recall@1 {recall1_before:.4f} -> {recall1_after:.4f} ({recall1_after - recall1_before:+.4f}), "
                        f"MRR {mrr_before:.4f} -> {mrr_after:.4f} ({mrr_after - mrr_before:+.4f}), "
                        f"NDCG@10 {ndcg10_before:.4f} -> {ndcg10_after:.4f} ({ndcg10_after - ndcg10_before:+.4f})"
                    ),
                    f"  说明: {cohort.get('description', 'n/a')}",
                    f"  目标分布: {format_count_map(cohort.get('targetDirectionCounts', {}))}",
                ]
            )
    else:
        lines.append("- n/a")

    lines.extend(
        [
            "",
            "## Notable Changes Since Previous Run",
            "",
        ]
    )

    comparison = payload.comparison_to_previous_experiment or {}
    highlights = comparison.get("highlights", []) if isinstance(comparison, dict) else []
    if highlights:
        for item in highlights:
            lines.append(f"- {item}")
    else:
        lines.append("- n/a")

    lines.extend(
        [
            "",
            "## Cross-run Comparison",
            "",
        ]
    )

    if comparison:
        lines.append(
            f"- Previous run: {comparison.get('previousExperimentLabel', 'unknown')} @ {comparison.get('previousGeneratedAt', 'unknown')}"
        )
        dataset_scale_delta = comparison.get("datasetScaleDelta", {})
        lines.append(
            f"- Dataset scale delta: candidates {format_signed_number(dataset_scale_delta.get('candidateCount', 0))}, jobs {format_signed_number(dataset_scale_delta.get('jobCount', 0))}"
        )

        mode_deltas = comparison.get("modeMetricDeltas", {})
        with_deltas = mode_deltas.get("with_dictionary", {})
        gain_deltas = mode_deltas.get("dictionary_gain", {})
        lines.append(
            "- With-dictionary metric delta: "
            + format_metric_delta_map(with_deltas)
        )
        lines.append(
            "- Dictionary lift delta: "
            + format_metric_delta_map(gain_deltas)
        )

        cohort_deltas = comparison.get("cohortMetricDeltas", {})
        if cohort_deltas:
            lines.append("- Cohort deltas:")
            for cohort_key in sorted(cohort_deltas):
                cohort_delta = cohort_deltas[cohort_key]
                lines.append(
                    (
                        f"  {cohort_delta.get('label', cohort_key)}: sample Δ {format_signed_number(cohort_delta.get('sampleCountDelta', 0))}, "
                        f"with_dictionary {format_metric_delta_map(cohort_delta.get('with_dictionary', {}))}, "
                        f"lift {format_metric_delta_map(cohort_delta.get('dictionary_gain', {}))}"
                    )
                )
    else:
        lines.append("- n/a")

    lines.extend(
        [
            "",
            "## Artifacts",
            "",
            f"- Summary JSON: {payload.artifacts.get('summary', payload.summary_path.name)}",
            f"- Core metrics chart: {metrics_svg_name}",
            f"- Delta chart: {delta_svg_name}",
            f"- Experiment history table: offline_eval_history_{payload.stem_suffix}.md",
            f"- Experiment history CSV: offline_eval_history_{payload.stem_suffix}.csv",
            "",
            "## Suggested Thesis Caption",
            "",
            (
                f"在当前 {payload.candidate_count} 名候选人、{payload.job_count} 个岗位的离线合成数据集上，"
                f"技能词典标准化相较未标准化策略，已将 Recall@1 提升 {recall1_delta:.4f}、"
                f"Recall@3 提升 {recall3_delta:.4f}、MRR 提升 {mrr_delta:.4f}、"
                f"NDCG@10 提升 {ndcg10_delta:.4f}，说明其不仅改善首选岗位识别，也改善了 Top-3 召回与整体排序质量。"
            ),
            "",
        ]
    )
    return "\n".join(lines)


def render_experiment_history_markdown(entries: list[ExperimentHistoryEntry]) -> str:
    lines = [
        "# Offline Evaluation Experiment History",
        "",
        "| Generated At | Experiment | Scale | Manifest | With R@1 | With MRR | Lift R@1 | Lift MRR | Highlights |",
        "| --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- |",
    ]
    if not entries:
        lines.append("| n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |")
        return "\n".join(lines) + "\n"

    for entry in entries:
        with_recall1 = entry.with_dictionary.get("recall@1", 0.0)
        with_mrr = entry.with_dictionary.get("mrr", 0.0)
        lift_recall1 = with_recall1 - entry.without_dictionary.get("recall@1", 0.0)
        lift_mrr = with_mrr - entry.without_dictionary.get("mrr", 0.0)
        highlights = " / ".join(entry.highlights) if entry.highlights else "-"
        lines.append(
            "| {generated_at} | {label} | {scale} | {manifest} | {with_recall1} | {with_mrr} | {lift_recall1} | {lift_mrr} | {highlights} |".format(
                generated_at=escape_markdown_cell(entry.generated_at),
                label=escape_markdown_cell(entry.experiment_label),
                scale=f"{entry.candidate_count}x{entry.job_count}",
                manifest=escape_markdown_cell(short_hash(entry.manifest_hash)),
                with_recall1=f"{with_recall1:.4f}",
                with_mrr=f"{with_mrr:.4f}",
                lift_recall1=f"{lift_recall1:+.4f}",
                lift_mrr=f"{lift_mrr:+.4f}",
                highlights=escape_markdown_cell(highlights),
            )
        )

    return "\n".join(lines) + "\n"


def render_experiment_history_csv(entries: list[ExperimentHistoryEntry]) -> str:
    header = [
        "generatedAt",
        "experimentLabel",
        "candidateCount",
        "jobCount",
        "manifestHash",
        "withRecallAt1",
        "withMrr",
        "liftRecallAt1",
        "liftMrr",
        "highlights",
    ]
    lines = [",".join(header)]
    for entry in entries:
        with_recall1 = entry.with_dictionary.get("recall@1", 0.0)
        with_mrr = entry.with_dictionary.get("mrr", 0.0)
        lift_recall1 = with_recall1 - entry.without_dictionary.get("recall@1", 0.0)
        lift_mrr = with_mrr - entry.without_dictionary.get("mrr", 0.0)
        lines.append(
            ",".join(
                [
                    csv_cell(entry.generated_at),
                    csv_cell(entry.experiment_label),
                    csv_cell(str(entry.candidate_count)),
                    csv_cell(str(entry.job_count)),
                    csv_cell(entry.manifest_hash),
                    csv_cell(f"{with_recall1:.4f}"),
                    csv_cell(f"{with_mrr:.4f}"),
                    csv_cell(f"{lift_recall1:+.4f}"),
                    csv_cell(f"{lift_mrr:+.4f}"),
                    csv_cell(" / ".join(entry.highlights)),
                ]
            )
        )
    return "\n".join(lines) + "\n"


def select_thesis_digest_entries(entries: list[ExperimentHistoryEntry], max_entries: int = 6) -> list[ExperimentHistoryEntry]:
    if not entries:
        return []

    latest_by_manifest: dict[str, ExperimentHistoryEntry] = {}
    for entry in entries:
        latest_by_manifest.setdefault(entry.manifest_hash, entry)

    selected = sorted(latest_by_manifest.values(), key=lambda item: item.generated_at)
    if len(selected) <= max_entries:
        return selected

    keep_middle = max_entries - 2
    middle = selected[1:-1]
    step = max(1, len(middle) / max(1, keep_middle))
    sampled_middle = [middle[min(len(middle) - 1, int(index * step))] for index in range(keep_middle)]

    deduped_middle: list[ExperimentHistoryEntry] = []
    seen_keys: set[tuple[str, str]] = set()
    for entry in sampled_middle:
        key = (entry.generated_at, entry.manifest_hash)
        if key not in seen_keys:
            seen_keys.add(key)
            deduped_middle.append(entry)

    return [selected[0], *deduped_middle[:keep_middle], selected[-1]]


def render_thesis_digest_markdown(payload: SummaryPayload, entries: list[ExperimentHistoryEntry]) -> str:
    lines = [
        "# Offline Evaluation Thesis Digest",
        "",
        "本摘要仅保留适合写入论文正文或答辩主讲稿的关键实验节点，避免直接贴全量 history 表。",
        "",
        "## Latest Headline",
        "",
        (
            f"- 最新有效实验 {payload.experiment_label} 基于 {payload.candidate_count} 名候选人、{payload.job_count} 个岗位，"
            f"with_dictionary 达到 Recall@1 {payload.with_dictionary['recall@1']:.4f}、"
            f"Recall@3 {payload.with_dictionary['recall@3']:.4f}、MRR {payload.with_dictionary['mrr']:.4f}、"
            f"NDCG@10 {payload.with_dictionary['ndcg@10']:.4f}。"
        ),
        (
            f"- 相比未使用词典标准化策略，Recall@1 提升 {payload.with_dictionary['recall@1'] - payload.without_dictionary['recall@1']:+.4f}，"
            f"MRR 提升 {payload.with_dictionary['mrr'] - payload.without_dictionary['mrr']:+.4f}。"
        ),
        "",
        "## Selected Milestones",
        "",
        "| Stage | Experiment | Scale | Manifest | With R@1 | With MRR | Lift R@1 | Lift MRR | Key Change |",
        "| --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- |",
    ]

    if not entries:
        lines.append("| n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |")
    else:
        for index, entry in enumerate(entries, start=1):
            with_recall1 = entry.with_dictionary.get("recall@1", 0.0)
            with_mrr = entry.with_dictionary.get("mrr", 0.0)
            lift_recall1 = with_recall1 - entry.without_dictionary.get("recall@1", 0.0)
            lift_mrr = with_mrr - entry.without_dictionary.get("mrr", 0.0)
            key_change = entry.highlights[0] if entry.highlights else "-"
            lines.append(
                "| {stage} | {label} | {scale} | {manifest} | {with_recall1} | {with_mrr} | {lift_recall1} | {lift_mrr} | {key_change} |".format(
                    stage=f"M{index}",
                    label=escape_markdown_cell(entry.experiment_label),
                    scale=f"{entry.candidate_count}x{entry.job_count}",
                    manifest=escape_markdown_cell(short_hash(entry.manifest_hash)),
                    with_recall1=f"{with_recall1:.4f}",
                    with_mrr=f"{with_mrr:.4f}",
                    lift_recall1=f"{lift_recall1:+.4f}",
                    lift_mrr=f"{lift_mrr:+.4f}",
                    key_change=escape_markdown_cell(key_change),
                )
            )

    lines.extend(
        [
            "",
            "## Recommended Thesis Wording",
            "",
            (
                f"实验结果表明，随着离线评测样本逐步扩展并引入更贴近真实别名扰动的候选人简历，"
                f"技能词典标准化策略在最新 {payload.candidate_count}x{payload.job_count} 数据规模下仍保持稳定增益："
                f"Recall@1 从 {payload.without_dictionary['recall@1']:.4f} 提升至 {payload.with_dictionary['recall@1']:.4f}，"
                f"MRR 从 {payload.without_dictionary['mrr']:.4f} 提升至 {payload.with_dictionary['mrr']:.4f}，"
                f"说明该策略不仅提升首选岗位命中率，也改善整体排序质量。"
            ),
            "",
        ]
    )
    return "\n".join(lines)


def render_thesis_digest_csv(entries: list[ExperimentHistoryEntry]) -> str:
    header = [
        "stage",
        "generatedAt",
        "experimentLabel",
        "candidateCount",
        "jobCount",
        "manifestHash",
        "withRecallAt1",
        "withMrr",
        "liftRecallAt1",
        "liftMrr",
        "keyChange",
    ]
    lines = [",".join(header)]
    for index, entry in enumerate(entries, start=1):
        with_recall1 = entry.with_dictionary.get("recall@1", 0.0)
        with_mrr = entry.with_dictionary.get("mrr", 0.0)
        lift_recall1 = with_recall1 - entry.without_dictionary.get("recall@1", 0.0)
        lift_mrr = with_mrr - entry.without_dictionary.get("mrr", 0.0)
        lines.append(
            ",".join(
                [
                    csv_cell(f"M{index}"),
                    csv_cell(entry.generated_at),
                    csv_cell(entry.experiment_label),
                    csv_cell(str(entry.candidate_count)),
                    csv_cell(str(entry.job_count)),
                    csv_cell(entry.manifest_hash),
                    csv_cell(f"{with_recall1:.4f}"),
                    csv_cell(f"{with_mrr:.4f}"),
                    csv_cell(f"{lift_recall1:+.4f}"),
                    csv_cell(f"{lift_mrr:+.4f}"),
                    csv_cell(entry.highlights[0] if entry.highlights else ""),
                ]
            )
        )
    return "\n".join(lines) + "\n"


def format_count_map(values: dict[str, Any]) -> str:
    if not values:
        return "n/a"
    return ", ".join(f"{key}={values[key]}" for key in sorted(values))


def svg_header(width: int, height: int) -> str:
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-label="offline evaluation chart">'


def rect(x: float, y: float, width: float, height: float, fill: str, rx: float = 0) -> str:
    return f'<rect x="{x:.2f}" y="{y:.2f}" width="{width:.2f}" height="{height:.2f}" rx="{rx:.2f}" fill="{fill}" />'


def line(x1: float, y1: float, x2: float, y2: float, stroke: str, stroke_width: float) -> str:
    return f'<line x1="{x1:.2f}" y1="{y1:.2f}" x2="{x2:.2f}" y2="{y2:.2f}" stroke="{stroke}" stroke-width="{stroke_width:.2f}" />'


def text(x: float, y: float, value: str, size: int, fill: str, anchor: str = "start", weight: str = "400") -> str:
    safe = escape(value)
    return (
        f'<text x="{x:.2f}" y="{y:.2f}" font-size="{size}" fill="{fill}" '
        f'font-family="Segoe UI, PingFang SC, Microsoft YaHei, sans-serif" text-anchor="{anchor}" font-weight="{weight}">{safe}</text>'
    )


def format_metric(value: float) -> str:
    return f"{value:.4f}".rstrip("0").rstrip(".")


def format_signed_number(value: Any) -> str:
    numeric = int(value or 0)
    return f"{numeric:+d}"


def format_metric_delta_map(values: dict[str, Any]) -> str:
    if not values:
        return "n/a"
    return ", ".join(f"{key}={float(values.get(key, 0.0)):+.4f}" for key in PRIMARY_METRICS)


def short_hash(value: str) -> str:
    if not value or value == "n/a":
        return "n/a"
    return value[:12]


def escape_markdown_cell(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ").strip()


def csv_cell(value: str) -> str:
    escaped = value.replace('"', '""')
    return f'"{escaped}"'


if __name__ == "__main__":
    main()