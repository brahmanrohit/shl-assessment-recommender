package com.teamtask.exception;

/**
 * Thrown when someone asks for a task id that does not exist.
 * A dedicated exception makes the code read clearly and lets us
 * turn it into a proper HTTP 404 response (see TaskNotFoundExceptionMapper).
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Task with id " + id + " was not found");
    }
}
