-- IVFFlat index for approximate nearest-neighbour search using cosine distance.
-- lists = 100 is a reasonable default for datasets under ~1M rows.
-- Tune this value as the dataset grows: aim for sqrt(total_rows) lists.
CREATE INDEX IF NOT EXISTS idx_code_chunks_embedding_cosine
    ON code_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
