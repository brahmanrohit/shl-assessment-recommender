package com.teamtask.repository;

import com.teamtask.model.Comment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Data access for comments. "join fetch c.author" loads the author's name
 * in the same query (avoids N+1 when listing a task's comments).
 */
@ApplicationScoped
public class CommentRepository implements PanacheRepository<Comment> {

    public List<Comment> listByTask(Long taskId) {
        return find("select c from Comment c join fetch c.author where c.task.id = ?1 order by c.createdAt", taskId)
                .list();
    }
}
