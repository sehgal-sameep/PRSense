package com.codewithsam.prsense.exception;

public class RepositoryIndexingException extends RuntimeException {

    public RepositoryIndexingException(String message) {
        super(message);
    }

    public RepositoryIndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}
