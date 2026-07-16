package com.teamtask.dto;

import com.teamtask.model.Attachment;

import java.time.LocalDateTime;

/**
 * Public view of an attachment. Note what is MISSING: the s3Key.
 * Clients get files only through short-lived presigned URLs, never
 * by learning our internal storage layout.
 */
public record AttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        Long uploaderId,
        String uploaderName,
        LocalDateTime createdAt) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploader().getId(),
                attachment.getUploader().getDisplayName(),
                attachment.getCreatedAt());
    }
}
