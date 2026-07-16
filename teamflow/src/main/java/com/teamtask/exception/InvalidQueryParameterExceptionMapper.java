package com.teamtask.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * InvalidQueryParameterException -> HTTP 400 with our standard error shape.
 */
@Provider
public class InvalidQueryParameterExceptionMapper implements ExceptionMapper<InvalidQueryParameterException> {

    @Override
    public Response toResponse(InvalidQueryParameterException exception) {
        ErrorResponse error = new ErrorResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
}
