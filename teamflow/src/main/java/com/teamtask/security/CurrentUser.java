package com.teamtask.security;

import jakarta.enterprise.context.RequestScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Convenient view of "who is calling right now", read from the verified JWT.
 *
 * @RequestScoped = one instance per HTTP request (each request carries its
 * own token). Quarkus injects the parsed JsonWebToken for us; by the time
 * this runs, the signature and expiry have already been checked.
 *
 * Resources inject this and hand it to services, which use it for
 * OWNERSHIP checks ("is this YOUR project?") - that's object-level
 * authorization, one step finer than role checks.
 */
@RequestScoped
public class CurrentUser {

    private final JsonWebToken jwt;

    public CurrentUser(JsonWebToken jwt) {
        this.jwt = jwt;
    }

    /** The user's database id (we put it in the token's "sub" claim at login). */
    public Long id() {
        return Long.valueOf(jwt.getSubject());
    }

    /** The user's email (the "upn" claim). */
    public String email() {
        return jwt.getName();
    }

    /** True if the token's groups claim contains ADMIN. */
    public boolean isAdmin() {
        return jwt.getGroups().contains("ADMIN");
    }
}
