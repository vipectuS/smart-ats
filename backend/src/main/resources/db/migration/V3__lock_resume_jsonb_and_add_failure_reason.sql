ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS parse_failure_reason TEXT;

ALTER TABLE resumes
    ALTER COLUMN parsed_data TYPE jsonb
    USING CASE
        WHEN parsed_data IS NULL THEN NULL
        ELSE parsed_data::jsonb
    END;