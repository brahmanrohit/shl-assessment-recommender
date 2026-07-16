package com.teamtask.exception;

/**
 * Thrown when an authenticated user tries to touch a resource that belongs
 * to someone else (ownership / object-level authorization).
 * Mapped to HTTP 403 Forbidden - "I know who you are, but this isn't yours".
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String resource, Long id) {
        super("You do not have access to " + resource + " " + id);
    }
}
