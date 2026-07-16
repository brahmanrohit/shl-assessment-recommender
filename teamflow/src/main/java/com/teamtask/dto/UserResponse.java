package com.teamtask.dto;

import com.teamtask.model.Role;
import com.teamtask.model.User;

import java.time.LocalDateTime;

/**
 * Public view of a user. Notice what is MISSING: the password hash.
 * A response DTO is our guarantee that sensitive columns never leak out.
 */
public record UserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt());
    }
}
