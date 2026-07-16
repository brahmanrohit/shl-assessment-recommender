package com.teamtask.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * An EXCEPTION MAPPER catches a specific exception anywhere in the app and
 * turns it into a proper HTTP response.
 *
 * Here: whenever a TaskNotFoundException is thrown, the client receives a
 * clean "404 Not Found" with a JSON body, instead of an ugly 500 stack trace.
 */
@Provider
public class TaskNotFoundExceptionMapper implements ExceptionMapper<TaskNotFoundException> {

    @Override
    public Response toResponse(TaskNotFoundException exception) {
        ErrorResponse error = new ErrorResponse(
                Response.Status.NOT_FOUND.getStatusCode(),
                exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }
}
