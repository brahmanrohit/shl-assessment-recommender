package com.teamtask.repository;

import com.teamtask.model.Task;
import com.teamtask.model.TaskStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The REPOSITORY is the only layer that talks to the database.
 *
 * By implementing Panache's PanacheRepository<Task>, we get a huge set of
 * ready-made methods for free: persist(), findById(), listAll(),
 * deleteById(), count(), and more. We only write the custom queries.
 *
 * FETCH STRATEGY (Phase 2, tuned after review):
 *  - "left join fetch t.assignee": the DTO reads assignee.displayName, so
 *    lists must load it in the SAME query or we get N+1 selects.
 *  - the project is NOT fetched in lists: the DTO only reads project.getId(),
 *    which a lazy proxy answers from the FK column with NO query. Filtering
 *    by owner uses a plain "join" (SQL join, nothing transferred).
 *  - findByIdWithRefs DOES fetch the project: the ownership check reads
 *    project.getOwner() - one row, negligible cost.
 */
@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {

    private static final String LIST_BASE =
            "select t from Task t left join fetch t.assignee";

    /**
     * Tasks the given user may see: all of them for admins, otherwise only
     * tasks inside projects they own. Optional status filter.
     * The WHERE clause is assembled piece by piece - watch the SQL log.
     */
    public List<Task> listVisible(Long userId, boolean admin, TaskStatus status) {
        StringBuilder hql = new StringBuilder(LIST_BASE);
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new java.util.ArrayList<>();

        if (!admin) {
            hql.append(" join t.project p"); // join to filter, fetch nothing
            conditions.add("p.owner.id = :userId");
            params.put("userId", userId);
        }
        if (status != null) {
            conditions.add("t.status = :status");
            params.put("status", status);
        }
        if (!conditions.isEmpty()) {
            hql.append(" where ").append(String.join(" and ", conditions));
        }
        hql.append(" order by t.id");

        return find(hql.toString(), params).list();
    }

    /** One task with project + assignee pre-loaded, or null. */
    public Task findByIdWithRefs(Long id) {
        return find("select t from Task t join fetch t.project left join fetch t.assignee where t.id = :id",
                Map.of("id", id)).firstResult();
    }

    /** All tasks of one project (caller has already checked access). */
    public List<Task> listByProject(Long projectId) {
        return find(LIST_BASE + " where t.project.id = :projectId order by t.id",
                Map.of("projectId", projectId)).list();
    }
}
