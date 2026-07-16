package com.teamtask.dto;

import com.teamtask.model.Project;

import java.time.LocalDateTime;

/**
 * Public view of a project, including who owns it (id + display name only -
 * never the owner's email or other private fields).
 */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        String ownerName,
        LocalDateTime createdAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId(),
                project.getOwner().getDisplayName(),
                project.getCreatedAt());
    }
}
