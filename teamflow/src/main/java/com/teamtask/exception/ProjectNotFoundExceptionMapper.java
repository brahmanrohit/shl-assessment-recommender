package com.teamtask.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * ProjectNotFoundException -> HTTP 404 with our standard error shape.
 */
@Provider
public class ProjectNotFoundExceptionMapper implements ExceptionMapper<ProjectNotFoundException> {

    @Override
    public Response toResponse(ProjectNotFoundException exception) {
        ErrorResponse error = new ErrorResponse(
                Response.Status.NOT_FOUND.getStatusCode(),
                exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }
}
