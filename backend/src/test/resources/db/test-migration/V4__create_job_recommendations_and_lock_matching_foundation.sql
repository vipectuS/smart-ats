CREATE TABLE job_recommendations (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    resume_id UUID NOT NULL,
    match_score NUMERIC(5, 2) NOT NULL,
    xai_reasoning TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_recommendations_job_resume UNIQUE (job_id, resume_id),
    CONSTRAINT fk_job_recommendations_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_recommendations_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
);

CREATE INDEX idx_job_recommendations_job_id ON job_recommendations(job_id);
CREATE INDEX idx_job_recommendations_resume_id ON job_recommendations(resume_id);