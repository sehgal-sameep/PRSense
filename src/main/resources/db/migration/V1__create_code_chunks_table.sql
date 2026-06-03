-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS code_chunks (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    repository    VARCHAR(255) NOT NULL,
    file_path     VARCHAR(1000) NOT NULL,
    package_name  VARCHAR(500),
    class_name    VARCHAR(255),
    method_name   VARCHAR(255),
    symbol_type   VARCHAR(50)  NOT NULL,
    content       TEXT         NOT NULL,
    content_hash  VARCHAR(64)  NOT NULL,
    embedding     vector(1536),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_code_chunks_repo_path
    ON code_chunks (repository, file_path);

CREATE INDEX IF NOT EXISTS idx_code_chunks_content_hash
    ON code_chunks (content_hash);
