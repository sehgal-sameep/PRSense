package com.codewithsam.prsense.mcp.service.impl;

import com.codewithsam.prsense.mcp.dto.GetFileRequest;
import com.codewithsam.prsense.mcp.response.FileContentResult;
import com.codewithsam.prsense.mcp.service.FileContentToolService;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileContentToolServiceImpl implements FileContentToolService {

    private final CodeChunkRepository codeChunkRepository;

    @Override
    public FileContentResult getFileContent(GetFileRequest request) {
        log.info("File content retrieval — repository: '{}', path: '{}'",
                request.getRepository(), request.getPath());

        // Strategy 1: try reading from local filesystem
        Path localPath = Path.of(request.getPath());
        if (Files.exists(localPath)) {
            return readFromFilesystem(localPath, request);
        }

        // Strategy 2: assemble from indexed chunks (content stored in code_chunks)
        return assembleFromChunks(request);
    }

    private FileContentResult readFromFilesystem(Path path, GetFileRequest request) {
        try {
            String content = Files.readString(path);
            String[] lines = content.split("\n");
            int total = lines.length;

            if (request.getStartLine() != null && request.getEndLine() != null) {
                int start = Math.max(0, request.getStartLine() - 1);
                int end = Math.min(total, request.getEndLine());
                content = String.join("\n", Arrays.copyOfRange(lines, start, end));
                return FileContentResult.builder()
                        .path(request.getPath()).content(content).totalLines(total)
                        .startLine(request.getStartLine()).endLine(request.getEndLine())
                        .truncated(false).build();
            }

            return FileContentResult.builder()
                    .path(request.getPath()).content(content)
                    .totalLines(total).truncated(false).build();
        } catch (IOException e) {
            log.warn("Could not read file from filesystem '{}': {}", path, e.getMessage());
            return assembleFromChunks(request);
        }
    }

    private FileContentResult assembleFromChunks(GetFileRequest request) {
        log.debug("Assembling file content from indexed chunks — path: '{}'", request.getPath());

        String assembled = codeChunkRepository.findByRepository(request.getRepository())
                .stream()
                .filter(c -> request.getPath().equals(c.getFilePath()))
                .map(c -> "// --- " + c.getSymbolType() + ": " + c.getClassName()
                        + (c.getMethodName() != null ? "." + c.getMethodName() : "") + " ---\n"
                        + c.getContent())
                .reduce("", (a, b) -> a + "\n\n" + b)
                .strip();

        if (assembled.isEmpty()) {
            return FileContentResult.builder()
                    .path(request.getPath())
                    .content("File not found in local filesystem or repository index.")
                    .totalLines(0).truncated(false).build();
        }

        String[] lines = assembled.split("\n");
        return FileContentResult.builder()
                .path(request.getPath()).content(assembled)
                .totalLines(lines.length).truncated(false).build();
    }
}
