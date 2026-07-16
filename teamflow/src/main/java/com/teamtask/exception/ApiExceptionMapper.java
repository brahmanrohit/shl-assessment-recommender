package com.teamtask.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * The single mapper for all ApiExceptions. JAX-RS picks the mapper whose
 * type is the nearest superclass of the thrown exception, so every
 * ApiException subclass lands here automatically.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException exception) {
        return Response.status(exception.status())
                .entity(new ErrorResponse(exception.status(), exception.getMessage()))
                .build();
    }
}
