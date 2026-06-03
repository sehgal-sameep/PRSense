package com.codewithsam.prsense.integration;

import com.codewithsam.prsense.constants.SymbolType;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import com.codewithsam.prsense.util.VectorUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test using a real PostgreSQL + pgvector container.
 * Requires Docker to be available in the test environment.
 *
 * Uses the pgvector Docker image which ships with the vector extension pre-installed.
 */
@SpringBootTest
@Testcontainers
class RepositoryIndexIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("prsense_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CodeChunkRepository codeChunkRepository;

    @AfterEach
    void tearDown() {
        codeChunkRepository.deleteAll();
    }

    @Test
    void saveAndRetrieveCodeChunk() {
        CodeChunkEntity chunk = CodeChunkEntity.builder()
                .repository("test-repo")
                .filePath("src/UserService.java")
                .packageName("com.example")
                .className("UserService")
                .methodName("createUser")
                .symbolType(SymbolType.METHOD)
                .content("public User createUser(String name) { return new User(name); }")
                .contentHash("abc123")
                .embedding(VectorUtil.toVectorString(new float[]{0.1f, 0.2f, 0.3f}))
                .build();

        codeChunkRepository.save(chunk);

        List<CodeChunkEntity> saved = codeChunkRepository.findByRepository("test-repo");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getClassName()).isEqualTo("UserService");
        assertThat(saved.get(0).getEmbedding()).isNotBlank();
    }

    @Test
    void incrementalIndexing_skipsExistingHashes() {
        String contentHash = "deadbeef";
        CodeChunkEntity chunk = CodeChunkEntity.builder()
                .repository("test-repo")
                .filePath("src/Foo.java")
                .className("Foo")
                .symbolType(SymbolType.CLASS)
                .content("class Foo {}")
                .contentHash(contentHash)
                .build();

        codeChunkRepository.save(chunk);

        boolean exists = codeChunkRepository.existsByRepositoryAndFilePathAndContentHash(
                "test-repo", "src/Foo.java", contentHash);
        assertThat(exists).isTrue();
    }

    @Test
    void countByRepository_returnsCorrectCount() {
        for (int i = 0; i < 3; i++) {
            codeChunkRepository.save(CodeChunkEntity.builder()
                    .repository("count-repo")
                    .filePath("src/Class" + i + ".java")
                    .className("Class" + i)
                    .symbolType(SymbolType.CLASS)
                    .content("class Class" + i + " {}")
                    .contentHash("hash" + i)
                    .build());
        }

        assertThat(codeChunkRepository.countByRepository("count-repo")).isEqualTo(3);
    }
}
