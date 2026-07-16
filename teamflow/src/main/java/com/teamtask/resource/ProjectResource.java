package com.teamtask.resource;

import com.teamtask.dto.ProjectRequest;
import com.teamtask.dto.ProjectResponse;
import com.teamtask.dto.TaskResponse;
import com.teamtask.model.Project;
import com.teamtask.security.CurrentUser;
import com.teamtask.service.ProjectService;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST endpoints for projects.
 *
 *   GET    /api/projects            -> my projects (admin: all)
 *   GET    /api/projects/{id}       -> one project (owner/admin)
 *   GET    /api/projects/{id}/tasks -> its tasks   (owner/admin)
 *   POST   /api/projects            -> create (owner = me)
 *   PUT    /api/projects/{id}       -> update (owner/admin)
 *   DELETE /api/projects/{id}       -> delete + cascade (owner/admin)
 *
 * Role check (@RolesAllowed) answers "are you logged in as a member/admin?".
 * The OWNERSHIP check inside the services answers "is this YOURS?" - that's
 * why a valid MEMBER can still get a 403 here.
 */
@Path("/api/projects")
@RolesAllowed({"ADMIN", "MEMBER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final CurrentUser currentUser;

    public ProjectResource(ProjectService projectService, TaskService taskService, CurrentUser currentUser) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.currentUser = currentUser;
    }

    @GET
    public List<ProjectResponse> list() {
        return projectService.listVisible(currentUser)
                .stream().map(ProjectResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public ProjectResponse getById(@PathParam("id") Long id) {
        return ProjectResponse.from(projectService.findAccessible(id, currentUser));
    }

    /** Nested route: the tasks INSIDE a project. */
    @GET
    @Path("/{id}/tasks")
    public List<TaskResponse> tasks(@PathParam("id") Long id) {
        return taskService.listByProject(id, currentUser)
                .stream().map(TaskResponse::from).toList();
    }

    @POST
    public Response create(@Valid ProjectRequest request) {
        Project created = projectService.create(request, currentUser);
        return Response.status(Response.Status.CREATED)
                .entity(ProjectResponse.from(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    public ProjectResponse update(@PathParam("id") Long id, @Valid ProjectRequest request) {
        return ProjectResponse.from(projectService.update(id, request, currentUser));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        projectService.delete(id, currentUser);
        return Response.noContent().build();
    }
}
