ALTER TABLE job_recommendations
    ADD COLUMN IF NOT EXISTS semantic_score NUMERIC(5, 2) NOT NULL DEFAULT 0;

ALTER TABLE job_recommendations
    ADD COLUMN IF NOT EXISTS suitability_report TEXT NOT NULL DEFAULT '';

ALTER TABLE job_recommendations
    ADD COLUMN IF NOT EXISTS matched_skills JSONB NOT NULL DEFAULT '[]';

ALTER TABLE job_recommendations
    ADD COLUMN IF NOT EXISTS missing_skills JSONB NOT NULL DEFAULT '[]';