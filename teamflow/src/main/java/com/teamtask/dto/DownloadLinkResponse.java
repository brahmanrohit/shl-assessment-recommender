package com.teamtask.dto;

/**
 * A short-lived presigned URL: anyone holding it can download the file
 * until it expires - that's why the expiry is short and the link is
 * generated fresh per request.
 */
public record DownloadLinkResponse(String url, long expiresInSeconds) {
}
