UPDATE users
SET role = 'ADMIN'
WHERE role = 'SYSTEM_ADMIN';

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_role;

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('HR', 'CANDIDATE', 'ADMIN'));

ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS user_id UUID;

ALTER TABLE resumes
    DROP CONSTRAINT IF EXISTS fk_resumes_user_id;

ALTER TABLE resumes
    ADD CONSTRAINT fk_resumes_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes(user_id);

CREATE TABLE IF NOT EXISTS candidate_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    github_url VARCHAR(500),
    portfolio_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_candidate_profiles_user_id ON candidate_profiles(user_id);