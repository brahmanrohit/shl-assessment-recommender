package com.teamtask;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Tests for projects: CRUD, the ownership boundary between two members,
 * and the Phase 2 cascade (delete project -> its tasks disappear).
 */
@QuarkusTest
class ProjectResourceTest {

    @Test
    void listProjects_withoutToken_returns401() {
        given()
            .when().get("/api/projects")
            .then().statusCode(401);
    }

    @Test
    void createProject_returns201_withOwner() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .contentType("application/json")
            .body("""
                { "name": "My portfolio", "description": "job-hunt work" }
                """)
            .when().post("/api/projects")
            .then()
            .statusCode(201)
            .body("name", equalTo("My portfolio"))
            .body("ownerName", equalTo("Test Member")); // owner = the caller
    }

    @Test
    void createProject_withoutName_returns400() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .contentType("application/json")
            .body("{}")
            .when().post("/api/projects")
            .then().statusCode(400);
    }

    @Test
    void listProjects_containsOwnProject() {
        String owner = AuthTestSupport.memberToken();
        AuthTestSupport.createProject(owner, "Listed project");

        given()
            .header("Authorization", "Bearer " + owner)
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .body("name", org.hamcrest.CoreMatchers.hasItem("Listed project"));
    }

    @Test
    void getProject_asOwner_returns200() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Own readable project");

        given()
            .header("Authorization", "Bearer " + owner)
            .when().get("/api/projects/" + projectId)
            .then()
            .statusCode(200)
            .body("name", equalTo("Own readable project"));
    }

    @Test
    void getUnknownProject_returns404() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/projects/999999")
            .then().statusCode(404);
    }

    @Test
    void getProject_ofAnotherMember_returns403() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Not yours");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.secondMemberToken())
            .when().get("/api/projects/" + projectId)
            .then().statusCode(403);
    }

    @Test
    void updateProject_ofAnotherMember_returns403() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "No renames by strangers");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.secondMemberToken())
            .contentType("application/json")
            .body("""
                { "name": "Hijacked" }
                """)
            .when().put("/api/projects/" + projectId)
            .then().statusCode(403);
    }

    @Test
    void deleteProject_ofAnotherMember_returns403() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "No deletes by strangers");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.secondMemberToken())
            .when().delete("/api/projects/" + projectId)
            .then().statusCode(403);
    }

    @Test
    void listProjectTasks_ofAnotherMember_returns403() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Private task list");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.secondMemberToken())
            .when().get("/api/projects/" + projectId + "/tasks")
            .then().statusCode(403);
    }

    @Test
    void getProject_asAdmin_returns200() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Admin sees all");

        given()
            .header("Authorization", "Bearer " + AuthTestSupport.adminToken())
            .when().get("/api/projects/" + projectId)
            .then().statusCode(200);
    }

    @Test
    void updateProject_asOwner_returns200() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Before rename");

        given()
            .header("Authorization", "Bearer " + owner)
            .contentType("application/json")
            .body("""
                { "name": "After rename", "description": "updated" }
                """)
            .when().put("/api/projects/" + projectId)
            .then()
            .statusCode(200)
            .body("name", equalTo("After rename"));
    }

    @Test
    void deleteProject_cascadesToTasks() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Doomed project");
        Integer taskId = AuthTestSupport.createTask(owner, projectId, "Doomed task");

        // Owner deletes the project...
        given()
            .header("Authorization", "Bearer " + owner)
            .when().delete("/api/projects/" + projectId)
            .then().statusCode(204);

        // ...and the task inside it is gone too (FK cascade in action).
        given()
            .header("Authorization", "Bearer " + owner)
            .when().get("/api/tasks/" + taskId)
            .then().statusCode(404);
    }

    @Test
    void listProjectTasks_showsOnlyThatProjectsTasks() {
        String owner = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(owner, "Scoped list project");
        AuthTestSupport.createTask(owner, projectId, "Only task in here");

        given()
            .header("Authorization", "Bearer " + owner)
            .when().get("/api/projects/" + projectId + "/tasks")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].title", equalTo("Only task in here"));
    }
}
