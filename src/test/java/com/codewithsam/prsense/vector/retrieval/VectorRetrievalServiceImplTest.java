package com.codewithsam.prsense.vector.retrieval;

import com.codewithsam.prsense.constants.SymbolType;
import com.codewithsam.prsense.dto.response.CodeChunkDto;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.exception.VectorSearchException;
import com.codewithsam.prsense.mapper.CodeChunkMapper;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import com.codewithsam.prsense.util.VectorUtil;
import com.codewithsam.prsense.vector.embedding.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorRetrievalServiceImplTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private CodeChunkRepository codeChunkRepository;
    @Mock private CodeChunkMapper codeChunkMapper;

    @InjectMocks
    private VectorRetrievalServiceImpl retrievalService;

    @Test
    void search_returnsMatchingChunks() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        CodeChunkEntity entity = CodeChunkEntity.builder()
                .id(UUID.randomUUID())
                .repository("my-repo")
                .className("UserService")
                .symbolType(SymbolType.METHOD)
                .content("public User createUser(...) {}")
                .build();
        CodeChunkDto dto = CodeChunkDto.builder().className("UserService").build();

        when(embeddingService.embed("find users")).thenReturn(embedding);
        when(codeChunkRepository.findTopNSimilar(eq("my-repo"), anyString(), eq(5)))
                .thenReturn(List.of(entity));
        when(codeChunkMapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        List<CodeChunkDto> results = retrievalService.search("my-repo", "find users", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClassName()).isEqualTo("UserService");
    }

    @Test
    void search_throwsVectorSearchExceptionOnFailure() {
        when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("API down"));

        assertThatThrownBy(() -> retrievalService.search("repo", "query", 5))
                .isInstanceOf(VectorSearchException.class);
    }

    @Test
    void toVectorString_formatsCorrectly() {
        float[] embedding = {0.1f, 0.25f, 0.5f};
        String result = VectorUtil.toVectorString(embedding);
        assertThat(result).startsWith("[").endsWith("]").contains("0.1").contains("0.25");
    }
}
