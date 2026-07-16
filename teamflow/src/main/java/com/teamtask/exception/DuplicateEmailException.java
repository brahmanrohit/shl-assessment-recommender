package com.teamtask.exception;

/**
 * Thrown when someone signs up with an email that is already registered.
 * Mapped to HTTP 409 Conflict.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
