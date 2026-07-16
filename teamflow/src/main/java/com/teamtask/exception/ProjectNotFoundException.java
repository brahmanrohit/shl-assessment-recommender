package com.teamtask.exception;

/** Unknown project id -> 404. */
public class ProjectNotFoundException extends ApiException {

    public ProjectNotFoundException(Long id) {
        super(404, "Project with id " + id + " was not found");
    }
}
