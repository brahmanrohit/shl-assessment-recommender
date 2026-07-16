package com.teamtask.dto;

/**
 * Parses and SANITIZES the paging query parameters. All clamping rules live
 * in one place so every endpoint behaves identically:
 *
 *  - page: 0-based; negative or missing -> 0
 *  - size: default 20, clamped to 1..100 (an unbounded size would defeat
 *    the whole point of pagination)
 *  - sort: "field" or "field,desc"; the FIELD is only validated later
 *    against a per-entity whitelist in the repository (never trusted raw)
 */
public record PageParams(int page, int size, String sortField, boolean ascending) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static PageParams from(Integer page, Integer size, String sort, String defaultSortField) {
        int safePage = (page == null || page < 0) ? 0 : page;

        int safeSize = (size == null) ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);

        String field = defaultSortField;
        boolean ascending = true;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            field = parts[0].trim();
            if (parts.length > 1) {
                ascending = !"desc".equalsIgnoreCase(parts[1].trim());
            }
        }
        return new PageParams(safePage, safeSize, field, ascending);
    }
}
