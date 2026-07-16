package com.teamtask.exception;

/** Unknown attachment id -> 404. */
public class AttachmentNotFoundException extends ApiException {

    public AttachmentNotFoundException(Long id) {
        super(404, "Attachment with id " + id + " was not found");
    }
}
