ALTER TABLE job_favorites
    ADD COLUMN IF NOT EXISTS status VARCHAR(32);

UPDATE job_favorites
SET status = 'FAVORITED'
WHERE status IS NULL;

ALTER TABLE job_favorites
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE job_favorites
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE job_favorites
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE job_favorites
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE job_favorites
    DROP CONSTRAINT IF EXISTS chk_job_favorites_status;

ALTER TABLE job_favorites
    ADD CONSTRAINT chk_job_favorites_status CHECK (status IN ('FAVORITED', 'UNFAVORITED'));

ALTER TABLE job_ignores
    ADD COLUMN IF NOT EXISTS status VARCHAR(32);

UPDATE job_ignores
SET status = 'IGNORED'
WHERE status IS NULL;

ALTER TABLE job_ignores
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE job_ignores
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE job_ignores
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE job_ignores
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE job_ignores
    DROP CONSTRAINT IF EXISTS chk_job_ignores_status;

ALTER TABLE job_ignores
    ADD CONSTRAINT chk_job_ignores_status CHECK (status IN ('IGNORED', 'UNIGNORED'));