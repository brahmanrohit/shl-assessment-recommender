package com.teamtask.exception;

/**
 * Thrown when a project id doesn't exist. Mapped to HTTP 404.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Project with id " + id + " was not found");
    }
}
