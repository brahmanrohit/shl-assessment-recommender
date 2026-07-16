package com.teamtask.exception;

/** Unknown task id -> 404. */
public class TaskNotFoundException extends ApiException {

    public TaskNotFoundException(Long id) {
        super(404, "Task with id " + id + " was not found");
    }
}
