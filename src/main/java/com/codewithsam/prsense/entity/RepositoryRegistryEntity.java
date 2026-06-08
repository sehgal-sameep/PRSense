package com.codewithsam.prsense.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "repository_registry",
    indexes = {
        @Index(name = "idx_registry_project_repo", columnList = "project_name, repository_name"),
        @Index(name = "idx_registry_status",       columnList = "status")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryRegistryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Azure DevOps repository GUID — stable primary identifier for webhook routing
    @Column(name = "repository_id", unique = true, nullable = false)
    private String repositoryId;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    // Human-readable name — used as the key in code_chunks.repository
    @Column(name = "repository_name", nullable = false)
    private String repositoryName;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    // SHA of the last commit that was fully indexed
    @Column(name = "last_indexed_commit")
    private String lastIndexedCommit;

    @Column(name = "last_indexed_at")
    private LocalDateTime lastIndexedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RepositoryStatus status;

    @Column(name = "chunk_count")
    private int chunkCount;

    @Column(name = "file_count")
    private int fileCount;

    @Column(name = "index_version")
    private int indexVersion;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
