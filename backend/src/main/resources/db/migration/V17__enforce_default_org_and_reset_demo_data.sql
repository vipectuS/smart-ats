ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS is_system_default BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE organizations
    DROP CONSTRAINT IF EXISTS chk_organizations_system_default_enabled;

ALTER TABLE organizations
    ADD CONSTRAINT chk_organizations_system_default_enabled
        CHECK (NOT (is_system_default = TRUE AND enabled = FALSE));

CREATE UNIQUE INDEX IF NOT EXISTS uk_organizations_single_system_default
    ON organizations (is_system_default)
    WHERE is_system_default = TRUE;

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
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'Whoseyards Foundation',
    'SYSTEM_DEFAULT_ORG_RESEEDED_BY_DATASEEDER',
    'sysd****only',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    token_hash = EXCLUDED.token_hash,
    token_preview = EXCLUDED.token_preview,
    enabled = TRUE,
    is_system_default = TRUE,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM organizations
WHERE id <> '00000000-0000-0000-0000-000000000002';

UPDATE users
SET role = 'ADMIN',
    organization_id = '00000000-0000-0000-0000-000000000002',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin';
