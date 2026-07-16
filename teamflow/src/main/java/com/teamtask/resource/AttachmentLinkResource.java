package com.teamtask.resource;

import com.teamtask.dto.DownloadLinkResponse;
import com.teamtask.security.CurrentUser;
import com.teamtask.service.AttachmentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * GET /api/attachments/{id}/link -> short-lived presigned S3 download URL.
 * Access is checked through the attachment's task (owner/admin).
 */
@Path("/api/attachments")
@RolesAllowed({"ADMIN", "MEMBER"})
@Produces(MediaType.APPLICATION_JSON)
public class AttachmentLinkResource {

    private final AttachmentService service;
    private final CurrentUser currentUser;

    public AttachmentLinkResource(AttachmentService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GET
    @Path("/{id}/link")
    public DownloadLinkResponse link(@PathParam("id") Long id) {
        return new DownloadLinkResponse(
                service.presignedUrl(id, currentUser),
                service.presignExpirySeconds());
    }
}
