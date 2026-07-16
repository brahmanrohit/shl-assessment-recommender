package com.teamtask.repository;

import com.teamtask.model.Task;
import com.teamtask.model.TaskStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * The REPOSITORY is the only layer that talks to the database.
 *
 * By implementing Panache's PanacheRepository<Task>, we get a huge set of
 * ready-made methods for free: persist(), findById(), listAll(),
 * deleteById(), count(), and more. We only write the custom queries.
 *
 * @ApplicationScoped means Quarkus creates ONE shared instance and injects
 * it wherever it is needed (this is dependency injection / CDI).
 */
@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {

    /**
     * Find every task with a given status.
     * Panache turns this into: SELECT * FROM tasks WHERE status = ?
     */
    public List<Task> findByStatus(TaskStatus status) {
        return list("status", status);
    }
}
