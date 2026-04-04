ALTER TABLE job_recommendations
    ADD COLUMN IF NOT EXISTS xai_report JSONB;

CREATE TABLE job_applications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_applications_user_job UNIQUE (user_id, job_id)
);

CREATE INDEX idx_job_applications_user_id ON job_applications(user_id);
CREATE INDEX idx_job_applications_job_id ON job_applications(job_id);

CREATE TABLE job_favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_favorites_user_job UNIQUE (user_id, job_id)
);

CREATE INDEX idx_job_favorites_user_id ON job_favorites(user_id);
CREATE INDEX idx_job_favorites_job_id ON job_favorites(job_id);

CREATE TABLE job_ignores (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_ignores_user_job UNIQUE (user_id, job_id)
);

CREATE INDEX idx_job_ignores_user_id ON job_ignores(user_id);
CREATE INDEX idx_job_ignores_job_id ON job_ignores(job_id);