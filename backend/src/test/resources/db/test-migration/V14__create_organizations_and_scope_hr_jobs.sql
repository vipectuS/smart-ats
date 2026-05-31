CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    token_hash VARCHAR(255) NOT NULL,
    token_preview VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

MERGE INTO organizations (id, name, token_hash, token_preview, enabled, created_at, updated_at)
KEY (id)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Legacy Organization',
    'LEGACY_ORGANIZATION_IMPORTED',
    'legacy-import',
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS organization_id UUID;

ALTER TABLE users
    ADD CONSTRAINT IF NOT EXISTS fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

UPDATE users
SET organization_id = '00000000-0000-0000-0000-000000000001'
WHERE role = 'HR' AND organization_id IS NULL;

ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS organization_id UUID;

ALTER TABLE jobs
    ADD CONSTRAINT IF NOT EXISTS fk_jobs_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

UPDATE jobs j
SET organization_id = COALESCE(
    (
        SELECT u.organization_id
        FROM users u
        WHERE u.id = j.created_by
    ),
    '00000000-0000-0000-0000-000000000001'
)
WHERE organization_id IS NULL;

ALTER TABLE jobs
    ALTER COLUMN organization_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users (organization_id);
CREATE INDEX IF NOT EXISTS idx_jobs_organization_id ON jobs (organization_id);