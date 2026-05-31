ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS is_system_default BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE organizations
    DROP CONSTRAINT IF EXISTS chk_organizations_system_default_enabled;

ALTER TABLE organizations
    ADD CONSTRAINT chk_organizations_system_default_enabled
        CHECK (NOT (is_system_default = TRUE AND enabled = FALSE));

DELETE FROM job_recommendations;
DELETE FROM job_applications;
DELETE FROM job_favorites;
DELETE FROM job_ignores;
DELETE FROM candidate_profiles;
DELETE FROM resumes;
DELETE FROM jobs;

DELETE FROM users
WHERE username <> 'admin';

UPDATE organizations
SET is_system_default = FALSE,
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE is_system_default = TRUE;

INSERT INTO organizations (
    id,
    name,
    token_hash,
    token_preview,
    enabled,
    is_system_default,
    created_at,
    updated_at
)
SELECT
    '00000000-0000-0000-0000-000000000002',
    'Whoseyards Foundation',
    'SYSTEM_DEFAULT_ORG_RESEEDED_BY_DATASEEDER',
    'sysd****only',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM organizations
    WHERE id = '00000000-0000-0000-0000-000000000002'
);

UPDATE organizations
SET name = 'Whoseyards Foundation',
    token_hash = 'SYSTEM_DEFAULT_ORG_RESEEDED_BY_DATASEEDER',
    token_preview = 'sysd****only',
    enabled = TRUE,
    is_system_default = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '00000000-0000-0000-0000-000000000002';

DELETE FROM organizations
WHERE id <> '00000000-0000-0000-0000-000000000002';

UPDATE users
SET role = 'ADMIN',
    organization_id = '00000000-0000-0000-0000-000000000002',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin';