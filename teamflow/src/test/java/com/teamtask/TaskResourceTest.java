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
 * Since Phase 2, a task lives INSIDE a project, so tests first create a
 * project (see AuthTestSupport helpers) - and ownership is enforced: a
 * different member gets 403 on someone else's task.
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
    void createTask_returns201WithProjectId() {
        String token = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(token, "Task create project");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body("""
                { "title": "Write the README", "projectId": %d, "priority": "HIGH" }
                """.formatted(projectId))
            .when().post("/api/tasks")
            .then()
            .statusCode(201)
            .body("title", equalTo("Write the README"))
            .body("status", equalTo("TODO"))            // default applied
            .body("projectId", equalTo(projectId));     // relation persisted
    }

    @Test
    void createTask_withoutTitleOrProject_returns400() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .contentType("application/json")
            .body("{}")
            .when().post("/api/tasks")
            .then().statusCode(400);
    }

    @Test
    void createTask_withUnknownAssignee_returns400() {
        String token = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(token, "Assignee check project");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body("""
                { "title": "Ghost assignee", "projectId": %d, "assigneeId": 999999 }
                """.formatted(projectId))
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
    void getTask_ofAnotherMember_returns403() {
        // Member #1 creates a project + task...
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Ownership project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "Private task");

        // ...member #2 may NOT see it: authenticated (not 401) but forbidden.
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.secondMemberToken())
            .when().get("/api/tasks/" + taskId)
            .then().statusCode(403);
    }

    @Test
    void deleteTask_asProjectOwner_returns204() {
        // Phase 2 ownership rule: the owner may delete tasks in their project.
        String token = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(token, "Delete-own project");
        Integer taskId = AuthTestSupport.createTask(token, projectId, "Task to delete");

        given()
            .header("Authorization", "Bearer " + token)
            .when().delete("/api/tasks/" + taskId)
            .then().statusCode(204);
    }

    @Test
    void deleteTask_ofAnotherMember_returns403() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Delete-foreign project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "Protected task");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.secondMemberToken())
            .when().delete("/api/tasks/" + taskId)
            .then().statusCode(403);
    }

    @Test
    void deleteTask_asAdmin_returns204() {
        String token = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(token, "Delete-as-admin project");
        Integer taskId = AuthTestSupport.createTask(token, projectId, "Task admin deletes");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.adminToken())
            .when().delete("/api/tasks/" + taskId)
            .then().statusCode(204);
    }
}
