package com.teamtask.dto;

import com.teamtask.model.Comment;

import java.time.LocalDateTime;

/**
 * Public view of a comment: what was said, by whom (name only), and when.
 */
public record CommentResponse(
        Long id,
        String body,
        Long authorId,
        String authorName,
        LocalDateTime createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                comment.getAuthor().getId(),
                comment.getAuthor().getDisplayName(),
                comment.getCreatedAt());
    }
}
