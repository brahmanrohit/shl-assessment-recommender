package com.teamtask.resource;

import com.teamtask.dto.TaskRequest;
import com.teamtask.dto.TaskResponse;
import com.teamtask.model.Task;
import com.teamtask.model.TaskStatus;
import com.teamtask.service.TaskService;
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
 *   GET    /api/tasks            -> list all (optionally ?status=TODO)
 *   GET    /api/tasks/{id}       -> get one
 *   POST   /api/tasks            -> create
 *   PUT    /api/tasks/{id}       -> update
 *   DELETE /api/tasks/{id}       -> delete
 *
 * @Produces/@Consumes JSON means requests and responses use JSON.
 * The resource stays THIN: it only translates HTTP <-> service calls.
 */
@Path("/api/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    private final TaskService service;

    public TaskResource(TaskService service) {
        this.service = service;
    }

    /**
     * List tasks. If ?status=IN_PROGRESS is supplied, filter by that status.
     */
    @GET
    public List<TaskResponse> list(@QueryParam("status") TaskStatus status) {
        List<Task> tasks = (status == null)
                ? service.listAll()
                : service.listByStatus(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public TaskResponse getById(@PathParam("id") Long id) {
        return TaskResponse.from(service.findById(id));
    }

    /**
     * Create a task. @Valid triggers the validation rules in TaskRequest.
     * Returns HTTP 201 Created, which is the correct status for a new resource.
     */
    @POST
    public Response create(@Valid TaskRequest request) {
        Task created = service.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(TaskResponse.from(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    public TaskResponse update(@PathParam("id") Long id, @Valid TaskRequest request) {
        return TaskResponse.from(service.update(id, request));
    }

    /**
     * Delete a task. Returns HTTP 204 No Content (success, nothing to send back).
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
