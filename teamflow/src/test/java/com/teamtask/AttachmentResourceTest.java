package com.teamtask;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Phase 4 tests. Quarkus Dev Services starts a throwaway LocalStack (S3)
 * container automatically for tests - no AWS account, no config.
 */
@QuarkusTest
class AttachmentResourceTest {

    private static final byte[] CONTENT = "hello attachment".getBytes();

    @Test
    void upload_thenList_thenGetLink_works() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Attachment project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "Task with file");

        Integer attachmentId = given()
            .header("Authorization", "Bearer " + owner)
            .multiPart("file", "notes.txt", CONTENT, "text/plain")
            .when().post("/api/tasks/" + taskId + "/attachments")
            .then()
            .statusCode(201)
            .body("fileName", equalTo("notes.txt"))
            .body("uploaderName", equalTo("Test Member"))
            .body("sizeBytes", equalTo(CONTENT.length))
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + owner)
            .when().get("/api/tasks/" + taskId + "/attachments")
            .then()
            .statusCode(200)
            .body("totalElements", greaterThanOrEqualTo(1));

        given()
            .header("Authorization", "Bearer " + owner)
            .when().get("/api/attachments/" + attachmentId + "/link")
            .then()
            .statusCode(200)
            .body("url", notNullValue())
            .body("expiresInSeconds", equalTo(900));
    }

    @Test
    void upload_disallowedContentType_returns400() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Bad type project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "No exes please");

        given()
            .header("Authorization", "Bearer " + owner)
            .multiPart("file", "virus.exe", CONTENT, "application/x-msdownload")
            .when().post("/api/tasks/" + taskId + "/attachments")
            .then().statusCode(400);
    }

    @Test
    void upload_onAnotherMembersTask_returns403() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Private attachment project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "Not your task");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.secondMemberToken())
            .multiPart("file", "notes.txt", CONTENT, "text/plain")
            .when().post("/api/tasks/" + taskId + "/attachments")
            .then().statusCode(403);
    }

    @Test
    void link_unknownAttachment_returns404() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/attachments/999999/link")
            .then().statusCode(404);
    }
}
