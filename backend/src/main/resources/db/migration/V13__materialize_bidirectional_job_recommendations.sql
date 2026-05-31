ALTER TABLE job_recommendations
    ADD COLUMN IF NOT EXISTS semantic_score NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS suitability_report TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS matched_skills JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS missing_skills JSONB NOT NULL DEFAULT '[]'::jsonb;