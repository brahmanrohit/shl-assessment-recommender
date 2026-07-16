package com.teamtask.service;

import com.teamtask.dto.PageParams;
import com.teamtask.dto.TaskRequest;
import com.teamtask.exception.AccessDeniedException;
import com.teamtask.exception.InvalidReferenceException;
import com.teamtask.exception.TaskNotFoundException;
import com.teamtask.model.Project;
import com.teamtask.model.Task;
import com.teamtask.model.TaskPriority;
import com.teamtask.model.TaskStatus;
import com.teamtask.model.User;
import com.teamtask.repository.TaskRepository;
import com.teamtask.repository.UserRepository;
import com.teamtask.security.CurrentUser;
import io.quarkus.panache.common.Page;
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
 * Since Phase 2, every operation is scoped to the CALLER: you only see and
 * touch tasks inside projects you own (admins see everything). The project
 * ownership rule itself lives in ProjectService.findAccessible - reused here.
 */
@ApplicationScoped
public class TaskService {

    private final TaskRepository repository;
    private final ProjectService projectService;
    private final UserRepository users;

    public TaskService(TaskRepository repository, ProjectService projectService, UserRepository users) {
        this.repository = repository;
        this.projectService = projectService;
        this.users = users;
    }

    /** One PAGE of tasks I'm allowed to see, with filters + total count. */
    @Transactional
    public PagedResult<Task> listVisible(CurrentUser user, TaskStatus status,
                                         TaskPriority priority, PageParams pageParams) {
        List<Task> content = repository
                .queryVisible(user.id(), user.isAdmin(), status, priority,
                        pageParams.sortField(), pageParams.ascending())
                .page(Page.of(pageParams.page(), pageParams.size()))
                .list();
        long total = repository.countVisible(user.id(), user.isAdmin(), status, priority);
        return new PagedResult<>(content, total);
    }

    /** One PAGE of a project's tasks - after checking I may see that project. */
    @Transactional
    public PagedResult<Task> listByProject(Long projectId, CurrentUser user, PageParams pageParams) {
        projectService.findAccessible(projectId, user); // 404/403 gate
        List<Task> content = repository
                .queryByProject(projectId, pageParams.sortField(), pageParams.ascending())
                .page(Page.of(pageParams.page(), pageParams.size()))
                .list();
        long total = repository.countByProject(projectId);
        return new PagedResult<>(content, total);
    }

    /**
     * Load one task and enforce access via its project's owner.
     * 404 if the task doesn't exist, 403 if it belongs to someone else.
     */
    @Transactional
    public Task findAccessible(Long id, CurrentUser user) {
        Task task = repository.findByIdWithRefs(id);
        if (task == null) {
            throw new TaskNotFoundException(id);
        }
        if (!user.isAdmin() && !task.getProject().getOwner().getId().equals(user.id())) {
            throw new AccessDeniedException("task", id);
        }
        return task;
    }

    @Transactional
    public Task create(TaskRequest request, CurrentUser user) {
        // You can only add tasks to projects you may access (404/403 otherwise).
        Project project = projectService.findAccessible(request.projectId, user);

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
        task.setProject(project);
        task.setAssignee(resolveAssignee(request.assigneeId));

        repository.persist(task); // INSERT into the database
        return task;              // now has a generated id
    }

    @Transactional
    public Task update(Long id, TaskRequest request, CurrentUser user) {
        Task existing = findAccessible(id, user);

        // Moving the task to another project? That project must be yours too.
        if (!existing.getProject().getId().equals(request.projectId)) {
            Project target = projectService.findAccessible(request.projectId, user);
            existing.setProject(target);
        }

        existing.setTitle(request.title);
        existing.setDescription(request.description);
        if (request.status != null) {
            existing.setStatus(request.status);
        }
        if (request.priority != null) {
            existing.setPriority(request.priority);
        }
        existing.setDueDate(request.dueDate);
        existing.setAssignee(resolveAssignee(request.assigneeId));

        // No explicit save needed: 'existing' is a managed entity, so Hibernate
        // writes the changes to MySQL automatically when the transaction commits.
        return existing;
    }

    /**
     * Phase 2 ownership rule: the project OWNER (or an admin) may delete
     * tasks in their project. findAccessible does the 404/403 work.
     * (Supersedes Phase 1's admin-only rule - the instruction/Phases.md
     * ownership decision applies to ALL modifications, deletes included.)
     */
    @Transactional
    public void delete(Long id, CurrentUser user) {
        Task task = findAccessible(id, user);
        repository.delete(task);
    }

    /** Turn an optional assigneeId into a User, rejecting unknown ids (400). */
    private User resolveAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        User assignee = users.findById(assigneeId);
        if (assignee == null) {
            throw new InvalidReferenceException("Assignee with id " + assigneeId + " does not exist");
        }
        return assignee;
    }
}
