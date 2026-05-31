CREATE TABLE job_evaluation_runs (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    evaluated_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    version_number INTEGER NOT NULL,
    skill_weight NUMERIC(5, 2) NOT NULL,
    experience_weight NUMERIC(5, 2) NOT NULL,
    education_weight NUMERIC(5, 2) NOT NULL,
    semantic_weight NUMERIC(5, 2) NOT NULL,
    evaluated_count INTEGER NOT NULL,
    recommendation_snapshot JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_job_evaluation_runs_job_version UNIQUE (job_id, version_number)
);

CREATE INDEX idx_job_evaluation_runs_job_version ON job_evaluation_runs(job_id, version_number DESC);