package com.teamtask.service;

import com.teamtask.dto.TaskRequest;
import com.teamtask.exception.TaskNotFoundException;
import com.teamtask.model.Task;
import com.teamtask.model.TaskStatus;
import com.teamtask.repository.TaskRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * The SERVICE layer holds the business logic. It sits between the REST layer
 * (which deals with HTTP) and the repository (which deals with the database).
 *
 * Why a separate layer? It keeps each part focused:
 *   Resource  -> "talk HTTP"
 *   Service   -> "apply the rules"
 *   Repository-> "talk to the database"
 * This separation is exactly what interviewers mean by "clean architecture".
 *
 * @Transactional means the method runs inside a database transaction: all its
 * changes either fully succeed together, or are fully rolled back on error.
 */
@ApplicationScoped
public class TaskService {

    private final TaskRepository repository;

    // Constructor injection: Quarkus passes in the repository automatically.
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Task> listAll() {
        return repository.listAll();
    }

    @Transactional
    public List<Task> listByStatus(TaskStatus status) {
        return repository.findByStatus(status);
    }

    @Transactional
    public Task findById(Long id) {
        Task task = repository.findById(id);
        if (task == null) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    @Transactional
    public Task create(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title);
        task.setDescription(request.description);
        if (request.status != null) {
            task.setStatus(request.status);
        }
        if (request.priority != null) {
            task.setPriority(request.priority);
        }
        task.setDueDate(request.dueDate);

        repository.persist(task); // INSERT into the database
        return task;              // now has a generated id
    }

    @Transactional
    public Task update(Long id, TaskRequest request) {
        Task existing = findById(id); // reuses the 404 logic above

        existing.setTitle(request.title);
        existing.setDescription(request.description);
        if (request.status != null) {
            existing.setStatus(request.status);
        }
        if (request.priority != null) {
            existing.setPriority(request.priority);
        }
        existing.setDueDate(request.dueDate);

        // No explicit save needed: 'existing' is a managed entity, so Hibernate
        // writes the changes to MySQL automatically when the transaction commits.
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            throw new TaskNotFoundException(id);
        }
    }
}
