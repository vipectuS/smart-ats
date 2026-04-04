ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS status VARCHAR(32);

UPDATE job_applications
SET status = 'APPLIED'
WHERE status IS NULL;

ALTER TABLE job_applications
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE job_applications
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE job_applications
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE job_applications
    DROP CONSTRAINT IF EXISTS chk_job_applications_status;

ALTER TABLE job_applications
    ADD CONSTRAINT chk_job_applications_status CHECK (status IN ('APPLIED', 'WITHDRAWN'));