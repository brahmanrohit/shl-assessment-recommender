package com.teamtask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST/PUT /api/projects. The owner is NEVER sent by the client -
 * it is always the logged-in user (from the JWT). Letting clients pick an
 * owner would let them create projects "as" someone else.
 */
public class ProjectRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters")
    public String name;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    public String description;
}
