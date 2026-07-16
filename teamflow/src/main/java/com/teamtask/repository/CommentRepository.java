package com.teamtask.repository;

import com.teamtask.model.Comment;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Data access for comments. "join fetch c.author" loads the author's name
 * in the same query (avoids N+1 when listing a task's comments).
 *
 * Comments are always shown oldest-first (a conversation), so there is no
 * client-chosen sort here - just page/size.
 */
@ApplicationScoped
public class CommentRepository implements PanacheRepository<Comment> {

    public PanacheQuery<Comment> queryByTask(Long taskId) {
        return find("select c from Comment c join fetch c.author where c.task.id = :taskId order by c.createdAt, c.id",
                Map.of("taskId", taskId));
    }

    public long countByTask(Long taskId) {
        return count("task.id = ?1", taskId);
    }
}
