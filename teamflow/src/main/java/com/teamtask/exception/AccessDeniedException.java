package com.teamtask.exception;

/**
 * Authenticated but touching someone else's resource (ownership /
 * object-level authorization) -> 403 Forbidden.
 */
public class AccessDeniedException extends ApiException {

    public AccessDeniedException(String resource, Long id) {
        super(403, "You do not have access to " + resource + " " + id);
    }
}
