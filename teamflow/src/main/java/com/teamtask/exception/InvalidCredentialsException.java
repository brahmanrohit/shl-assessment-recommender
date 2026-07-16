package com.teamtask.exception;

/**
 * Login failed -> 401. The message is deliberately vague: never reveal
 * whether the email exists or the password was wrong (account enumeration).
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(401, "Invalid email or password");
    }
}
