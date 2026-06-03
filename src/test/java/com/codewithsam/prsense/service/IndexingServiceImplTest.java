package com.codewithsam.prsense.service;

import com.codewithsam.prsense.config.IndexingProperties;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.exception.RepositoryIndexingException;
import com.codewithsam.prsense.parser.CodeParser;
import com.codewithsam.prsense.parser.ParsedSymbol;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import com.codewithsam.prsense.service.impl.IndexingServiceImpl;
import com.codewithsam.prsense.vector.embedding.EmbeddingService;
import com.codewithsam.prsense.vector.indexing.CodeChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexingServiceImplTest {

    @Mock private CodeParser codeParser;
    @Mock private CodeChunker codeChunker;
    @Mock private EmbeddingService embeddingService;
    @Mock private CodeChunkRepository codeChunkRepository;

    @TempDir
    Path tempDir;

    private IndexingServiceImpl indexingService;

    @BeforeEach
    void setUp() {
        IndexingProperties props = new IndexingProperties();
        props.setSupportedExtensions(List.of(".java"));
        props.setMaxFileSizeBytes(100 * 1024L);

        indexingService = new IndexingServiceImpl(
                List.of(codeParser), codeChunker, embeddingService, codeChunkRepository, props);
    }

    @Test
    void indexRepository_throwsForNonExistentPath() {
        assertThatThrownBy(() -> indexingService.indexRepository("repo", "/non/existent/path"))
                .isInstanceOf(RepositoryIndexingException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void indexRepository_skipsFilesWhenChunkAlreadyExists() throws IOException {
        Path javaFile = Files.writeString(tempDir.resolve("Foo.java"),
                "public class Foo { public void bar() {} }");

        ParsedSymbol symbol = ParsedSymbol.builder()
                .filePath(javaFile.toString())
                .className("Foo")
                .content("public void bar() {}")
                .build();

        CodeChunkEntity chunk = CodeChunkEntity.builder()
                .content("public void bar() {}")
                .contentHash("somehash")
                .build();

        when(codeParser.supports(anyString())).thenReturn(true);
        when(codeParser.parse(anyString(), anyString())).thenReturn(List.of(symbol));
        when(codeChunker.chunk(anyString(), anyList())).thenReturn(List.of(chunk));
        when(codeChunkRepository.existsByRepositoryAndFilePathAndContentHash(anyString(), anyString(), anyString()))
                .thenReturn(true); // chunk already exists → skip

        indexingService.indexRepository("repo", tempDir.toString());

        verify(embeddingService, never()).embedBatch(anyList());
        verify(codeChunkRepository, never()).saveAll(anyList());
    }

    @Test
    void indexRepository_indexesNewFiles() throws IOException {
        Path javaFile = Files.writeString(tempDir.resolve("Bar.java"),
                "public class Bar { public void baz() {} }");

        ParsedSymbol symbol = ParsedSymbol.builder()
                .filePath(javaFile.toString())
                .className("Bar")
                .content("public void baz() {}")
                .build();

        CodeChunkEntity chunk = CodeChunkEntity.builder()
                .content("public void baz() {}")
                .contentHash("newhash")
                .build();

        when(codeParser.supports(anyString())).thenReturn(true);
        when(codeParser.parse(anyString(), anyString())).thenReturn(List.of(symbol));
        when(codeChunker.chunk(anyString(), anyList())).thenReturn(List.of(chunk));
        when(codeChunkRepository.existsByRepositoryAndFilePathAndContentHash(anyString(), anyString(), anyString()))
                .thenReturn(false);
        when(embeddingService.embedBatch(anyList())).thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));

        indexingService.indexRepository("repo", tempDir.toString());

        verify(codeChunkRepository).saveAll(anyList());
    }
}
