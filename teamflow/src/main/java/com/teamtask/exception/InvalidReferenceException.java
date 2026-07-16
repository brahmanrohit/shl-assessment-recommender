package com.teamtask.exception;

/**
 * Thrown when a request body references another row that doesn't exist
 * (e.g. an assigneeId pointing at no user). The request is well-formed but
 * semantically wrong -> HTTP 400 Bad Request.
 */
public class InvalidReferenceException extends RuntimeException {

    public InvalidReferenceException(String message) {
        super(message);
    }
}
