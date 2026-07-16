package com.teamtask.service;

import java.util.List;

/**
 * What a service hands back for a paged list: one page of entities plus the
 * total row count (the resource layer turns this into a PageResponse DTO).
 * Two queries happen per page - the SELECT for the rows and a COUNT for the
 * total. That's normal; every pagination framework does the same.
 */
public record PagedResult<T>(List<T> content, long totalElements) {
}
