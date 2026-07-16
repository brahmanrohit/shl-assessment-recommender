package com.teamtask.resource;

import com.teamtask.dto.CommentRequest;
import com.teamtask.dto.CommentResponse;
import com.teamtask.model.Comment;
import com.teamtask.security.CurrentUser;
import com.teamtask.service.CommentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

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

    @GET
    public List<CommentResponse> list(@PathParam("taskId") Long taskId) {
        return commentService.listForTask(taskId, currentUser)
                .stream().map(CommentResponse::from).toList();
    }

    @POST
    public Response create(@PathParam("taskId") Long taskId, @Valid CommentRequest request) {
        Comment created = commentService.create(taskId, request, currentUser);
        return Response.status(Response.Status.CREATED)
                .entity(CommentResponse.from(created))
                .build();
    }
}
