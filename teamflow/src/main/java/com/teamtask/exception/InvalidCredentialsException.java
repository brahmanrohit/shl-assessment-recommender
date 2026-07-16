package com.teamtask.exception;

/**
 * Thrown when login fails. IMPORTANT: the message is deliberately vague -
 * we never reveal WHETHER the email exists or the password was wrong,
 * because that information helps attackers enumerate accounts.
 * Mapped to HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
