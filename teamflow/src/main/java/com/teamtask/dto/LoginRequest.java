package com.teamtask.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/auth/login.
 */
public class LoginRequest {

    @NotBlank(message = "Email is required")
    public String email;

    @NotBlank(message = "Password is required")
    public String password;
}
