package com.codewithsam.prsense.vector.indexing;

import com.codewithsam.prsense.constants.IndexingConstants;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.parser.ParsedSymbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

// Converts parsed symbols into CodeChunkEntity objects (without embeddings — those are added later)
@Component
@Slf4j
public class CodeChunker {

    public List<CodeChunkEntity> chunk(String repositoryName, List<ParsedSymbol> symbols) {
        List<CodeChunkEntity> chunks = symbols.stream()
                .map(symbol -> toChunk(repositoryName, symbol))
                .collect(Collectors.toList());

        log.debug("Created {} chunk(s) from {} symbol(s) for repository '{}'",
                chunks.size(), symbols.size(), repositoryName);
        return chunks;
    }

    private CodeChunkEntity toChunk(String repositoryName, ParsedSymbol symbol) {
        return CodeChunkEntity.builder()
                .repository(repositoryName)
                .filePath(symbol.getFilePath())
                .packageName(symbol.getPackageName())
                .className(symbol.getClassName())
                .methodName(symbol.getMethodName())
                .symbolType(symbol.getSymbolType())
                .content(symbol.getContent())
                .contentHash(sha256(symbol.getContent()))
                .build();
    }

    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(IndexingConstants.HASH_ALGORITHM);
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
