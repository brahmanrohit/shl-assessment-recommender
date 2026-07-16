package com.teamtask.exception;

/**
 * Thrown when a query parameter is well-formed HTTP but semantically invalid
 * (e.g. sort=notAColumn). Mapped to HTTP 400.
 *
 * SECURITY NOTE: sort fields end up inside an ORDER BY clause. Anything not
 * on the whitelist MUST be rejected - concatenating raw user input into a
 * query string is how injection vulnerabilities are born.
 */
public class InvalidQueryParameterException extends RuntimeException {

    public InvalidQueryParameterException(String message) {
        super(message);
    }
}
