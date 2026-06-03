package com.codewithsam.prsense.vector.indexing;

import com.codewithsam.prsense.constants.SymbolType;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.parser.ParsedSymbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeChunkerTest {

    private CodeChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new CodeChunker();
    }

    @Test
    void chunk_mapsSymbolsToEntities() {
        ParsedSymbol symbol = ParsedSymbol.builder()
                .filePath("src/UserService.java")
                .packageName("com.example")
                .className("UserService")
                .methodName("createUser")
                .symbolType(SymbolType.METHOD)
                .content("public User createUser(String name) { return new User(name); }")
                .build();

        List<CodeChunkEntity> chunks = chunker.chunk("my-repo", List.of(symbol));

        assertThat(chunks).hasSize(1);
        CodeChunkEntity chunk = chunks.get(0);
        assertThat(chunk.getRepository()).isEqualTo("my-repo");
        assertThat(chunk.getFilePath()).isEqualTo("src/UserService.java");
        assertThat(chunk.getClassName()).isEqualTo("UserService");
        assertThat(chunk.getMethodName()).isEqualTo("createUser");
        assertThat(chunk.getSymbolType()).isEqualTo(SymbolType.METHOD);
        assertThat(chunk.getContentHash()).isNotBlank();
    }

    @Test
    void chunk_generatesConsistentHashForSameContent() {
        String content = "public void foo() {}";
        ParsedSymbol s1 = ParsedSymbol.builder().filePath("A.java").className("A").symbolType(SymbolType.METHOD).content(content).build();
        ParsedSymbol s2 = ParsedSymbol.builder().filePath("B.java").className("B").symbolType(SymbolType.METHOD).content(content).build();

        List<CodeChunkEntity> chunks = chunker.chunk("repo", List.of(s1, s2));

        assertThat(chunks.get(0).getContentHash()).isEqualTo(chunks.get(1).getContentHash());
    }

    @Test
    void chunk_generatesDifferentHashForDifferentContent() {
        ParsedSymbol s1 = ParsedSymbol.builder().filePath("A.java").className("A").symbolType(SymbolType.METHOD).content("public void foo() {}").build();
        ParsedSymbol s2 = ParsedSymbol.builder().filePath("A.java").className("A").symbolType(SymbolType.METHOD).content("public void bar() {}").build();

        List<CodeChunkEntity> chunks = chunker.chunk("repo", List.of(s1, s2));

        assertThat(chunks.get(0).getContentHash()).isNotEqualTo(chunks.get(1).getContentHash());
    }

    @Test
    void chunk_returnsEmptyForEmptySymbolList() {
        assertThat(chunker.chunk("repo", List.of())).isEmpty();
    }
}
