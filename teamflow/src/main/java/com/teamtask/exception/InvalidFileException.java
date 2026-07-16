package com.teamtask.exception;

/** Upload rejected (too big, wrong type, missing) -> 400. */
public class InvalidFileException extends ApiException {

    public InvalidFileException(String message) {
        super(400, message);
    }
}
