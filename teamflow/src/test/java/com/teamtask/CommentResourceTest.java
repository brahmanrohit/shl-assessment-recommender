package com.teamtask;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Tests for comments: adding/listing under a task, author stamping,
 * and the inherited ownership rule (can't comment on someone else's task).
 */
@QuarkusTest
class CommentResourceTest {

    @Test
    void addAndListComments_onOwnTask_works() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Comment project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "Discussed task");

        given()
            .header("Authorization", "Bearer " + owner)
            .contentType("application/json")
            .body("""
                { "body": "First comment!" }
                """)
            .when().post("/api/tasks/" + taskId + "/comments")
            .then()
            .statusCode(201)
            .body("body", equalTo("First comment!"))
            .body("authorName", equalTo("Test Member")); // author = the caller

        given()
            .header("Authorization", "Bearer " + owner)
            .when().get("/api/tasks/" + taskId + "/comments")
            .then()
            .statusCode(200)
            .body("totalElements", equalTo(1))
            .body("content[0].body", equalTo("First comment!"));
    }

    @Test
    void addComment_onAnotherMembersTask_returns403() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Private comment project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "Private task");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.secondMemberToken())
            .contentType("application/json")
            .body("""
                { "body": "Sneaky comment" }
                """)
            .when().post("/api/tasks/" + taskId + "/comments")
            .then().statusCode(403);
    }

    @Test
    void addComment_withBlankBody_returns400() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Blank comment project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "Some task");

        given()
            .header("Authorization", "Bearer " + owner)
            .contentType("application/json")
            .body("{}")
            .when().post("/api/tasks/" + taskId + "/comments")
            .then().statusCode(400);
    }
}
