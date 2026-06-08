package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.constants.SymbolType;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.mcp.config.McpProperties;
import com.codewithsam.prsense.mcp.dto.SearchCodeRequest;
import com.codewithsam.prsense.mcp.response.CodeSearchResult;
import com.codewithsam.prsense.mcp.service.impl.CodeSearchToolServiceImpl;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import com.codewithsam.prsense.vector.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeSearchToolServiceImplTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private CodeChunkRepository codeChunkRepository;

    private CodeSearchToolServiceImpl service;

    @BeforeEach
    void setUp() {
        McpProperties props = new McpProperties();
        service = new CodeSearchToolServiceImpl(embeddingService, codeChunkRepository, props);
    }

    @Test
    void search_returnsMatchesFromRepository() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        CodeChunkEntity chunk = CodeChunkEntity.builder()
                .id(UUID.randomUUID())
                .repository("my-repo")
                .className("UserService")
                .methodName("createUser")
                .symbolType(SymbolType.METHOD)
                .filePath("src/UserService.java")
                .content("public User createUser(String name) { return repo.save(new User(name)); }")
                .build();

        when(codeChunkRepository.findTopNSimilar(eq("my-repo"), anyString(), anyInt()))
                .thenReturn(List.of(chunk));

        SearchCodeRequest request = SearchCodeRequest.builder()
                .query("user creation logic")
                .repository("my-repo")
                .build();

        CodeSearchResult result = service.search(request);

        assertThat(result.getTotalResults()).isEqualTo(1);
        assertThat(result.getMatches().get(0).getClassName()).isEqualTo("UserService");
        assertThat(result.getMatches().get(0).getMethodName()).isEqualTo("createUser");
        assertThat(result.getMatches().get(0).getContentSnippet()).isNotBlank();
    }

    @Test
    void search_cappedAtMaxSearchLimit() {
        McpProperties props = new McpProperties();
        props.setMaxSearchLimit(5);
        service = new CodeSearchToolServiceImpl(embeddingService, codeChunkRepository, props);

        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(codeChunkRepository.findTopNSimilar(anyString(), anyString(), eq(5)))
                .thenReturn(List.of());

        SearchCodeRequest request = SearchCodeRequest.builder()
                .query("anything")
                .repository("repo")
                .limit(100) // exceeds max
                .build();

        service.search(request);
        // verify the limit was capped (captured by mockito eq(5))
    }

    @Test
    void search_usesDefaultLimitWhenNotSpecified() {
        McpProperties props = new McpProperties();
        props.setDefaultSearchLimit(10);
        service = new CodeSearchToolServiceImpl(embeddingService, codeChunkRepository, props);

        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(codeChunkRepository.findTopNSimilar(anyString(), anyString(), eq(10)))
                .thenReturn(List.of());

        service.search(SearchCodeRequest.builder().query("q").repository("r").build());
    }
}
