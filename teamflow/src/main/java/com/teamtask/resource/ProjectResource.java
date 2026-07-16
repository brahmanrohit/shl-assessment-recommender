package com.teamtask.resource;

import com.teamtask.dto.PageParams;
import com.teamtask.dto.PageResponse;
import com.teamtask.dto.ProjectRequest;
import com.teamtask.dto.ProjectResponse;
import com.teamtask.dto.TaskResponse;
import com.teamtask.model.Project;
import com.teamtask.model.Task;
import com.teamtask.security.CurrentUser;
import com.teamtask.service.PagedResult;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    /** Paginated: ?page=&size=&sort=name,asc (whitelist: id, name, createdAt). */
    @GET
    public PageResponse<ProjectResponse> list(@QueryParam("page") Integer page,
                                              @QueryParam("size") Integer size,
                                              @QueryParam("sort") String sort) {
        PageParams pageParams = PageParams.from(page, size, sort, "id");
        PagedResult<Project> result = projectService.listVisible(currentUser, pageParams);
        return PageResponse.of(
                result.content().stream().map(ProjectResponse::from).toList(),
                pageParams.page(), pageParams.size(), result.totalElements());
    }

    @GET
    @Path("/{id}")
    public ProjectResponse getById(@PathParam("id") Long id) {
        return ProjectResponse.from(projectService.findAccessible(id, currentUser));
    }

    /** Nested route: the tasks INSIDE a project, paginated like /api/tasks. */
    @GET
    @Path("/{id}/tasks")
    public PageResponse<TaskResponse> tasks(@PathParam("id") Long id,
                                            @QueryParam("page") Integer page,
                                            @QueryParam("size") Integer size,
                                            @QueryParam("sort") String sort) {
        PageParams pageParams = PageParams.from(page, size, sort, "id");
        PagedResult<Task> result = taskService.listByProject(id, currentUser, pageParams);
        return PageResponse.of(
                result.content().stream().map(TaskResponse::from).toList(),
                pageParams.page(), pageParams.size(), result.totalElements());
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
