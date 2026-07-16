package com.teamtask;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * Tests for signup and login - including the security behaviors that
 * interviewers ask about: no password leaks, vague login errors, 409 on
 * duplicate emails, and signups never becoming ADMIN.
 */
@QuarkusTest
class AuthResourceTest {

    @Test
    void signup_returns201_withoutPasswordHash() {
        given()
            .contentType("application/json")
            .body("""
                { "email": "alice@test.local", "password": "alice-pass-123", "displayName": "Alice" }
                """)
            .when().post("/api/auth/signup")
            .then()
            .statusCode(201)
            .body("email", equalTo("alice@test.local"))
            .body("role", equalTo("MEMBER"))          // never ADMIN from signup
            .body("passwordHash", nullValue());       // the hash must NEVER leak
    }

    @Test
    void signup_duplicateEmail_returns409() {
        String body = """
            { "email": "bob@test.local", "password": "bob-pass-1234", "displayName": "Bob" }
            """;

        given().contentType("application/json").body(body)
            .when().post("/api/auth/signup")
            .then().statusCode(201);

        given().contentType("application/json").body(body)
            .when().post("/api/auth/signup")
            .then().statusCode(409);
    }

    @Test
    void signup_invalidEmail_returns400() {
        given()
            .contentType("application/json")
            .body("""
                { "email": "not-an-email", "password": "some-pass-123", "displayName": "X" }
                """)
            .when().post("/api/auth/signup")
            .then().statusCode(400);
    }

    @Test
    void login_returnsToken() {
        given()
            .contentType("application/json")
            .body("""
                { "email": "carol@test.local", "password": "carol-pass-123", "displayName": "Carol" }
                """)
            .when().post("/api/auth/signup");

        given()
            .contentType("application/json")
            .body("""
                { "email": "carol@test.local", "password": "carol-pass-123" }
                """)
            .when().post("/api/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .body("user.email", equalTo("carol@test.local"));
    }

    @Test
    void login_wrongPassword_returns401() {
        given()
            .contentType("application/json")
            .body("""
                { "email": "admin@teamflow.local", "password": "definitely-wrong" }
                """)
            .when().post("/api/auth/login")
            .then()
            .statusCode(401)
            // Vague on purpose: never reveal whether the email exists.
            .body("message", equalTo("Invalid email or password"));
    }
}
