package com.teamtask.dto;

import com.teamtask.model.TaskPriority;
import com.teamtask.model.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * The data a CLIENT sends us when creating or updating a task (the request body).
 *
 * We keep this separate from the Task entity on purpose:
 *  - the client should NOT be able to set the id or createdAt
 *  - we can validate the incoming data with annotations below
 *
 * When a request arrives, the validation rules are checked automatically
 * (because the resource marks the parameter with @Valid). If a rule fails,
 * the client gets a clear 400 error instead of a crash.
 */
public class TaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    public String title;

    /** Which project the task belongs to (Phase 2). Required. */
    @NotNull(message = "projectId is required")
    public Long projectId;

    /** Optional: id of the user working on this task. */
    @Positive(message = "assigneeId must be a positive id")
    public Long assigneeId;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    public String description;

    /** Optional. If omitted on create, the entity defaults to TODO. */
    public TaskStatus status;

    /** Optional. If omitted on create, the entity defaults to MEDIUM. */
    public TaskPriority priority;

    @FutureOrPresent(message = "Due date cannot be in the past")
    public LocalDate dueDate;
}
