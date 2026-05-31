from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import re
from collections import Counter
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any


TOKEN_SPLIT_REGEX = re.compile(r"[^a-z0-9+#.]+")
STOP_WORDS = {
    "a", "an", "and", "are", "as", "at", "be", "build", "by", "for", "from", "in", "into",
    "is", "of", "on", "or", "that", "the", "to", "with", "we", "you", "our", "will", "can",
}
EMBEDDING_DIMENSIONS = 1536
DEFAULT_TOP_KS = (1, 3, 5, 10)
COMPARISON_METRICS = ("recall@1", "recall@3", "mrr", "ndcg@10")
DEFAULT_WEIGHTS = {
    "skill": 35.0,
    "experience": 25.0,
    "education": 10.0,
    "semantic": 30.0,
}
SCENARIO_COHORTS = {
    "alias_challenge": {
        "label": "Alias Challenge",
        "description": "Combine alias-heavy and alias-rich samples to measure how strongly dictionary normalization helps messy skill naming.",
    },
    "alias_heavy": {
        "label": "Alias-heavy",
        "description": "Samples dominated by abbreviated or alternate spellings that stress exact skill matching.",
    },
    "alias_rich": {
        "label": "Alias-rich",
        "description": "Samples mixing natural alternate terms with broader narrative noise.",
    },
    "hard_negative": {
        "label": "Hard-negative",
        "description": "Samples that contain nearby but misleading terminology and should resist false-positive ranking.",
    },
    "borderline": {
        "label": "Borderline",
        "description": "Samples near the decision boundary where ranking quality is sensitive to weak signals.",
    },
    "baseline": {
        "label": "Baseline",
        "description": "Samples without explicit alias or adversarial scenario tags, used as the reference slice.",
    },
}


@dataclass(frozen=True)
class CandidateSample:
    sample_id: str
    candidate_name: str
    target_direction: str
    fit_level: str
    expected_top_jobs: list[str]
    cohort_tags: tuple[str, ...]
    truth: dict[str, Any]


@dataclass(frozen=True)
class RankedJob:
    rank: int
    job_id: str
    title: str
    match_score: float
    skill_score: float
    experience_score: float
    education_score: float
    semantic_score: float
    relevant: bool


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate offline recommendation quality on the synthetic dataset.")
    parser.add_argument(
        "--dataset-dir",
        type=Path,
        default=Path(__file__).resolve().parents[2] / "doc" / "synthetic-dataset",
        help="Synthetic dataset directory containing jobs.json, manifest.json, and truth/.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path(__file__).resolve().parent / "output",
        help="Directory used for CSV and JSON reports.",
    )
    parser.add_argument(
        "--top-k",
        type=str,
        default=",".join(str(value) for value in DEFAULT_TOP_KS),
        help="Comma-separated cutoff list, for example 1,3,5,10.",
    )
    parser.add_argument(
        "--experiment-label",
        type=str,
        default="baseline",
        help="Short human-readable label for this evaluation run.",
    )
    args = parser.parse_args()

    top_ks = tuple(sorted({int(part.strip()) for part in args.top_k.split(",") if part.strip()}))
    dataset_dir = args.dataset_dir.resolve()
    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs = load_json(dataset_dir / "jobs.json")
    manifest = load_json(dataset_dir / "manifest.json")
    candidates = load_candidates(dataset_dir, manifest)
    dataset_summary = summarize_dataset(manifest, jobs, dataset_dir)
    alias_normalizer = build_skill_normalizer(include_aliases=True)
    identity_normalizer: dict[str, str] = {}

    evaluations = {
        "without_dictionary": evaluate_mode(candidates, jobs, identity_normalizer, top_ks),
        "with_dictionary": evaluate_mode(candidates, jobs, alias_normalizer, top_ks),
    }
    cohort_breakdowns = build_cohort_breakdowns(
        candidates,
        jobs,
        {
            "without_dictionary": identity_normalizer,
            "with_dictionary": alias_normalizer,
        },
        top_ks,
    )

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    artifact_files = {
        "summary": f"offline_eval_summary_{timestamp}.json",
        "withoutDictionaryCsv": f"offline_eval_without_dictionary_{timestamp}.csv",
        "withDictionaryCsv": f"offline_eval_with_dictionary_{timestamp}.csv",
    }
    summary_payload = {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "experimentLabel": args.experiment_label.strip() or "baseline",
        "datasetDir": str(dataset_dir),
        "datasetSummary": dataset_summary,
        "jobCount": len(jobs),
        "candidateCount": len(candidates),
        "topKs": list(top_ks),
        "weights": DEFAULT_WEIGHTS,
        "artifacts": artifact_files,
        "modes": {mode: payload["metrics"] for mode, payload in evaluations.items()},
        "cohortBreakdowns": cohort_breakdowns,
    }
    comparison_to_previous = build_previous_experiment_comparison(output_dir, summary_payload)
    if comparison_to_previous:
        summary_payload["comparisonToPreviousExperiment"] = comparison_to_previous

    summary_path = output_dir / artifact_files["summary"]
    write_json(summary_path, summary_payload)

    for mode, payload in evaluations.items():
        csv_name = artifact_files["withDictionaryCsv"] if mode == "with_dictionary" else artifact_files["withoutDictionaryCsv"]
        write_rankings_csv(output_dir / csv_name, payload["rankings"])

    upsert_experiment_index(output_dir, summary_payload)
    write_json(output_dir / "offline_eval_latest.json", summary_payload)

    print(json.dumps(summary_payload, ensure_ascii=False, indent=2))


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_candidates(dataset_dir: Path, manifest: list[dict[str, Any]]) -> list[CandidateSample]:
    candidates: list[CandidateSample] = []
    for item in manifest:
        truth_path = dataset_dir / item["truthFile"]
        truth = load_json(truth_path)
        candidates.append(
            CandidateSample(
                sample_id=item["sampleId"],
                candidate_name=item["candidateName"],
                target_direction=item["targetDirection"],
                fit_level=item["fitLevel"],
                expected_top_jobs=list(item.get("expectedTopJobs", [])),
                cohort_tags=derive_cohort_tags(item),
                truth=truth,
            )
        )
    return candidates


def summarize_dataset(manifest: list[dict[str, Any]], jobs: list[dict[str, Any]], dataset_dir: Path) -> dict[str, Any]:
    target_direction_counts = Counter(item.get("targetDirection", "unknown") for item in manifest)
    fit_level_counts = Counter(item.get("fitLevel", "unknown") for item in manifest)
    scenario_cohort_counts = Counter()
    for item in manifest:
        for tag in derive_cohort_tags(item):
            if tag in SCENARIO_COHORTS:
                scenario_cohort_counts[tag] += 1
    expected_top_job_sizes = [len(item.get("expectedTopJobs", [])) for item in manifest]

    return {
        "manifestHash": sha256_file(dataset_dir / "manifest.json"),
        "jobsHash": sha256_file(dataset_dir / "jobs.json"),
        "truthFileCount": len(list((dataset_dir / "truth").glob("*.json"))),
        "targetDirectionCounts": dict(sorted(target_direction_counts.items())),
        "fitLevelCounts": dict(sorted(fit_level_counts.items())),
        "scenarioCohortCounts": {key: scenario_cohort_counts[key] for key in SCENARIO_COHORTS if scenario_cohort_counts.get(key, 0) > 0},
        "expectedTopJobStats": {
            "min": min(expected_top_job_sizes) if expected_top_job_sizes else 0,
            "max": max(expected_top_job_sizes) if expected_top_job_sizes else 0,
            "average": round(mean(expected_top_job_sizes), 4) if expected_top_job_sizes else 0.0,
        },
        "jobTitles": [job["title"] for job in jobs],
    }


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def upsert_experiment_index(output_dir: Path, summary_payload: dict[str, Any]) -> None:
    index_path = output_dir / "offline_eval_experiments.json"
    existing_runs: list[dict[str, Any]] = []
    if index_path.exists():
        existing_runs = load_json(index_path)

    run_entry = {
        "generatedAt": summary_payload["generatedAt"],
        "experimentLabel": summary_payload["experimentLabel"],
        "candidateCount": summary_payload["candidateCount"],
        "jobCount": summary_payload["jobCount"],
        "datasetSummary": {
            "manifestHash": summary_payload["datasetSummary"].get("manifestHash"),
            "jobsHash": summary_payload["datasetSummary"].get("jobsHash"),
            "truthFileCount": summary_payload["datasetSummary"].get("truthFileCount"),
            "fitLevelCounts": summary_payload["datasetSummary"].get("fitLevelCounts", {}),
            "targetDirectionCounts": summary_payload["datasetSummary"].get("targetDirectionCounts", {}),
            "scenarioCohortCounts": summary_payload["datasetSummary"].get("scenarioCohortCounts", {}),
        },
        "artifacts": summary_payload["artifacts"],
        "modes": summary_payload["modes"],
        "cohortBreakdowns": {
            key: {
                "label": value.get("label"),
                "sampleCount": value.get("sampleCount"),
                "modes": value.get("modes", {}),
            }
            for key, value in summary_payload.get("cohortBreakdowns", {}).items()
        },
        "comparisonToPreviousExperiment": summary_payload.get("comparisonToPreviousExperiment"),
    }

    filtered_runs = [run for run in existing_runs if run.get("artifacts", {}).get("summary") != run_entry["artifacts"]["summary"]]
    filtered_runs.append(run_entry)
    filtered_runs.sort(key=lambda item: item.get("generatedAt", ""))
    write_json(index_path, filtered_runs)


def build_skill_normalizer(include_aliases: bool) -> dict[str, str]:
    repo_root = Path(__file__).resolve().parents[2]
    migration_paths = [
        repo_root / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V12__create_skill_dictionary.sql",
        repo_root / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V16__expand_professional_skill_dictionary.sql",
    ]
    normalizer: dict[str, str] = {}

    for path in migration_paths:
        if not path.exists():
            continue
        sql = path.read_text(encoding="utf-8")
        for canonical_name, aliases in parse_skill_dictionary_entries(sql):
            canonical_tokens = " ".join(tokenize(canonical_name))
            if not canonical_tokens:
                continue
            normalizer[canonical_tokens] = canonical_tokens
            if include_aliases:
                for alias in aliases:
                    alias_tokens = " ".join(tokenize(alias))
                    if alias_tokens:
                        normalizer[alias_tokens] = canonical_tokens

    return normalizer


def parse_skill_dictionary_entries(sql: str) -> list[tuple[str, list[str]]]:
    entries: dict[str, list[str]] = {}
    update_pattern = re.compile(
        r"aliases\s*=\s*'(?P<aliases>\[[^']*\])'.*?where\s+lower\(name\)\s*=\s*'(?P<name>[^']+)'",
        re.IGNORECASE,
    )
    insert_pattern = re.compile(
        r"select\s+cast\('[^']+'\s+as\s+uuid\),\s*'(?P<name>[^']+)',\s*'[^']+',\s*'(?P<aliases>\[[^']*\])',\s*true",
        re.IGNORECASE,
    )

    for match in update_pattern.finditer(sql):
        name = match.group("name")
        aliases = json.loads(match.group("aliases"))
        entries[name] = [str(alias) for alias in aliases]

    for match in insert_pattern.finditer(sql):
        name = match.group("name")
        aliases = json.loads(match.group("aliases"))
        entries.setdefault(name, []).extend(str(alias) for alias in aliases)

    return [(name, aliases) for name, aliases in entries.items()]


def evaluate_mode(
    candidates: list[CandidateSample],
    jobs: list[dict[str, Any]],
    normalizer: dict[str, str],
    top_ks: tuple[int, ...],
) -> dict[str, Any]:
    rankings_by_candidate: list[dict[str, Any]] = []
    recall_buckets = {k: [] for k in top_ks}
    ndcg_buckets = {k: [] for k in top_ks}
    reciprocal_ranks: list[float] = []

    for candidate in candidates:
        ranked_jobs = rank_jobs_for_candidate(candidate, jobs, normalizer)
        rankings_by_candidate.append(
            {
                "sampleId": candidate.sample_id,
                "candidateName": candidate.candidate_name,
                "targetDirection": candidate.target_direction,
                "fitLevel": candidate.fit_level,
                "cohortTags": list(candidate.cohort_tags),
                "expectedTopJobs": candidate.expected_top_jobs,
                "predictions": [job.__dict__ for job in ranked_jobs],
            }
        )

        relevance = [1 if ranked.relevant else 0 for ranked in ranked_jobs]
        relevant_total = max(len(candidate.expected_top_jobs), 1)
        reciprocal_ranks.append(first_relevant_rr(relevance))
        for k in top_ks:
            effective_k = min(k, len(ranked_jobs))
            hits = sum(relevance[:effective_k])
            recall_buckets[k].append(hits / relevant_total)
            ndcg_buckets[k].append(ndcg_at_k(relevance, effective_k, relevant_total))

    metrics: dict[str, float] = {}
    for k in top_ks:
        metrics[f"recall@{k}"] = round(mean(recall_buckets[k]), 4)
        metrics[f"ndcg@{k}"] = round(mean(ndcg_buckets[k]), 4)
    metrics["mrr"] = round(mean(reciprocal_ranks), 4)

    return {
        "metrics": metrics,
        "rankings": rankings_by_candidate,
    }


def derive_cohort_tags(item: dict[str, Any]) -> tuple[str, ...]:
    target_direction = str(item.get("targetDirection", "")).lower()
    tags: list[str] = []
    has_special_scenario = False

    if "alias-heavy" in target_direction:
        tags.extend(["alias_heavy", "alias_challenge"])
        has_special_scenario = True
    if "alias-rich" in target_direction:
        tags.extend(["alias_rich", "alias_challenge"])
        has_special_scenario = True
    if "hard-negative" in target_direction:
        tags.append("hard_negative")
        has_special_scenario = True
    if "borderline" in target_direction:
        tags.append("borderline")
        has_special_scenario = True
    if not has_special_scenario:
        tags.append("baseline")

    fit_level = str(item.get("fitLevel", "unknown")).strip().lower() or "unknown"
    tags.append(f"fit_{fit_level}")
    return tuple(dict.fromkeys(tags))


def build_cohort_breakdowns(
    candidates: list[CandidateSample],
    jobs: list[dict[str, Any]],
    normalizers: dict[str, dict[str, str]],
    top_ks: tuple[int, ...],
) -> dict[str, Any]:
    breakdowns: dict[str, Any] = {}

    for cohort_key, meta in SCENARIO_COHORTS.items():
        cohort_candidates = [candidate for candidate in candidates if cohort_key in candidate.cohort_tags]
        if not cohort_candidates:
            continue

        breakdowns[cohort_key] = {
            "label": meta["label"],
            "description": meta["description"],
            "sampleCount": len(cohort_candidates),
            "sampleIds": [candidate.sample_id for candidate in cohort_candidates],
            "targetDirectionCounts": dict(sorted(Counter(candidate.target_direction for candidate in cohort_candidates).items())),
            "fitLevelCounts": dict(sorted(Counter(candidate.fit_level for candidate in cohort_candidates).items())),
            "modes": {
                mode: evaluate_mode(cohort_candidates, jobs, normalizer, top_ks)["metrics"]
                for mode, normalizer in normalizers.items()
            },
        }

    return breakdowns


def build_previous_experiment_comparison(output_dir: Path, current_summary: dict[str, Any]) -> dict[str, Any] | None:
    index_path = output_dir / "offline_eval_experiments.json"
    if not index_path.exists():
        return None

    existing_runs = load_json(index_path)
    if not isinstance(existing_runs, list) or not existing_runs:
        return None

    previous_run = select_previous_meaningful_run(existing_runs, current_summary)
    if previous_run is None:
        return None

    previous_modes = previous_run.get("modes", {})
    current_modes = current_summary.get("modes", {})

    comparison = {
        "previousExperimentLabel": previous_run.get("experimentLabel", "unknown"),
        "previousGeneratedAt": previous_run.get("generatedAt"),
        "datasetScaleDelta": {
            "candidateCount": int(current_summary.get("candidateCount", 0)) - int(previous_run.get("candidateCount", 0)),
            "jobCount": int(current_summary.get("jobCount", 0)) - int(previous_run.get("jobCount", 0)),
        },
        "modeMetricDeltas": {
            "without_dictionary": diff_metric_maps(previous_modes.get("without_dictionary", {}), current_modes.get("without_dictionary", {})),
            "with_dictionary": diff_metric_maps(previous_modes.get("with_dictionary", {}), current_modes.get("with_dictionary", {})),
            "dictionary_gain": diff_metric_maps(
                build_gain_metrics(previous_modes),
                build_gain_metrics(current_modes),
            ),
        },
        "cohortMetricDeltas": build_cohort_metric_deltas(
            previous_run.get("cohortBreakdowns", {}),
            current_summary.get("cohortBreakdowns", {}),
        ),
    }
    comparison["highlights"] = build_comparison_highlights(comparison)

    return comparison


def select_previous_meaningful_run(existing_runs: list[dict[str, Any]], current_summary: dict[str, Any]) -> dict[str, Any] | None:
    for run in sorted(existing_runs, key=lambda item: item.get("generatedAt", ""), reverse=True):
        if not is_effectively_same_experiment(run, current_summary):
            return run
    return None


def is_effectively_same_experiment(previous_run: dict[str, Any], current_summary: dict[str, Any]) -> bool:
    previous_dataset = previous_run.get("datasetSummary", {})
    current_dataset = current_summary.get("datasetSummary", {})

    if previous_run.get("candidateCount") != current_summary.get("candidateCount"):
        return False
    if previous_run.get("jobCount") != current_summary.get("jobCount"):
        return False
    if previous_dataset.get("manifestHash") != current_dataset.get("manifestHash"):
        return False
    if previous_dataset.get("jobsHash") != current_dataset.get("jobsHash"):
        return False
    if previous_run.get("modes") != current_summary.get("modes"):
        return False
    if previous_run.get("cohortBreakdowns", {}) != simplify_cohort_breakdowns(current_summary.get("cohortBreakdowns", {})):
        return False
    return True


def simplify_cohort_breakdowns(cohort_breakdowns: dict[str, Any]) -> dict[str, Any]:
    simplified: dict[str, Any] = {}
    for key, value in cohort_breakdowns.items():
        simplified[key] = {
            "label": value.get("label"),
            "sampleCount": value.get("sampleCount"),
            "modes": value.get("modes", {}),
        }
    return simplified


def build_gain_metrics(modes: dict[str, Any]) -> dict[str, float]:
    without_metrics = modes.get("without_dictionary", {}) if isinstance(modes, dict) else {}
    with_metrics = modes.get("with_dictionary", {}) if isinstance(modes, dict) else {}
    gains: dict[str, float] = {}
    for metric in COMPARISON_METRICS:
        gains[metric] = round(float(with_metrics.get(metric, 0.0)) - float(without_metrics.get(metric, 0.0)), 4)
    return gains


def diff_metric_maps(previous_metrics: dict[str, Any], current_metrics: dict[str, Any]) -> dict[str, float]:
    deltas: dict[str, float] = {}
    for metric in COMPARISON_METRICS:
        deltas[metric] = round(float(current_metrics.get(metric, 0.0)) - float(previous_metrics.get(metric, 0.0)), 4)
    return deltas


def build_cohort_metric_deltas(previous_cohorts: dict[str, Any], current_cohorts: dict[str, Any]) -> dict[str, Any]:
    deltas: dict[str, Any] = {}
    for cohort_key, current_cohort in current_cohorts.items():
        previous_cohort = previous_cohorts.get(cohort_key)
        if not previous_cohort:
            continue

        deltas[cohort_key] = {
            "label": current_cohort.get("label", cohort_key),
            "sampleCountDelta": int(current_cohort.get("sampleCount", 0)) - int(previous_cohort.get("sampleCount", 0)),
            "with_dictionary": diff_metric_maps(
                previous_cohort.get("modes", {}).get("with_dictionary", {}),
                current_cohort.get("modes", {}).get("with_dictionary", {}),
            ),
            "dictionary_gain": diff_metric_maps(
                build_gain_metrics(previous_cohort.get("modes", {})),
                build_gain_metrics(current_cohort.get("modes", {})),
            ),
        }

    return deltas


def build_comparison_highlights(comparison: dict[str, Any]) -> list[str]:
    highlights: list[str] = []
    dataset_scale_delta = comparison.get("datasetScaleDelta", {})
    candidate_delta = int(dataset_scale_delta.get("candidateCount", 0))
    job_delta = int(dataset_scale_delta.get("jobCount", 0))
    if candidate_delta != 0 or job_delta != 0:
        highlights.append(f"样本规模较上一轮变化：候选人 {candidate_delta:+d}、岗位 {job_delta:+d}。")

    gain_delta = comparison.get("modeMetricDeltas", {}).get("dictionary_gain", {})
    top_gain_metric = max(
        COMPARISON_METRICS,
        key=lambda metric: abs(float(gain_delta.get(metric, 0.0))),
        default=None,
    )
    if top_gain_metric is not None:
        top_gain_value = float(gain_delta.get(top_gain_metric, 0.0))
        highlights.append(f"整体词典增益变化最明显的指标是 {top_gain_metric}，相较上一轮 {top_gain_value:+.4f}。")

    cohort_deltas = comparison.get("cohortMetricDeltas", {})
    sample_expansions = [
        (value.get("label", key), int(value.get("sampleCountDelta", 0)))
        for key, value in cohort_deltas.items()
        if int(value.get("sampleCountDelta", 0)) > 0
    ]
    sample_expansions.sort(key=lambda item: item[1], reverse=True)
    if sample_expansions:
        expansion_summary = "，".join(f"{label} {delta:+d}" for label, delta in sample_expansions[:2])
        highlights.append(f"本轮新增样本主要集中在 {expansion_summary}。")

    top_cohort_positive_label = None
    top_cohort_positive_metric = None
    top_cohort_positive_value = 0.0
    top_cohort_negative_label = None
    top_cohort_negative_metric = None
    top_cohort_negative_value = 0.0
    for key, value in cohort_deltas.items():
        for metric in COMPARISON_METRICS:
            candidate_value = float(value.get("dictionary_gain", {}).get(metric, 0.0))
            if candidate_value > top_cohort_positive_value:
                top_cohort_positive_label = value.get("label", key)
                top_cohort_positive_metric = metric
                top_cohort_positive_value = candidate_value
            if candidate_value < top_cohort_negative_value:
                top_cohort_negative_label = value.get("label", key)
                top_cohort_negative_metric = metric
                top_cohort_negative_value = candidate_value

    if top_cohort_positive_label and top_cohort_positive_metric and top_cohort_positive_value > 0:
        highlights.append(
            f"cohort 层面增益上升最明显的是 {top_cohort_positive_label}，其 {top_cohort_positive_metric} 的词典增益相较上一轮 {top_cohort_positive_value:+.4f}。"
        )

    if top_cohort_negative_label and top_cohort_negative_metric and top_cohort_negative_value < 0:
        highlights.append(
            f"cohort 层面回落最明显的是 {top_cohort_negative_label}，其 {top_cohort_negative_metric} 的词典增益相较上一轮 {top_cohort_negative_value:+.4f}。"
        )

    return highlights


def rank_jobs_for_candidate(
    candidate: CandidateSample,
    jobs: list[dict[str, Any]],
    normalizer: dict[str, str],
) -> list[RankedJob]:
    scored: list[RankedJob] = []
    for job in jobs:
        skill_score = calculate_skill_score(job, candidate.truth, normalizer)
        experience_score = calculate_experience_score(job, candidate.truth)
        education_score = calculate_education_score(job, candidate.truth)
        semantic_score = calculate_semantic_score(job, candidate.truth)
        hybrid_score = round(
            skill_score * DEFAULT_WEIGHTS["skill"] / 100.0
            + experience_score * DEFAULT_WEIGHTS["experience"] / 100.0
            + education_score * DEFAULT_WEIGHTS["education"] / 100.0
            + semantic_score * DEFAULT_WEIGHTS["semantic"] / 100.0,
            2,
        )
        scored.append(
            RankedJob(
                rank=0,
                job_id=job["jobId"],
                title=job["title"],
                match_score=hybrid_score,
                skill_score=skill_score,
                experience_score=experience_score,
                education_score=education_score,
                semantic_score=semantic_score,
                relevant=job["jobId"] in candidate.expected_top_jobs,
            )
        )

    sorted_jobs = sorted(scored, key=lambda item: item.match_score, reverse=True)
    return [
        RankedJob(
            rank=index,
            job_id=item.job_id,
            title=item.title,
            match_score=item.match_score,
            skill_score=item.skill_score,
            experience_score=item.experience_score,
            education_score=item.education_score,
            semantic_score=item.semantic_score,
            relevant=item.relevant,
        )
        for index, item in enumerate(sorted_jobs, start=1)
    ]


def calculate_skill_score(job: dict[str, Any], truth: dict[str, Any], normalizer: dict[str, str]) -> float:
    required_skills = extract_required_skill_terms(job, normalizer)
    candidate_skill_terms = normalize_to_canonical_tokens(truth.get("skills", []), normalizer)

    if not required_skills:
        return 60.0

    matched = required_skills.intersection(candidate_skill_terms)
    return round(len(matched) / len(required_skills) * 100.0, 2)


def calculate_experience_score(job: dict[str, Any], truth: dict[str, Any]) -> float:
    requirements = job.get("requirements") or {}
    required_years = parse_experience_years(requirements.get("experienceYears"))
    candidate_years = estimate_experience_years(truth.get("workExperiences", []))
    experience_keywords = extract_experience_keywords(job)
    corpus = set(tokenize(truth.get("summary")))
    for experience in truth.get("workExperiences", []):
        corpus.update(tokenize(experience.get("company")))
        corpus.update(tokenize(experience.get("title")))
        for text in experience.get("responsibilities", []):
            corpus.update(tokenize(text))
        for text in experience.get("achievements", []):
            corpus.update(tokenize(text))

    matched_keywords = experience_keywords.intersection(corpus)
    if required_years is None and truth.get("workExperiences"):
        years_score = 70.0
    elif required_years is None:
        years_score = 0.0
    elif required_years == 0:
        years_score = 70.0
    else:
        years_score = min(candidate_years / required_years, 1.0) * 70.0

    if not experience_keywords:
        keyword_score = 30.0 if truth.get("workExperiences") else 0.0
    else:
        keyword_score = len(matched_keywords) / len(experience_keywords) * 30.0

    return round(years_score + keyword_score, 2)


def calculate_education_score(job: dict[str, Any], truth: dict[str, Any]) -> float:
    education_experiences = truth.get("educationExperiences", [])
    if not education_experiences:
        return 0.0

    education_keywords = extract_education_keywords(job)
    corpus: set[str] = set()
    for education in education_experiences:
        corpus.update(tokenize(education.get("school")))
        corpus.update(tokenize(education.get("degree")))
        corpus.update(tokenize(education.get("fieldOfStudy")))

    if not education_keywords:
        return 70.0

    matched_keywords = education_keywords.intersection(corpus)
    return round(40.0 + len(matched_keywords) / len(education_keywords) * 60.0, 2)


def calculate_semantic_score(job: dict[str, Any], truth: dict[str, Any]) -> float:
    job_embedding = generate_local_embedding(build_job_text(job))
    resume_embedding = generate_local_embedding(build_resume_text(truth))
    similarity = cosine_similarity(job_embedding, resume_embedding)
    return round(max(0.0, min(1.0, similarity)) * 100.0, 2)


def extract_required_skill_terms(job: dict[str, Any], normalizer: dict[str, str]) -> set[str]:
    requirements = job.get("requirements") or {}
    skills = requirements.get("skills")
    if isinstance(skills, list) and skills:
        return normalize_to_canonical_tokens(skills, normalizer)
    if isinstance(skills, str) and skills.strip():
        return normalize_to_canonical_tokens([skills], normalizer)
    return normalize_to_canonical_tokens([job.get("title"), job.get("description")], normalizer)


def normalize_to_canonical_tokens(raw_skills: list[Any], normalizer: dict[str, str]) -> set[str]:
    normalized: set[str] = set()
    for raw in raw_skills:
        if raw is None:
            continue
        raw_tokens = " ".join(tokenize(str(raw)))
        if not raw_tokens:
            continue
        mapped = normalizer.get(raw_tokens, raw_tokens)
        normalized.update(part for part in mapped.split(" ") if part)
    return normalized


def parse_experience_years(value: Any) -> float | None:
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return float(value)
    try:
        return float(str(value).strip())
    except ValueError:
        return None


def estimate_experience_years(experiences: list[dict[str, Any]]) -> float:
    if not experiences:
        return 0.0
    return sum(estimate_years_between(exp.get("startDate"), exp.get("endDate")) for exp in experiences)


def estimate_years_between(start_date: Any, end_date: Any) -> float:
    start_year = parse_year(start_date)
    if start_year is None:
        return 1.0

    end_text = "" if end_date is None else str(end_date).strip()
    if not end_text or end_text.lower().startswith("present"):
        end_year = datetime.now().year
    else:
        end_year = parse_year(end_text) or datetime.now().year
    return float(max(end_year - start_year, 1))


def parse_year(value: Any) -> int | None:
    if value is None:
        return None
    match = re.search(r"(\d{4})", str(value))
    return int(match.group(1)) if match else None


def extract_experience_keywords(job: dict[str, Any]) -> set[str]:
    requirements = job.get("requirements") or {}
    raw_keywords = requirements.get("experienceKeywords")
    keywords: list[str] = []
    if isinstance(raw_keywords, list):
        for item in raw_keywords:
            keywords.extend(tokenize(item))
    elif isinstance(raw_keywords, str):
        keywords.extend(tokenize(raw_keywords))

    corpus = tokenize(job.get("title")) + tokenize(job.get("description")) + keywords
    return {token for token in corpus if token not in {"junior", "senior", "mid"}}


def extract_education_keywords(job: dict[str, Any]) -> set[str]:
    requirements = job.get("requirements") or {}
    raw_keywords = requirements.get("educationKeywords")
    explicit_keywords: list[str] = []
    if isinstance(raw_keywords, list):
        for item in raw_keywords:
            explicit_keywords.extend(tokenize(item))
    elif isinstance(raw_keywords, str):
        explicit_keywords.extend(tokenize(raw_keywords))

    degree_keywords = {"bachelor", "master", "phd", "computer", "science", "engineering"}
    job_tokens = tokenize(job.get("title")) + tokenize(job.get("description"))
    return set(explicit_keywords + [token for token in job_tokens if token in degree_keywords])


def build_job_text(job: dict[str, Any]) -> str:
    requirement_text = " ".join(f"{key} {value}" for key, value in (job.get("requirements") or {}).items())
    return " ".join(part for part in [job.get("title"), job.get("description"), requirement_text] if part).strip()


def build_resume_text(truth: dict[str, Any]) -> str:
    work_text = " ".join(
        " ".join(
            [
                str(experience.get("company", "")),
                str(experience.get("title", "")),
                " ".join(experience.get("responsibilities", [])),
                " ".join(experience.get("achievements", [])),
            ]
        ).strip()
        for experience in truth.get("workExperiences", [])
    )
    education_text = " ".join(
        " ".join(
            [
                str(education.get("school", "")),
                str(education.get("degree", "")),
                str(education.get("fieldOfStudy", "")),
            ]
        ).strip()
        for education in truth.get("educationExperiences", [])
    )
    skill_text = " ".join(str(skill) for skill in truth.get("skills", []))
    return " ".join(
        part for part in [truth.get("candidateName"), truth.get("summary"), work_text, education_text, skill_text] if part
    ).strip()


def generate_local_embedding(text: str, dimensions: int = EMBEDDING_DIMENSIONS) -> list[float]:
    vector = [0.0] * dimensions
    tokens = re.findall(r"[a-z0-9+#.]+", text.lower())
    if not tokens:
        tokens = ["empty"]

    for token in tokens:
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        index = int.from_bytes(digest[:4], "big") % dimensions
        sign = 1.0 if digest[4] % 2 == 0 else -1.0
        weight = 0.5 + (digest[5] / 255.0)
        vector[index] += sign * weight

    norm = math.sqrt(sum(value * value for value in vector))
    if norm == 0:
        raise RuntimeError("Local embedding fallback produced a zero vector")
    return [round(value / norm, 8) for value in vector]


def cosine_similarity(left: list[float], right: list[float]) -> float:
    size = min(len(left), len(right))
    if size == 0:
        return 0.0

    dot = 0.0
    left_norm = 0.0
    right_norm = 0.0
    for index in range(size):
        dot += left[index] * right[index]
        left_norm += left[index] * left[index]
        right_norm += right[index] * right[index]

    if left_norm == 0.0 or right_norm == 0.0:
        return 0.0
    return max(0.0, min(1.0, dot / (math.sqrt(left_norm) * math.sqrt(right_norm))))


def tokenize(text: Any) -> list[str]:
    if text is None:
        return []
    parts = TOKEN_SPLIT_REGEX.split(str(text).lower())
    return [part.strip() for part in parts if len(part.strip()) >= 2 and part.strip() not in STOP_WORDS]


def first_relevant_rr(relevance: list[int]) -> float:
    for index, value in enumerate(relevance, start=1):
        if value > 0:
            return 1.0 / index
    return 0.0


def ndcg_at_k(relevance: list[int], k: int, relevant_total: int) -> float:
    if k <= 0:
        return 0.0
    dcg = 0.0
    for index, rel in enumerate(relevance[:k], start=1):
        dcg += rel / math.log2(index + 1)

    ideal_hits = min(relevant_total, k)
    if ideal_hits == 0:
        return 0.0
    idcg = sum(1.0 / math.log2(index + 1) for index in range(1, ideal_hits + 1))
    return 0.0 if idcg == 0 else dcg / idcg


def mean(values: list[float]) -> float:
    return 0.0 if not values else sum(values) / len(values)


def write_json(path: Path, payload: Any) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_rankings_csv(path: Path, rankings: list[dict[str, Any]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(
            [
                "sample_id",
                "candidate_name",
                "target_direction",
                "fit_level",
                "expected_top_jobs",
                "rank",
                "job_id",
                "job_title",
                "match_score",
                "skill_score",
                "experience_score",
                "education_score",
                "semantic_score",
                "relevant",
            ]
        )
        for candidate in rankings:
            expected_jobs = ",".join(candidate["expectedTopJobs"])
            for prediction in candidate["predictions"]:
                writer.writerow(
                    [
                        candidate["sampleId"],
                        candidate["candidateName"],
                        candidate["targetDirection"],
                        candidate["fitLevel"],
                        expected_jobs,
                        prediction["rank"],
                        prediction["job_id"],
                        prediction["title"],
                        prediction["match_score"],
                        prediction["skill_score"],
                        prediction["experience_score"],
                        prediction["education_score"],
                        prediction["semantic_score"],
                        prediction["relevant"],
                    ]
                )


if __name__ == "__main__":
    main()