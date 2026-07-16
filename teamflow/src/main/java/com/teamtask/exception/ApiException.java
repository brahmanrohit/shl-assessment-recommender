package com.teamtask.exception;

/**
 * Base for every "expected" API error. Each subclass just picks its HTTP
 * status; ONE mapper (ApiExceptionMapper) turns them all into the standard
 * ErrorResponse JSON. Before this, every exception had its own copy-pasted
 * mapper class - seven files doing the same ten lines.
 */
public abstract class ApiException extends RuntimeException {

    private final int status;

    protected ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
