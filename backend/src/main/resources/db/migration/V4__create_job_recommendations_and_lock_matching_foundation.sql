CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE job_recommendations (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    match_score NUMERIC(5, 2) NOT NULL,
    xai_reasoning TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_recommendations_job_resume UNIQUE (job_id, resume_id)
);

CREATE INDEX idx_job_recommendations_job_id ON job_recommendations(job_id);
CREATE INDEX idx_job_recommendations_resume_id ON job_recommendations(resume_id);