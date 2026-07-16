package com.teamtask.exception;

import java.util.Map;

/**
 * A consistent shape for every error the API returns, so clients always
 * know what to expect. Example JSON:
 * {
 *   "status": 404,
 *   "message": "Task with id 99 was not found",
 *   "fieldErrors": null
 * }
 */
public class ErrorResponse {

    public int status;
    public String message;

    /** Only used for validation errors: maps a field name to what went wrong. */
    public Map<String, String> fieldErrors;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public ErrorResponse(int status, String message, Map<String, String> fieldErrors) {
        this.status = status;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }
}
