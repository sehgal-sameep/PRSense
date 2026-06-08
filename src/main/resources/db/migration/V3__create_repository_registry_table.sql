-- Registry of Azure DevOps repositories that PRSense manages for indexing
CREATE TABLE IF NOT EXISTS repository_registry (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id         VARCHAR(255) UNIQUE NOT NULL,  -- Azure DevOps repo GUID
    project_name          VARCHAR(255) NOT NULL,
    repository_name       VARCHAR(255) NOT NULL,
    default_branch        VARCHAR(255) NOT NULL DEFAULT 'main',
    last_indexed_commit   VARCHAR(64),                   -- SHA of last indexed HEAD
    last_indexed_at       TIMESTAMP,
    status                VARCHAR(50)  NOT NULL DEFAULT 'REGISTERED',
    chunk_count           INT          NOT NULL DEFAULT 0,
    file_count            INT          NOT NULL DEFAULT 0,
    index_version         INT          NOT NULL DEFAULT 0,
    failure_reason        TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_registry_project_repo
    ON repository_registry (project_name, repository_name);

CREATE INDEX IF NOT EXISTS idx_registry_status
    ON repository_registry (status);
