package com.teamtask.repository;

import com.teamtask.exception.InvalidQueryParameterException;
import com.teamtask.model.Task;
import com.teamtask.model.TaskPriority;
import com.teamtask.model.TaskStatus;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The REPOSITORY is the only layer that talks to the database.
 *
 * FETCH STRATEGY (Phase 2):
 *  - "left join fetch t.assignee": the DTO reads assignee.displayName, so
 *    lists must load it in the SAME query or we get N+1 selects.
 *  - the project is NOT fetched in lists: the DTO only reads project.getId(),
 *    which a lazy proxy answers from the FK column with NO query.
 *
 * PAGINATION (Phase 3):
 *  - queries return PanacheQuery so the service can apply .page(...) -
 *    Hibernate turns that into SQL LIMIT/OFFSET.
 *  - sort fields are validated against SORTABLE (a whitelist): user input
 *    NEVER reaches the ORDER BY clause raw. Every ORDER BY ends with a
 *    ", t.id" tiebreaker so pages are stable when values are equal.
 */
@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {

    private static final String LIST_BASE =
            "select t from Task t left join fetch t.assignee";

    /** API sort name -> HQL path. The ONLY values allowed into ORDER BY. */
    private static final Map<String, String> SORTABLE = Map.of(
            "id", "t.id",
            "title", "t.title",
            "status", "t.status",
            "priority", "t.priority",
            "dueDate", "t.dueDate",
            "createdAt", "t.createdAt");

    /**
     * Page of tasks the user may see (admins: all; members: own projects),
     * optionally filtered by status/priority, sorted by a whitelisted field.
     */
    public PanacheQuery<Task> queryVisible(Long userId, boolean admin, TaskStatus status,
                                           TaskPriority priority, String sortField, boolean ascending) {
        StringBuilder hql = new StringBuilder(LIST_BASE);
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();

        if (!admin) {
            hql.append(" join t.project p"); // join to filter, fetch nothing
            conditions.add("p.owner.id = :userId");
            params.put("userId", userId);
        }
        if (status != null) {
            conditions.add("t.status = :status");
            params.put("status", status);
        }
        if (priority != null) {
            conditions.add("t.priority = :priority");
            params.put("priority", priority);
        }
        if (!conditions.isEmpty()) {
            hql.append(" where ").append(String.join(" and ", conditions));
        }
        hql.append(orderBy(sortField, ascending));

        return find(hql.toString(), params);
    }

    /** Total row count for the same filters (no fetch, no order - just COUNT). */
    public long countVisible(Long userId, boolean admin, TaskStatus status, TaskPriority priority) {
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();

        if (!admin) {
            // path expression project.owner.id makes Hibernate join for us
            conditions.add("project.owner.id = :userId");
            params.put("userId", userId);
        }
        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status);
        }
        if (priority != null) {
            conditions.add("priority = :priority");
            params.put("priority", priority);
        }
        return conditions.isEmpty() ? count() : count(String.join(" and ", conditions), params);
    }

    /** One task with project + assignee pre-loaded, or null. */
    public Task findByIdWithRefs(Long id) {
        return find("select t from Task t join fetch t.project left join fetch t.assignee where t.id = :id",
                Map.of("id", id)).firstResult();
    }

    /** Page of one project's tasks (caller has already checked access). */
    public PanacheQuery<Task> queryByProject(Long projectId, String sortField, boolean ascending) {
        return find(LIST_BASE + " where t.project.id = :projectId" + orderBy(sortField, ascending),
                Map.of("projectId", projectId));
    }

    public long countByProject(Long projectId) {
        return count("project.id = ?1", projectId);
    }

    /** Whitelist gate: unknown sort fields are rejected, never concatenated. */
    private String orderBy(String sortField, boolean ascending) {
        String path = SORTABLE.get(sortField);
        if (path == null) {
            throw new InvalidQueryParameterException(
                    "Cannot sort tasks by '" + sortField + "'. Allowed: " + String.join(", ", SORTABLE.keySet()));
        }
        return " order by " + path + (ascending ? " asc" : " desc") + ", t.id";
    }
}
