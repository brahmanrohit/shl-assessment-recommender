package com.teamtask.exception;

/** Email already registered -> 409 Conflict. */
public class DuplicateEmailException extends ApiException {

    public DuplicateEmailException(String email) {
        super(409, "An account with email '" + email + "' already exists");
    }
}
