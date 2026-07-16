package com.teamtask.resource;

import com.teamtask.dto.TaskRequest;
import com.teamtask.dto.TaskResponse;
import com.teamtask.model.Task;
import com.teamtask.model.TaskStatus;
import com.teamtask.security.CurrentUser;
import com.teamtask.service.TaskService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * The RESOURCE is the REST layer: it maps HTTP requests to Java methods.
 *
 * Base path: /api/tasks
 *   GET    /api/tasks            -> list MY tasks (optionally ?status=TODO)
 *   GET    /api/tasks/{id}       -> get one (if it's in my project)
 *   POST   /api/tasks            -> create (into one of my projects)
 *   PUT    /api/tasks/{id}       -> update
 *   DELETE /api/tasks/{id}       -> delete (ADMIN only)
 *
 * SECURITY: class-level @RolesAllowed = every endpoint needs a valid JWT
 * (401 without one). Since Phase 2, results are also scoped to the caller:
 * the injected CurrentUser is handed to the service, which enforces
 * project OWNERSHIP (403 if it exists but isn't yours). Admins see all.
 */
@Path("/api/tasks")
@RolesAllowed({"ADMIN", "MEMBER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    private final TaskService service;
    private final CurrentUser currentUser;

    public TaskResource(TaskService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    /**
     * List MY tasks (all tasks for admins). ?status=IN_PROGRESS filters.
     */
    @GET
    public List<TaskResponse> list(@QueryParam("status") TaskStatus status) {
        List<Task> tasks = service.listVisible(currentUser, status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public TaskResponse getById(@PathParam("id") Long id) {
        return TaskResponse.from(service.findAccessible(id, currentUser));
    }

    /**
     * Create a task. @Valid triggers the validation rules in TaskRequest.
     * Returns HTTP 201 Created, which is the correct status for a new resource.
     */
    @POST
    public Response create(@Valid TaskRequest request) {
        Task created = service.create(request, currentUser);
        return Response.status(Response.Status.CREATED)
                .entity(TaskResponse.from(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    public TaskResponse update(@PathParam("id") Long id, @Valid TaskRequest request) {
        return TaskResponse.from(service.update(id, request, currentUser));
    }

    /**
     * Delete a task. Returns HTTP 204 No Content (success, nothing to send back).
     * Allowed for the task's project OWNER or an admin - anyone else gets 403.
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id, currentUser);
        return Response.noContent().build();
    }
}
