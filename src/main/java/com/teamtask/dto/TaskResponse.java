package com.teamtask.dto;

import com.teamtask.model.Task;
import com.teamtask.model.TaskPriority;
import com.teamtask.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The data we send BACK to the client (the response body).
 *
 * This is a Java "record" - a compact, immutable data carrier (modern Java).
 * Using a response DTO means we control exactly which fields are exposed,
 * and we never accidentally leak internal database details.
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        LocalDateTime createdAt) {

    /** Convert a Task entity from the database into a response object. */
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt());
    }
}
