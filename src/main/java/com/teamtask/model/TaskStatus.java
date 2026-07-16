package com.teamtask.model;

/**
 * The lifecycle stage of a task.
 * Using an enum (instead of a plain String) means the database can only
 * ever hold one of these three valid values.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
