package com.codewithsam.prsense.entity;

import com.codewithsam.prsense.constants.SymbolType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "code_chunks",
    indexes = {
        @Index(name = "idx_code_chunks_repo_path", columnList = "repository, file_path"),
        @Index(name = "idx_code_chunks_content_hash", columnList = "content_hash")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String repository;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "class_name")
    private String className;

    @Column(name = "method_name")
    private String methodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "symbol_type", nullable = false)
    private SymbolType symbolType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // SHA-256 of content — used for incremental reindexing deduplication
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    // Stored as pgvector type. Written as a cast (?::vector) so PostgreSQL
    // receives the string literal "[v1,v2,...]" and stores it as native vector.
    // Read back as text representation so JDBC returns a plain String.
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @ColumnTransformer(write = "?::vector", read = "embedding::text")
    private String embedding;

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
