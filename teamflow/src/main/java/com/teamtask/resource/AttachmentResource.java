package com.teamtask.resource;

import com.teamtask.dto.AttachmentResponse;
import com.teamtask.dto.PageParams;
import com.teamtask.dto.PageResponse;
import com.teamtask.model.Attachment;
import com.teamtask.security.CurrentUser;
import com.teamtask.service.AttachmentService;
import com.teamtask.service.PagedResult;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * Attachments nested under a task (same shape as CommentResource):
 *
 *   POST /api/tasks/{taskId}/attachments  multipart upload (field "file")
 *   GET  /api/tasks/{taskId}/attachments  paginated metadata list
 *
 * (The class-level path must be THIS specific: JAX-RS picks the resource
 * class with the most literal path characters and never falls back, so a
 * generic "/api" class would lose /api/tasks/... routes to TaskResource.)
 * Download links live in AttachmentLinkResource.
 */
@Path("/api/tasks/{taskId}/attachments")
@RolesAllowed({"ADMIN", "MEMBER"})
@Produces(MediaType.APPLICATION_JSON)
public class AttachmentResource {

    private final AttachmentService service;
    private final CurrentUser currentUser;

    public AttachmentResource(AttachmentService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@PathParam("taskId") Long taskId, @RestForm("file") FileUpload file) {
        Attachment created = service.upload(taskId, file, currentUser);
        return Response.status(Response.Status.CREATED)
                .entity(AttachmentResponse.from(created))
                .build();
    }

    @GET
    public PageResponse<AttachmentResponse> list(@PathParam("taskId") Long taskId,
                                                 @QueryParam("page") Integer page,
                                                 @QueryParam("size") Integer size) {
        PageParams pageParams = PageParams.from(page, size, null, "createdAt");
        PagedResult<Attachment> result = service.listForTask(taskId, currentUser, pageParams);
        return PageResponse.of(
                result.content().stream().map(AttachmentResponse::from).toList(),
                pageParams.page(), pageParams.size(), result.totalElements());
    }
}
