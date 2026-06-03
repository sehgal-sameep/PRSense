package com.codewithsam.prsense.parser;

import com.codewithsam.prsense.constants.IndexingConstants;
import com.codewithsam.prsense.constants.SymbolType;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class JavaCodeParser implements CodeParser {

    @PostConstruct
    void configureParser() {
        // RAW disables language-level restrictions, accepting any Java version including 21+.
        // More future-proof than pinning a specific level constant.
        StaticJavaParser.setConfiguration(
                new ParserConfiguration()
                        .setLanguageLevel(ParserConfiguration.LanguageLevel.RAW));
        log.debug("JavaParser configured with language level: RAW (Java 21+ compatible)");
    }

    @Override
    public boolean supports(String filePath) {
        return filePath != null && filePath.endsWith(IndexingConstants.JAVA_EXTENSION);
    }

    @Override
    public List<ParsedSymbol> parse(String filePath, String source) {
        List<ParsedSymbol> symbols = new ArrayList<>();
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            String packageName = cu.getPackageDeclaration()
                    .map(p -> p.getName().toString())
                    .orElse("");

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(decl ->
                    extractFromClassOrInterface(filePath, packageName, decl, symbols));

            cu.findAll(EnumDeclaration.class).forEach(decl ->
                    extractFromEnum(filePath, packageName, decl, symbols));

            cu.findAll(RecordDeclaration.class).forEach(decl ->
                    extractFromRecord(filePath, packageName, decl, symbols));

            log.debug("Parsed {} symbol(s) from {}", symbols.size(), filePath);
        } catch (Exception e) {
            log.warn("Failed to parse {}: {}", filePath, e.getMessage());
        }
        return symbols;
    }

    private void extractFromClassOrInterface(String filePath, String packageName,
                                             ClassOrInterfaceDeclaration decl,
                                             List<ParsedSymbol> symbols) {
        String className = decl.getNameAsString();
        SymbolType classType = decl.isInterface() ? SymbolType.INTERFACE : SymbolType.CLASS;

        // Class-level chunk: declaration + fields (no method bodies)
        String classSignature = buildClassSignature(decl);
        symbols.add(ParsedSymbol.builder()
                .filePath(filePath)
                .packageName(packageName)
                .className(className)
                .symbolType(classType)
                .content(classSignature)
                .build());

        // One chunk per method
        decl.getMethods().forEach(method -> {
            String content = classSignature + "\n\n" + method;
            symbols.add(ParsedSymbol.builder()
                    .filePath(filePath)
                    .packageName(packageName)
                    .className(className)
                    .methodName(method.getNameAsString())
                    .symbolType(SymbolType.METHOD)
                    .content(truncate(content))
                    .build());
        });

        // One chunk per constructor
        decl.getConstructors().forEach(ctor -> {
            String content = classSignature + "\n\n" + ctor;
            symbols.add(ParsedSymbol.builder()
                    .filePath(filePath)
                    .packageName(packageName)
                    .className(className)
                    .methodName(className)
                    .symbolType(SymbolType.CONSTRUCTOR)
                    .content(truncate(content))
                    .build());
        });
    }

    private void extractFromEnum(String filePath, String packageName,
                                 EnumDeclaration decl, List<ParsedSymbol> symbols) {
        String className = decl.getNameAsString();
        symbols.add(ParsedSymbol.builder()
                .filePath(filePath)
                .packageName(packageName)
                .className(className)
                .symbolType(SymbolType.ENUM)
                .content(truncate(decl.toString()))
                .build());
    }

    private void extractFromRecord(String filePath, String packageName,
                                   RecordDeclaration decl, List<ParsedSymbol> symbols) {
        String className = decl.getNameAsString();
        symbols.add(ParsedSymbol.builder()
                .filePath(filePath)
                .packageName(packageName)
                .className(className)
                .symbolType(SymbolType.RECORD)
                .content(truncate(decl.toString()))
                .build());

        decl.getMethods().forEach(method -> {
            String content = "// Record: " + className + "\n" + method;
            symbols.add(ParsedSymbol.builder()
                    .filePath(filePath)
                    .packageName(packageName)
                    .className(className)
                    .methodName(method.getNameAsString())
                    .symbolType(SymbolType.METHOD)
                    .content(truncate(content))
                    .build());
        });
    }

    // Builds a class signature including annotations and field declarations (no method bodies)
    private String buildClassSignature(ClassOrInterfaceDeclaration decl) {
        StringBuilder sb = new StringBuilder();
        decl.getAnnotations().forEach(a -> sb.append(a).append("\n"));
        sb.append(decl.isInterface() ? "interface " : "class ").append(decl.getNameAsString());
        if (!decl.getExtendedTypes().isEmpty()) {
            sb.append(" extends ").append(decl.getExtendedTypes().get(0));
        }
        if (!decl.getImplementedTypes().isEmpty()) {
            sb.append(" implements ");
            decl.getImplementedTypes().forEach(t -> sb.append(t).append(", "));
        }
        sb.append(" {\n");
        decl.getFields().forEach(f -> sb.append("    ").append(f).append("\n"));
        sb.append("}");
        return sb.toString();
    }

    private String truncate(String content) {
        if (content.length() <= IndexingConstants.MAX_CONTENT_LENGTH) return content;
        return content.substring(0, IndexingConstants.MAX_CONTENT_LENGTH) + "\n// ... truncated";
    }
}
