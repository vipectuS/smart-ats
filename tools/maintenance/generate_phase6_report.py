from __future__ import annotations

import argparse
import json
import os
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from demo_api import ApiError, login


ROOT = Path(__file__).resolve().parents[2]
DATASET_DIR = ROOT / "doc" / "synthetic-dataset"
MANIFEST_PATH = DATASET_DIR / "manifest.json"
JOBS_PATH = DATASET_DIR / "jobs.json"
OUTPUT_DIR = ROOT / "tools" / "maintenance" / "output"
DEFAULT_PASSWORD = os.environ.get("SMART_ATS_DEMO_PASSWORD", "DemoPass123")


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate a reproducible Phase 6 evidence report from seeded demo data.")
    parser.add_argument("--base-url", default=os.environ.get("SMART_ATS_BASE_URL", "http://127.0.0.1:18080"))
    parser.add_argument(
        "--output",
        default=str(OUTPUT_DIR / "phase6_report.json"),
    )
    args = parser.parse_args()

    manifest = load_json(MANIFEST_PATH)
    jobs = load_json(JOBS_PATH)
    dataset_candidate_names = {sample["candidateName"] for sample in manifest}
    dataset_usernames = {f"demo_{sample['sampleId'].lower()}" for sample in manifest}
    dataset_job_titles = {job["title"] for job in jobs}
    admin_client = login(
        args.base_url,
        os.environ.get("SMART_ATS_ADMIN_USERNAME", "admin"),
        os.environ.get("SMART_ATS_ADMIN_PASSWORD", "admin"),
    )

    overview = admin_client.get("/api/admin/overview")
    skills = admin_client.get("/api/admin/skills")
    jobs_page = admin_client.get("/api/jobs", {"page": 0, "size": 200})
    resumes_page = admin_client.get("/api/resumes", {"page": 0, "size": 500})

    title_to_dataset_job_id = {job["title"]: job["jobId"] for job in jobs}
    actual_jobs = jobs_page["content"]
    actual_job_ids = {job["id"]: title_to_dataset_job_id.get(job["title"]) for job in actual_jobs}
    demo_jobs = [job for job in actual_jobs if job["title"] in dataset_job_titles]
    demo_resumes = [
        resume
        for resume in resumes_page["content"]
        if (resume.get("contactInfo") or "").endswith("@smartats.local")
    ]

    candidate_evidence = []
    top3_hits = 0
    top1_hits = 0

    for sample in manifest:
        username = f"demo_{sample['sampleId'].lower()}"
        candidate_client = login(args.base_url, username, DEFAULT_PASSWORD)
        try:
            match_response = candidate_client.post("/api/candidate/match-jobs")
            recommendations = match_response.get("recommendations", [])
        except ApiError as exc:
            candidate_evidence.append(
                {
                    "sampleId": sample["sampleId"],
                    "username": username,
                    "error": str(exc),
                },
            )
            continue

        predicted_job_ids = [
            actual_job_ids.get(item["jobId"])
            for item in recommendations[:3]
            if actual_job_ids.get(item["jobId"]) is not None
        ]
        expected = sample.get("expectedTopJobs", [])
        top1_hit = bool(predicted_job_ids[:1] and predicted_job_ids[0] in expected)
        top3_hit = any(job_id in expected for job_id in predicted_job_ids)
        top1_hits += int(top1_hit)
        top3_hits += int(top3_hit)

        candidate_evidence.append(
            {
                "sampleId": sample["sampleId"],
                "candidateName": sample["candidateName"],
                "expectedTopJobs": expected,
                "predictedTop3Jobs": predicted_job_ids,
                "evaluatedCount": match_response.get("evaluatedCount"),
                "top1Hit": top1_hit,
                "top3Hit": top3_hit,
            },
        )

    hr_recommendations = []
    for job in demo_jobs:
        recommendations = admin_client.get(f"/api/jobs/{job['id']}/recommendations")
        dataset_recommendations = [
            item
            for item in recommendations
            if item["candidate"]["basicInfo"]["fullName"] in dataset_candidate_names
        ]
        hr_recommendations.append(
            {
                "jobId": title_to_dataset_job_id.get(job["title"]),
                "title": job["title"],
                "recommendationCount": len(recommendations),
                "datasetRecommendationCount": len(dataset_recommendations),
                "topCandidates": [
                    {
                        "name": item["candidate"]["basicInfo"]["fullName"],
                        "score": item["matchScore"],
                    }
                    for item in dataset_recommendations[:3]
                ],
            },
        )

    report = {
        "generatedAt": datetime.now(UTC).replace(microsecond=0).isoformat(),
        "baseUrl": args.base_url,
        "overview": overview,
        "skillCount": len(skills),
        "jobCount": jobs_page["totalElements"],
        "resumeCount": resumes_page["totalElements"],
        "datasetScopedSummary": {
            "jobCount": len(demo_jobs),
            "candidateCount": len(dataset_usernames),
            "resumeCount": len(demo_resumes),
            "parsedResumeCount": sum(1 for resume in demo_resumes if resume.get("status") == "PARSED"),
        },
        "candidateRanking": {
            "sampleCount": len(manifest),
            "top1Hits": top1_hits,
            "top3Hits": top3_hits,
            "top1HitRate": round(top1_hits / max(len(manifest), 1), 4),
            "top3HitRate": round(top3_hits / max(len(manifest), 1), 4),
        },
        "candidateEvidence": candidate_evidence,
        "hrRecommendations": hr_recommendations,
    }

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()