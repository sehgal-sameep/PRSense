package com.codewithsam.prsense.entity;

public enum RepositoryStatus {
    REGISTERED,       // Created, awaiting initial index
    INDEXING,         // Initial full index in progress
    INDEXED,          // Index complete and current
    SYNC_IN_PROGRESS, // Incremental sync running
    STALE,            // Last sync detected drift (commits ahead of index)
    FAILED            // Last operation failed — see failure_reason
}
