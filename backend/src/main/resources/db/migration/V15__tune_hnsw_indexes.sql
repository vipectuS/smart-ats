-- --------------------------------------------------------------------------------------
-- 智能招聘辅助系统 / V15__tune_hnsw_indexes.sql
-- --------------------------------------------------------------------------------------
-- 为解决 THESIS_IMPROVEMENT_PLAN.md 中的 问题二 (Scalability) 专项优化。
-- 重构 pgvector 的 HNSW 索引：
-- 提高每个节点的边数 (m=24) 与构建时的查询深度 (ef_construction=128)
-- 以防止未来数据量突增至十万/百万级别时的 Seq Scan 回退，保障向量召回率与检索高可用。
-- 结合代码层的 LIMIT 切断机制，彻底根治全表扫描超时隐患。
-- --------------------------------------------------------------------------------------

DROP INDEX IF EXISTS idx_jobs_embedding_hnsw;
CREATE INDEX idx_jobs_embedding_hnsw 
    ON jobs USING hnsw (embedding vector_cosine_ops) 
    WITH (m = 24, ef_construction = 128);

DROP INDEX IF EXISTS idx_resumes_embedding_hnsw;
CREATE INDEX idx_resumes_embedding_hnsw 
    ON resumes USING hnsw (embedding vector_cosine_ops) 
    WITH (m = 24, ef_construction = 128);
