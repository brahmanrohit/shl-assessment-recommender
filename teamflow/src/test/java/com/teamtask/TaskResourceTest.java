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
 * Since Phase 1, /api/tasks requires a JWT - so these tests first sign up /
 * log in through the REAL auth endpoints and send the token, exactly like a
 * client would. (See AuthTestSupport for the helpers.)
 *
 * Run with:  mvn test    (requires Docker to be running)
 */
@QuarkusTest
class TaskResourceTest {

    @Test
    void listTasks_withoutToken_returns401() {
        given()
            .when().get("/api/tasks")
            .then().statusCode(401);
    }

    @Test
    void listTasks_withToken_returnsOk() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/tasks")
            .then().statusCode(200);
    }

    @Test
    void createTask_returns201WithTitle() {
        String body = """
            { "title": "Write the README", "priority": "HIGH" }
            """;

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
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
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .contentType("application/json")
            .body("{}")
            .when().post("/api/tasks")
            .then().statusCode(400);
    }

    @Test
    void getUnknownTask_returns404() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/tasks/999999")
            .then().statusCode(404);
    }

    @Test
    void deleteTask_asMember_returns403() {
        // Members may not delete - only ADMIN can (authorization, not authentication).
        Integer id = createSampleTask();

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().delete("/api/tasks/" + id)
            .then().statusCode(403);
    }

    @Test
    void deleteTask_asAdmin_returns204() {
        Integer id = createSampleTask();

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.adminToken())
            .when().delete("/api/tasks/" + id)
            .then().statusCode(204);
    }

    /** Creates a task as a member and returns its id. */
    private Integer createSampleTask() {
        return given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .contentType("application/json")
            .body("""
                { "title": "Task to delete", "priority": "LOW" }
                """)
            .when().post("/api/tasks")
            .then().statusCode(201)
            .extract().path("id");
    }
}
