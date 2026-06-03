package com.codewithsam.prsense.vector.embedding;

import com.codewithsam.prsense.config.EmbeddingProperties;
import com.codewithsam.prsense.config.OpenAiProperties;
import com.codewithsam.prsense.exception.EmbeddingGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiEmbeddingServiceImpl implements EmbeddingService {

    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    private final RestTemplate restTemplate;
    private final OpenAiProperties openAiProperties;
    private final EmbeddingProperties embeddingProperties;

    @Override
    public float[] embed(String text) {
        log.debug("Generating embedding for text ({} chars)", text.length());
        List<float[]> result = callEmbeddingApi(List.of(text));
        if (result.isEmpty()) {
            throw new EmbeddingGenerationException("OpenAI returned no embeddings for input text");
        }
        return result.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts.isEmpty()) return List.of();
        log.debug("Generating embeddings for {} text(s)", texts.size());

        List<float[]> all = new ArrayList<>();
        int batchSize = embeddingProperties.getBatchSize();

        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            log.debug("Embedding batch {}/{}", (i / batchSize) + 1, (texts.size() + batchSize - 1) / batchSize);
            all.addAll(callEmbeddingApi(batch));
        }

        return all;
    }

    @SuppressWarnings("unchecked")
    private List<float[]> callEmbeddingApi(List<String> inputs) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiProperties.getApiKey());
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = Map.of(
                "model", embeddingProperties.getModel(),
                "input", inputs
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(EMBEDDINGS_URL, request, Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");

            List<float[]> embeddings = new ArrayList<>();
            for (Map<String, Object> item : data) {
                List<Double> rawEmbedding = (List<Double>) item.get("embedding");
                float[] embedding = new float[rawEmbedding.size()];
                for (int i = 0; i < rawEmbedding.size(); i++) {
                    embedding[i] = rawEmbedding.get(i).floatValue();
                }
                embeddings.add(embedding);
            }

            log.debug("Received {} embedding(s) from OpenAI", embeddings.size());
            return embeddings;

        } catch (Exception e) {
            log.error("OpenAI embedding API call failed: {}", e.getMessage(), e);
            throw new EmbeddingGenerationException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }
}
