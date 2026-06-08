package com.codewithsam.prsense.mcp.exception;

public class ContextRetrievalException extends RuntimeException {

    public ContextRetrievalException(String message) { super(message); }

    public ContextRetrievalException(String message, Throwable cause) { super(message, cause); }
}
