package com.teamtask;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Phase 3 tests: the pagination envelope, size capping, out-of-range pages,
 * sort whitelisting (the injection defense), and filters.
 */
@QuarkusTest
class PaginationTest {

    @Test
    void taskList_returnsEnvelope_withRequestedSlice() {
        String token = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(token, "Pagination project");
        AuthTestSupport.createTask(token, projectId, "Page task A");
        AuthTestSupport.createTask(token, projectId, "Page task B");
        AuthTestSupport.createTask(token, projectId, "Page task C");

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/tasks?page=0&size=2")
            .then()
            .statusCode(200)
            .body("content.size()", equalTo(2))          // only the slice
            .body("page", equalTo(0))
            .body("size", equalTo(2))
            .body("totalElements", greaterThanOrEqualTo(3))
            .body("totalPages", greaterThanOrEqualTo(2));
    }

    @Test
    void taskList_outOfRangePage_returnsEmptyContent() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/tasks?page=9999&size=50")
            .then()
            .statusCode(200)
            .body("content.size()", equalTo(0)); // valid request, empty slice
    }

    @Test
    void taskList_oversizedPageSize_isCappedAt100() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/tasks?size=100000")
            .then()
            .statusCode(200)
            .body("size", equalTo(100)); // the cap, never the raw value
    }

    @Test
    void taskList_sortByWhitelistedField_works() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/tasks?sort=priority,desc")
            .then()
            .statusCode(200);
    }

    @Test
    void taskList_sortByUnknownField_returns400() {
        // The whitelist is the injection defense: unknown fields never reach ORDER BY.
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/tasks?sort=passwordHash")
            .then()
            .statusCode(400);
    }

    @Test
    void taskList_priorityFilter_onlyReturnsMatches() {
        String token = AuthTestSupport.memberToken();
        Integer projectId = AuthTestSupport.createProject(token, "Priority filter project");
        given()
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body("""
                { "title": "Urgent thing", "projectId": %d, "priority": "HIGH" }
                """.formatted(projectId))
            .when().post("/api/tasks")
            .then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/tasks?priority=HIGH&size=100")
            .then()
            .statusCode(200)
            .body("totalElements", greaterThanOrEqualTo(1))
            .body("content.priority", everyItem(equalTo("HIGH")));
    }

    @Test
    void projectList_sortByUnknownField_returns400() {
        given()
            .header("Authorization", "Bearer " + AuthTestSupport.memberToken())
            .when().get("/api/projects?sort=ownerPassword")
            .then()
            .statusCode(400);
    }
}
