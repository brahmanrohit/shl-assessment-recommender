package com.teamtask;

import static io.restassured.RestAssured.given;

/**
 * Test helper: obtains real JWTs by calling the actual auth endpoints,
 * exactly like a client application would, plus small setup helpers.
 *
 * Two different members exist so ownership tests can prove that user B
 * cannot touch user A's projects (the Phase 2 access rule).
 */
final class AuthTestSupport {

    private static final String MEMBER_EMAIL = "member@test.local";
    private static final String MEMBER_PASSWORD = "member-pass-123";

    private static final String OTHER_EMAIL = "other@test.local";
    private static final String OTHER_PASSWORD = "other-pass-123";

    private AuthTestSupport() {
    }

    /** JWT for test member #1 (signed up on first use). */
    static String memberToken() {
        signup(MEMBER_EMAIL, MEMBER_PASSWORD, "Test Member");
        return login(MEMBER_EMAIL, MEMBER_PASSWORD);
    }

    /** JWT for a DIFFERENT member - used to test ownership boundaries. */
    static String secondMemberToken() {
        signup(OTHER_EMAIL, OTHER_PASSWORD, "Other Member");
        return login(OTHER_EMAIL, OTHER_PASSWORD);
    }

    /** JWT for the bootstrap admin created by AdminBootstrap at startup. */
    static String adminToken() {
        return login("admin@teamflow.local", "admin1234");
    }

    /** Create a project as the given token's user; returns the new id. */
    static Integer createProject(String token, String name) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body("""
                { "name": "%s", "description": "created by test" }
                """.formatted(name))
            .when().post("/api/projects")
            .then().statusCode(201)
            .extract().path("id");
    }

    /** Create a task inside a project; returns the new id. */
    static Integer createTask(String token, Integer projectId, String title) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body("""
                { "title": "%s", "projectId": %d, "priority": "LOW" }
                """.formatted(title, projectId))
            .when().post("/api/tasks")
            .then().statusCode(201)
            .extract().path("id");
    }

    private static void signup(String email, String password, String displayName) {
        // A 409 on later calls just means the user already exists - fine.
        given()
            .contentType("application/json")
            .body("""
                { "email": "%s", "password": "%s", "displayName": "%s" }
                """.formatted(email, password, displayName))
            .when().post("/api/auth/signup");
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
