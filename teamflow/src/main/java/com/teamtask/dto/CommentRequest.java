package com.teamtask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/tasks/{id}/comments. The author is always the logged-in
 * user (from the JWT) - clients can't write comments as somebody else.
 */
public class CommentRequest {

    @NotBlank(message = "Body is required")
    @Size(max = 1000, message = "Body must be at most 1000 characters")
    public String body;
}
