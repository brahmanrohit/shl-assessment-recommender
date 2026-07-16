package com.teamtask.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * DuplicateEmailException -> HTTP 409 Conflict with our standard error shape.
 */
@Provider
public class DuplicateEmailExceptionMapper implements ExceptionMapper<DuplicateEmailException> {

    @Override
    public Response toResponse(DuplicateEmailException exception) {
        ErrorResponse error = new ErrorResponse(
                Response.Status.CONFLICT.getStatusCode(),
                exception.getMessage());
        return Response.status(Response.Status.CONFLICT).entity(error).build();
    }
}
