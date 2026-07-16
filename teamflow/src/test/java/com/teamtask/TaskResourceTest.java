package com.teamtask;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Integration tests. @QuarkusTest boots the whole application (and, thanks to
 * Quarkus Dev Services, automatically starts a throwaway MySQL container via
 * Docker) so we can hit the real HTTP endpoints.
 *
 * Run with:  mvn test    (requires Docker to be running)
 */
@QuarkusTest
class TaskResourceTest {

    @Test
    void listTasks_returnsOk() {
        given()
            .when().get("/api/tasks")
            .then().statusCode(200);
    }

    @Test
    void createTask_returns201WithTitle() {
        String body = """
            { "title": "Write the README", "priority": "HIGH" }
            """;

        given()
            .contentType("application/json")
            .body(body)
            .when().post("/api/tasks")
            .then()
            .statusCode(201)
            .body("title", equalTo("Write the README"))
            .body("status", equalTo("TODO"));   // default applied
    }

    @Test
    void createTask_withoutTitle_returns400() {
        given()
            .contentType("application/json")
            .body("{}")
            .when().post("/api/tasks")
            .then().statusCode(400);
    }

    @Test
    void getUnknownTask_returns404() {
        given()
            .when().get("/api/tasks/999999")
            .then().statusCode(404);
    }
}
