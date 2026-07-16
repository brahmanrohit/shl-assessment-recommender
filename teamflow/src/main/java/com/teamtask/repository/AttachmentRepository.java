package com.teamtask.repository;

import com.teamtask.model.Attachment;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Data access for attachments. Same patterns as CommentRepository:
 * join fetch the uploader (DTO shows the name), fixed oldest-first order.
 */
@ApplicationScoped
public class AttachmentRepository implements PanacheRepository<Attachment> {

    public PanacheQuery<Attachment> queryByTask(Long taskId) {
        return find("select a from Attachment a join fetch a.uploader where a.task.id = :taskId order by a.createdAt, a.id",
                Map.of("taskId", taskId));
    }

    public long countByTask(Long taskId) {
        return count("task.id = ?1", taskId);
    }
}
