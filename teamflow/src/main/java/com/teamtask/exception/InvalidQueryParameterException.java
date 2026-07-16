package com.teamtask.exception;

/**
 * Semantically invalid query parameter (e.g. sort=notAColumn) -> 400.
 * Sort fields are whitelisted before reaching ORDER BY (injection defense).
 */
public class InvalidQueryParameterException extends ApiException {

    public InvalidQueryParameterException(String message) {
        super(400, message);
    }
}
