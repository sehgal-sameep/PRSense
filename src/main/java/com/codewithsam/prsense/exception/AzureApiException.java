package com.codewithsam.prsense.exception;

public class AzureApiException extends RuntimeException {

    public AzureApiException(String message) {
        super(message);
    }

    public AzureApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
