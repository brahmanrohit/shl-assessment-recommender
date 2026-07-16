package com.teamtask.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/auth/signup.
 *
 * NOTE there is deliberately NO "role" field here: if clients could send
 * their own role, anyone could sign up as ADMIN (privilege escalation).
 * Every signup becomes a MEMBER; admins are created by the system.
 */
public class SignupRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    public String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    public String password;

    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must be at most 100 characters")
    public String displayName;
}
