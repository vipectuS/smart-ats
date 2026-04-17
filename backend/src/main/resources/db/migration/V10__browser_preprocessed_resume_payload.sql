ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS browser_preprocessed_payload JSONB;