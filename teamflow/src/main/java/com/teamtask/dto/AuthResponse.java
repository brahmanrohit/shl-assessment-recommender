package com.teamtask.dto;

/**
 * Body returned by POST /api/auth/login: the signed JWT plus how to use it.
 * The client sends it back on every request as:
 *   Authorization: Bearer <token>
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}
