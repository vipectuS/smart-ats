ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS admin_review_status VARCHAR(64) NOT NULL DEFAULT 'UNREVIEWED';

UPDATE resumes
SET admin_review_status = 'UNREVIEWED'
WHERE admin_review_status IS NULL;

ALTER TABLE resumes
    DROP CONSTRAINT IF EXISTS chk_resumes_admin_review_status;

ALTER TABLE resumes
    ADD CONSTRAINT chk_resumes_admin_review_status CHECK (
        admin_review_status IN (
            'UNREVIEWED',
            'NEEDS_CANDIDATE_UPDATE',
            'APPROVED_FOR_RETRY',
            'NO_FURTHER_ACTION'
        )
    );