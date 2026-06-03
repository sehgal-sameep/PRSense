package com.codewithsam.prsense.util;

// Converts between float[] and pgvector's string representation "[v1,v2,...]"
public final class VectorUtil {

    private VectorUtil() {}

    public static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static float[] fromVectorString(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank()) return new float[0];
        String inner = vectorStr.strip();
        if (inner.startsWith("[")) inner = inner.substring(1, inner.length() - 1);
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
