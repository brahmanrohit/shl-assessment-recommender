package com.teamtask.exception;

/**
 * Request body references a row that doesn't exist (e.g. assigneeId
 * pointing at no user) -> 400 Bad Request.
 */
public class InvalidReferenceException extends ApiException {

    public InvalidReferenceException(String message) {
        super(400, message);
    }
}
