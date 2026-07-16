package com.teamtask.resource;

import com.teamtask.dto.CommentRequest;
import com.teamtask.dto.CommentResponse;
import com.teamtask.dto.PageParams;
import com.teamtask.dto.PageResponse;
import com.teamtask.model.Comment;
import com.teamtask.security.CurrentUser;
import com.teamtask.service.CommentService;
import com.teamtask.service.PagedResult;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Comments live UNDER a task (nested resource):
 *
 *   GET  /api/tasks/{taskId}/comments -> list (if you may see the task)
 *   POST /api/tasks/{taskId}/comments -> add  (author = me)
 */
@Path("/api/tasks/{taskId}/comments")
@RolesAllowed({"ADMIN", "MEMBER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

    private final CommentService commentService;
    private final CurrentUser currentUser;

    public CommentResource(CommentService commentService, CurrentUser currentUser) {
        this.commentService = commentService;
        this.currentUser = currentUser;
    }

    /** Paginated, always oldest-first (fixed conversation order, no ?sort). */
    @GET
    public PageResponse<CommentResponse> list(@PathParam("taskId") Long taskId,
                                              @QueryParam("page") Integer page,
                                              @QueryParam("size") Integer size) {
        PageParams pageParams = PageParams.from(page, size, null, "createdAt");
        PagedResult<Comment> result = commentService.listForTask(taskId, currentUser, pageParams);
        return PageResponse.of(
                result.content().stream().map(CommentResponse::from).toList(),
                pageParams.page(), pageParams.size(), result.totalElements());
    }

    @POST
    public Response create(@PathParam("taskId") Long taskId, @Valid CommentRequest request) {
        Comment created = commentService.create(taskId, request, currentUser);
        return Response.status(Response.Status.CREATED)
                .entity(CommentResponse.from(created))
                .build();
    }
}
