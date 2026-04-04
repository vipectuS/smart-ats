ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS embedding VARCHAR(20000);

ALTER TABLE jobs
    ALTER COLUMN embedding VARCHAR(20000);

ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS embedding VARCHAR(20000);

ALTER TABLE resumes
    ALTER COLUMN embedding VARCHAR(20000);

CREATE INDEX IF NOT EXISTS idx_jobs_embedding ON jobs(embedding);
CREATE INDEX IF NOT EXISTS idx_resumes_embedding ON resumes(embedding);