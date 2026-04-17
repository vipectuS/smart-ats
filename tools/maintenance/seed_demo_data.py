from __future__ import annotations

import argparse
import json
import os
from collections import Counter
from datetime import UTC, datetime
from pathlib import Path
import time
from typing import Any

from demo_api import ApiError, JsonApiClient, ensure_user, login


ROOT = Path(__file__).resolve().parents[2]
DATASET_DIR = ROOT / "doc" / "synthetic-dataset"
MANIFEST_PATH = DATASET_DIR / "manifest.json"
JOBS_PATH = DATASET_DIR / "jobs.json"
OUTPUT_DIR = ROOT / "tools" / "maintenance" / "output"
DEFAULT_PASSWORD = os.environ.get("SMART_ATS_DEMO_PASSWORD", "DemoPass123")


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def infer_skill_category(skill: str) -> str:
    lowered = skill.lower()
    if lowered in {"kotlin", "java", "python", "typescript", "javascript", "sql", "node.js"}:
        return "engineering"
    if lowered in {"vue", "vite", "spring boot", "pandas", "echarts"}:
        return "framework"
    if lowered in {"postgresql", "mysql", "redis", "docker", "kubernetes", "linux", "monitoring", "ci/cd"}:
        return "infrastructure"
    if lowered in {"pytest", "selenium", "api testing", "a/b testing", "data visualization"}:
        return "tooling"
    return "general"


def build_resume_payload(sample: dict[str, Any]) -> dict[str, Any]:
    sample_id = sample["sampleId"]
    file_name = f"{sample_id}_{sample['candidateName']}.pdf"
    preview_text = ", ".join(sample.get("strengths", []))
    timestamp = datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    return {
        "rawContentReference": f"synthetic://dataset/{sample_id}/{file_name}",
        "browserPreprocessedPayload": {
            "engine": "pdfium-browser-seed",
            "mode": "synthetic-preview",
            "sourceFileName": file_name,
            "sourceMimeType": "application/pdf",
            "sourceFileSize": 4096,
            "derivedReference": f"browser-pdf-preview://seed/{sample_id}/{file_name}",
            "pageCount": 1,
            "extractedTextPreview": preview_text,
            "generatedAt": timestamp,
            "warnings": ["seed-demo-payload"],
            "pagePreviews": [
                {
                    "pageNumber": 1,
                    "width": 320,
                    "height": 452,
                    "imageDataUrl": "data:image/jpeg;base64,seed-preview",
                    "textPreview": preview_text,
                },
            ],
        },
    }


def fetch_existing_jobs(admin_client: JsonApiClient) -> dict[str, dict[str, Any]]:
    response = admin_client.get("/api/jobs", {"page": 0, "size": 200})
    return {job["title"]: job for job in response["content"]}


def fetch_existing_resumes(admin_client: JsonApiClient) -> dict[str, dict[str, Any]]:
    response = admin_client.get("/api/resumes", {"page": 0, "size": 500})
    content = response["content"]
    return {resume["rawContentReference"]: resume for resume in content}


def is_valid_talent_profile(resume: dict[str, Any]) -> bool:
    parsed_data = resume.get("parsedData") or {}
    return all(
        key in parsed_data and parsed_data[key]
        for key in ("basicInfo", "skills", "radarScores")
    )


def ensure_jobs(admin_client: JsonApiClient, jobs: list[dict[str, Any]], summary: Counter[str]) -> dict[str, str]:
    existing_by_title = fetch_existing_jobs(admin_client)
    job_mapping: dict[str, str] = {}

    for job in jobs:
        existing = existing_by_title.get(job["title"])
        if existing is None:
            created = admin_client.post(
                "/api/jobs",
                {
                    "title": job["title"],
                    "description": f"{job['description']}\n\n[Dataset] department={job['department']} location={job['location']} headcount={job['headcount']}",
                    "requirements": {
                        **job["requirements"],
                        "department": job["department"],
                        "location": job["location"],
                        "headcount": job["headcount"],
                        "datasetJobId": job["jobId"],
                    },
                },
            )
            existing_by_title[job["title"]] = created
            existing = created
            summary["jobs_created"] += 1
        else:
            summary["jobs_reused"] += 1

        job_mapping[job["jobId"]] = existing["id"]

    return job_mapping


def ensure_skills(admin_client: JsonApiClient, jobs: list[dict[str, Any]], manifest: list[dict[str, Any]], summary: Counter[str]) -> None:
    existing = admin_client.get("/api/admin/skills")
    existing_names = {item["name"].strip().lower() for item in existing}

    desired_skills: dict[str, str] = {}
    for job in jobs:
        for skill in job["requirements"].get("skills", []):
            desired_skills.setdefault(skill, infer_skill_category(skill))
    for sample in manifest:
        for skill in sample.get("strengths", []) + sample.get("gaps", []):
            desired_skills.setdefault(skill, infer_skill_category(skill))

    for skill_name, category in desired_skills.items():
        if skill_name.lower() in existing_names:
            summary["skills_reused"] += 1
            continue
        admin_client.post(
            "/api/admin/skills",
            {
                "name": skill_name,
                "category": category,
                "aliases": [],
                "enabled": True,
            },
        )
        existing_names.add(skill_name.lower())
        summary["skills_created"] += 1


def wait_for_resume(admin_client: JsonApiClient, resume_id: str, timeout_seconds: int) -> dict[str, Any]:
    deadline = time.time() + timeout_seconds
    last_payload: dict[str, Any] | None = None

    while time.time() < deadline:
        last_payload = admin_client.get(f"/api/resumes/{resume_id}/status")
        if last_payload["status"] in {"PARSED", "PARSE_FAILED"}:
            return last_payload
        time.sleep(1.5)

    raise TimeoutError(f"Resume {resume_id} did not finish parsing within {timeout_seconds} seconds: {last_payload}")


def ensure_candidates(
    base_url: str,
    admin_client: JsonApiClient,
    manifest: list[dict[str, Any]],
    job_mapping: dict[str, str],
    timeout_seconds: int,
    summary: Counter[str],
) -> list[dict[str, Any]]:
    existing_resumes = fetch_existing_resumes(admin_client)
    seeded_candidates: list[dict[str, Any]] = []

    for sample in manifest:
        username = f"demo_{sample['sampleId'].lower()}"
        email = f"{username}@smartats.local"
        candidate_client, created = ensure_user(base_url, username, email, DEFAULT_PASSWORD, "CANDIDATE")
        summary["candidate_users_created" if created else "candidate_users_reused"] += 1

        payload = build_resume_payload(sample)
        existing_resume = existing_resumes.get(payload["rawContentReference"])
        if existing_resume is None:
            created_resume = candidate_client.post("/api/resumes/upload", payload)
            existing_resumes[payload["rawContentReference"]] = created_resume
            existing_resume = created_resume
            summary["resumes_uploaded"] += 1
        else:
            summary["resumes_reused"] += 1

        if existing_resume.get("status") != "PARSED" or not is_valid_talent_profile(existing_resume):
            admin_client.post(f"/api/resumes/{existing_resume['id']}/parse")
            summary["resumes_reparsed"] += 1

        final_status = wait_for_resume(admin_client, existing_resume["id"], timeout_seconds)
        summary[f"resume_status_{final_status['status'].lower()}"] += 1

        expected_jobs = sample.get("expectedTopJobs", [])
        if expected_jobs:
            target_job_id = job_mapping.get(expected_jobs[0])
            if target_job_id:
                try:
                    apply_result = candidate_client.post(f"/api/jobs/{target_job_id}/apply")
                    summary["applications_created" if apply_result.get("created") else "applications_reused"] += 1
                except ApiError as exc:
                    summary["application_errors"] += 1
                    print(f"[warn] apply failed for {username}: {exc}")

        seeded_candidates.append(
            {
                "sampleId": sample["sampleId"],
                "username": username,
                "email": email,
                "resumeId": existing_resume["id"],
                "resumeStatus": final_status["status"],
            },
        )

    return seeded_candidates


def evaluate_jobs(admin_client: JsonApiClient, job_mapping: dict[str, str], summary: Counter[str]) -> None:
    for actual_job_id in job_mapping.values():
        admin_client.post(f"/api/jobs/{actual_job_id}/evaluate")
        summary["jobs_evaluated"] += 1


def write_summary(summary: dict[str, Any], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed demo users, jobs, resumes, and recommendations through real Smart ATS APIs.")
    parser.add_argument("--base-url", default=os.environ.get("SMART_ATS_BASE_URL", "http://127.0.0.1:18080"))
    parser.add_argument("--timeout-seconds", type=int, default=90)
    parser.add_argument("--skip-evaluate", action="store_true")
    parser.add_argument(
        "--output",
        default=str(OUTPUT_DIR / "seed_demo_data_summary.json"),
    )
    args = parser.parse_args()

    manifest = load_json(MANIFEST_PATH)
    jobs = load_json(JOBS_PATH)
    admin_username = os.environ.get("SMART_ATS_ADMIN_USERNAME", "admin")
    admin_password = os.environ.get("SMART_ATS_ADMIN_PASSWORD", "admin")

    admin_client = login(args.base_url, admin_username, admin_password)
    _, hr_created = ensure_user(args.base_url, "demo_hr", "demo_hr@smartats.local", DEFAULT_PASSWORD, "HR")

    summary: Counter[str] = Counter()
    summary["hr_users_created" if hr_created else "hr_users_reused"] += 1
    job_mapping = ensure_jobs(admin_client, jobs, summary)
    ensure_skills(admin_client, jobs, manifest, summary)
    candidates = ensure_candidates(args.base_url, admin_client, manifest, job_mapping, args.timeout_seconds, summary)

    if not args.skip_evaluate:
        evaluate_jobs(admin_client, job_mapping, summary)

    payload = {
        "generatedAt": datetime.now(UTC).replace(microsecond=0).isoformat(),
        "baseUrl": args.base_url,
        "summary": dict(summary),
        "jobMapping": job_mapping,
        "candidates": candidates,
    }
    write_summary(payload, Path(args.output))

    print(json.dumps(payload, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()