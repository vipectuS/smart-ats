ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS review_note VARCHAR(2000);

ALTER TABLE job_applications
    DROP CONSTRAINT IF EXISTS chk_job_applications_status;

ALTER TABLE job_applications
    ADD CONSTRAINT chk_job_applications_status CHECK (status IN ('APPLIED', 'INTERVIEW', 'REJECTED', 'WITHDRAWN'));