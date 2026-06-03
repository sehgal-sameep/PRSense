package com.codewithsam.prsense.vector.embedding;

import java.util.List;

// Abstraction over embedding generation — swap OpenAI for Ollama, Azure OpenAI, or Voyage AI
// without changing any callers.
public interface EmbeddingService {

    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);
}
