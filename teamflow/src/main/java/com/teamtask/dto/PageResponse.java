package com.teamtask.dto;

import java.util.List;

/**
 * The standard envelope every LIST endpoint returns (Phase 3).
 *
 * Why not just return a bare JSON array? Because real datasets grow:
 * "return everything" queries slow down, eat memory, and eventually take
 * the service down. Paginated responses keep every request bounded, and
 * the metadata lets clients build "page 3 of 12" UIs.
 *
 * <T> makes this generic: PageResponse<TaskResponse>,
 * PageResponse<ProjectResponse>, etc. - one envelope for all lists.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
