ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS admin_review_note TEXT;

ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS admin_reviewed_by VARCHAR(255);

ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS admin_reviewed_at TIMESTAMP;