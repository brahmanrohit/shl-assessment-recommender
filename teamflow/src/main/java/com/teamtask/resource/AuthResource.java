package com.teamtask.resource;

import com.teamtask.dto.AuthResponse;
import com.teamtask.dto.LoginRequest;
import com.teamtask.dto.SignupRequest;
import com.teamtask.dto.UserResponse;
import com.teamtask.model.User;
import com.teamtask.security.TokenService;
import com.teamtask.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The two doors into the system:
 *
 *   POST /api/auth/signup -> create an account            (201)
 *   POST /api/auth/login  -> exchange credentials for JWT (200)
 *
 * @PermitAll: these are the ONLY endpoints that work without a token -
 * you obviously can't be logged in before you log in.
 */
@Path("/api/auth")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthResource(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @POST
    @Path("/signup")
    public Response signup(@Valid SignupRequest request) {
        User created = authService.signup(request);
        return Response.status(Response.Status.CREATED)
                .entity(UserResponse.from(created))
                .build();
    }

    @POST
    @Path("/login")
    public AuthResponse login(@Valid LoginRequest request) {
        User user = authService.login(request);
        String token = tokenService.generate(user);
        return new AuthResponse(
                token,
                "Bearer",
                TokenService.TOKEN_LIFETIME.toSeconds(),
                UserResponse.from(user));
    }
}
