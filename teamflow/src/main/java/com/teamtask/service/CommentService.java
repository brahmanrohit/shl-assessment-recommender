package com.teamtask.service;

import com.teamtask.dto.CommentRequest;
import com.teamtask.model.Comment;
import com.teamtask.model.Task;
import com.teamtask.model.User;
import com.teamtask.repository.CommentRepository;
import com.teamtask.repository.UserRepository;
import com.teamtask.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Business logic for comments. Access control is delegated: you may comment
 * on (or read comments of) a task exactly when you may SEE the task, and
 * TaskService.findAccessible already decides that. No duplicated rules.
 */
@ApplicationScoped
public class CommentService {

    private final CommentRepository comments;
    private final TaskService tasks;
    private final UserRepository users;

    public CommentService(CommentRepository comments, TaskService tasks, UserRepository users) {
        this.comments = comments;
        this.tasks = tasks;
        this.users = users;
    }

    @Transactional
    public List<Comment> listForTask(Long taskId, CurrentUser user) {
        tasks.findAccessible(taskId, user); // 404/403 if not allowed
        return comments.listByTask(taskId);
    }

    @Transactional
    public Comment create(Long taskId, CommentRequest request, CurrentUser user) {
        Task task = tasks.findAccessible(taskId, user);
        User author = users.findById(user.id());

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setBody(request.body);

        comments.persist(comment);
        return comment;
    }
}
