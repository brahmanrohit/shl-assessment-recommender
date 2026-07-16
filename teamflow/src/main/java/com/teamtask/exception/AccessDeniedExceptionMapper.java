package com.teamtask.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * AccessDeniedException -> HTTP 403 with our standard error shape.
 */
@Provider
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    @Override
    public Response toResponse(AccessDeniedException exception) {
        ErrorResponse error = new ErrorResponse(
                Response.Status.FORBIDDEN.getStatusCode(),
                exception.getMessage());
        return Response.status(Response.Status.FORBIDDEN).entity(error).build();
    }
}
