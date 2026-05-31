CREATE TABLE job_evaluation_runs (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    evaluated_by_user_id UUID,
    version_number INTEGER NOT NULL,
    skill_weight NUMERIC(5, 2) NOT NULL,
    experience_weight NUMERIC(5, 2) NOT NULL,
    education_weight NUMERIC(5, 2) NOT NULL,
    semantic_weight NUMERIC(5, 2) NOT NULL,
    evaluated_count INTEGER NOT NULL,
    recommendation_snapshot JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_evaluation_runs_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_evaluation_runs_user FOREIGN KEY (evaluated_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_job_evaluation_runs_job_version UNIQUE (job_id, version_number)
);

CREATE INDEX idx_job_evaluation_runs_job_version ON job_evaluation_runs(job_id, version_number DESC);