package com.codewithsam.prsense.exception;

import com.codewithsam.prsense.dto.response.ErrorResponse;
import com.codewithsam.prsense.mcp.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AzureApiException.class)
    public ResponseEntity<ErrorResponse> handleAzureApiException(AzureApiException ex) {
        log.error("Azure DevOps API error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error("Azure DevOps API error: " + ex.getMessage()));
    }

    @ExceptionHandler(OpenAiException.class)
    public ResponseEntity<ErrorResponse> handleOpenAiException(OpenAiException ex) {
        log.error("OpenAI API error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error("OpenAI API error: " + ex.getMessage()));
    }

    @ExceptionHandler(EmbeddingGenerationException.class)
    public ResponseEntity<ErrorResponse> handleEmbeddingException(EmbeddingGenerationException ex) {
        log.error("Embedding generation failed: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(error("Embedding generation failed: " + ex.getMessage()));
    }

    @ExceptionHandler(VectorSearchException.class)
    public ResponseEntity<ErrorResponse> handleVectorSearchException(VectorSearchException ex) {
        log.error("Vector search failed: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("Vector search failed: " + ex.getMessage()));
    }

    @ExceptionHandler(RepositoryIndexingException.class)
    public ResponseEntity<ErrorResponse> handleIndexingException(RepositoryIndexingException ex) {
        log.error("Repository indexing failed: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error("Indexing failed: " + ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error(ex.getMessage()));
    }

    @ExceptionHandler(ToolExecutionException.class)
    public ResponseEntity<ErrorResponse> handleToolExecutionException(ToolExecutionException ex) {
        log.error("MCP tool execution failed [{}]: {}", ex.getToolName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("Tool execution failed [" + ex.getToolName() + "]: " + ex.getMessage()));
    }

    @ExceptionHandler({ReferenceSearchException.class, KnowledgeGraphException.class})
    public ResponseEntity<ErrorResponse> handleKnowledgeExceptions(RuntimeException ex) {
        log.error("Knowledge graph / reference search error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(ex.getMessage()));
    }

    @ExceptionHandler(ContextRetrievalException.class)
    public ResponseEntity<ErrorResponse> handleContextRetrievalException(ContextRetrievalException ex) {
        log.error("Context retrieval failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(ex.getMessage()));
    }

    @ExceptionHandler(ReviewHistoryException.class)
    public ResponseEntity<ErrorResponse> handleReviewHistoryException(ReviewHistoryException ex) {
        log.warn("Review history error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error("Validation failed: " + message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("Internal server error"));
    }

    private ErrorResponse error(String message) {
        return ErrorResponse.builder().status("error").message(message).build();
    }
}
