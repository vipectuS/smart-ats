ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS embedding VECTOR(1536);

ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS embedding VECTOR(1536);

CREATE INDEX IF NOT EXISTS idx_jobs_embedding_hnsw
    ON jobs USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_resumes_embedding_hnsw
    ON resumes USING hnsw (embedding vector_cosine_ops);