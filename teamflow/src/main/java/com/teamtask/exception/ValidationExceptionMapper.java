package com.teamtask.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns validation failures (e.g. a blank title) into a friendly 400 response
 * that tells the client exactly which field was wrong and why.
 *
 * Example response:
 * {
 *   "status": 400,
 *   "message": "Validation failed",
 *   "fieldErrors": { "title": "Title is required" }
 * }
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, String> fieldErrors = new HashMap<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            // propertyPath looks like "create.request.title" - keep the last part.
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.put(field, violation.getMessage());
        }

        ErrorResponse error = new ErrorResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Validation failed",
                fieldErrors);
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
}
