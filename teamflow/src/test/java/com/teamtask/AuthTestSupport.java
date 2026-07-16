package com.teamtask;

import static io.restassured.RestAssured.given;

/**
 * Test helper: obtains real JWTs by calling the actual auth endpoints,
 * exactly like a client application would.
 *
 * - memberToken(): signs up a test member (ignoring "already exists" on
 *   later calls) and logs in.
 * - adminToken(): logs in as the bootstrap admin that AdminBootstrap
 *   creates at startup (default credentials from application.properties).
 */
final class AuthTestSupport {

    private static final String MEMBER_EMAIL = "member@test.local";
    private static final String MEMBER_PASSWORD = "member-pass-123";

    private AuthTestSupport() {
    }

    static String memberToken() {
        // Sign up; a 409 on later calls just means the user already exists.
        given()
            .contentType("application/json")
            .body("""
                { "email": "%s", "password": "%s", "displayName": "Test Member" }
                """.formatted(MEMBER_EMAIL, MEMBER_PASSWORD))
            .when().post("/api/auth/signup");

        return login(MEMBER_EMAIL, MEMBER_PASSWORD);
    }

    static String adminToken() {
        return login("admin@teamflow.local", "admin1234");
    }

    private static String login(String email, String password) {
        return given()
            .contentType("application/json")
            .body("""
                { "email": "%s", "password": "%s" }
                """.formatted(email, password))
            .when().post("/api/auth/login")
            .then().statusCode(200)
            .extract().path("token");
    }
}
