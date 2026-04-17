# Maintenance Notes

This directory is reserved for non-product maintenance utilities only.

Before GitHub sync, all historical one-off patch scripts were removed to keep the repository clean.

Policy:
- Do not store temporary LLM-generated patch scripts in the project root.
- If a helper is repeatable and truly useful, document it and keep it here.
- If a helper is only used once, apply the change directly and remove the script afterward.

Current reusable helpers:
- `bootstrap_local_demo.sh`: start backend, ai-service, and frontend together, with optional `--with-demo-data` seeding.
- `stop_local_demo.sh`: stop the local demo processes started by the bootstrap script.
- `seed_demo_data.py`: import jobs, users, resumes, and applications through real APIs using the synthetic dataset.
- `generate_phase6_report.py`: export a structured Phase 6 evidence report in JSON.
