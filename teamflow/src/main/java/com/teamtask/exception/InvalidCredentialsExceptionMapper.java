package com.teamtask.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * InvalidCredentialsException -> HTTP 401 Unauthorized with our standard shape.
 */
@Provider
public class InvalidCredentialsExceptionMapper implements ExceptionMapper<InvalidCredentialsException> {

    @Override
    public Response toResponse(InvalidCredentialsException exception) {
        ErrorResponse error = new ErrorResponse(
                Response.Status.UNAUTHORIZED.getStatusCode(),
                exception.getMessage());
        return Response.status(Response.Status.UNAUTHORIZED).entity(error).build();
    }
}
