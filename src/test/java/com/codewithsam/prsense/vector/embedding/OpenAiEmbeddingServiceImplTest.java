package com.codewithsam.prsense.vector.embedding;

import com.codewithsam.prsense.config.EmbeddingProperties;
import com.codewithsam.prsense.config.OpenAiProperties;
import com.codewithsam.prsense.exception.EmbeddingGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiEmbeddingServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private OpenAiEmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey("test-key");

        EmbeddingProperties embeddingProperties = new EmbeddingProperties();
        embeddingProperties.setModel("text-embedding-3-small");
        embeddingProperties.setDimensions(3);
        embeddingProperties.setBatchSize(2);

        embeddingService = new OpenAiEmbeddingServiceImpl(restTemplate, openAiProperties, embeddingProperties);
    }

    @Test
    void embed_returnsFloatArrayFromOpenAiResponse() {
        Map<String, Object> response = Map.of(
                "data", List.of(Map.of("embedding", List.of(0.1, 0.2, 0.3), "index", 0))
        );
        when(restTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(response));

        float[] result = embeddingService.embed("test input");

        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo(0.1f, org.assertj.core.data.Offset.offset(0.001f));
    }

    @Test
    void embedBatch_processesInBatches() {
        Map<String, Object> response = Map.of(
                "data", List.of(
                        Map.of("embedding", List.of(0.1, 0.2, 0.3), "index", 0),
                        Map.of("embedding", List.of(0.4, 0.5, 0.6), "index", 1)
                )
        );
        when(restTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(response));

        List<float[]> results = embeddingService.embedBatch(List.of("text1", "text2", "text3"));

        // batchSize=2, so 2 API calls for 3 inputs (2+1)
        assertThat(results).hasSize(4); // 2 from first call + 2 from second (mocked same response)
    }

    @Test
    void embed_throwsEmbeddingGenerationExceptionOnApiFailure() {
        when(restTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> embeddingService.embed("text"))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("Failed to generate embeddings");
    }

    @Test
    void embedBatch_returnsEmptyForEmptyInput() {
        assertThat(embeddingService.embedBatch(List.of())).isEmpty();
    }
}
