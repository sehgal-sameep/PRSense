package com.codewithsam.prsense.repository;

import com.codewithsam.prsense.entity.CodeChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunkEntity, UUID> {

    List<CodeChunkEntity> findByRepository(String repository);

    boolean existsByRepositoryAndFilePathAndContentHash(
            String repository, String filePath, String contentHash);

    Optional<CodeChunkEntity> findTopByRepositoryOrderByCreatedAtDesc(String repository);

    long countByRepository(String repository);

    @Query("SELECT DISTINCT c.filePath FROM CodeChunkEntity c WHERE c.repository = :repository")
    List<String> findDistinctFilePathsByRepository(@Param("repository") String repository);

    @Query("SELECT CAST(c.symbolType AS string), COUNT(c) FROM CodeChunkEntity c " +
           "WHERE c.repository = :repository GROUP BY c.symbolType")
    List<Object[]> countBySymbolTypeForRepository(@Param("repository") String repository);

    @Modifying
    @Query("DELETE FROM CodeChunkEntity c WHERE c.repository = :repository AND c.filePath = :filePath")
    void deleteByRepositoryAndFilePath(
            @Param("repository") String repository, @Param("filePath") String filePath);

    // Cosine similarity search — embedding param must be in pgvector string format "[v1,v2,...]"
    @Query(value = """
            SELECT * FROM code_chunks
            WHERE repository = :repository
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<CodeChunkEntity> findTopNSimilar(
            @Param("repository") String repository,
            @Param("embedding") String embedding,
            @Param("limit") int limit);
}
