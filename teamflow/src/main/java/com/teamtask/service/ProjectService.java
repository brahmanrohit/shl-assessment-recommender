package com.teamtask.service;

import com.teamtask.dto.ProjectRequest;
import com.teamtask.exception.AccessDeniedException;
import com.teamtask.exception.ProjectNotFoundException;
import com.teamtask.model.Project;
import com.teamtask.model.User;
import com.teamtask.repository.ProjectRepository;
import com.teamtask.repository.UserRepository;
import com.teamtask.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Business logic for projects, including the Phase 2 OWNERSHIP rule:
 * a project is visible/modifiable only by its owner - or an ADMIN.
 *
 * findAccessible() is the single gatekeeper every other service reuses,
 * so the rule lives in exactly one place.
 */
@ApplicationScoped
public class ProjectService {

    private final ProjectRepository projects;
    private final UserRepository users;

    public ProjectService(ProjectRepository projects, UserRepository users) {
        this.projects = projects;
        this.users = users;
    }

    /** My projects - or every project if I'm an admin. */
    @Transactional
    public List<Project> listVisible(CurrentUser user) {
        return user.isAdmin()
                ? projects.listAllWithOwner()
                : projects.listByOwner(user.id());
    }

    /**
     * The gatekeeper: load a project and enforce ownership.
     * 404 if it doesn't exist, 403 if it exists but isn't yours.
     */
    @Transactional
    public Project findAccessible(Long id, CurrentUser user) {
        Project project = projects.findByIdWithOwner(id);
        if (project == null) {
            throw new ProjectNotFoundException(id);
        }
        if (!user.isAdmin() && !project.getOwner().getId().equals(user.id())) {
            throw new AccessDeniedException("project", id);
        }
        return project;
    }

    @Transactional
    public Project create(ProjectRequest request, CurrentUser user) {
        // getReference-style lookup: the owner row certainly exists (it's the
        // caller), so a managed reference by id is enough.
        User owner = users.findById(user.id());

        Project project = new Project();
        project.setName(request.name);
        project.setDescription(request.description);
        project.setOwner(owner);

        projects.persist(project);
        return project;
    }

    @Transactional
    public Project update(Long id, ProjectRequest request, CurrentUser user) {
        Project project = findAccessible(id, user);
        project.setName(request.name);
        project.setDescription(request.description);
        return project; // managed entity - flushed on commit
    }

    /**
     * Deleting a project cascades to its tasks, and each task cascades to its
     * comments (see the entity mappings). One delete, whole subtree gone.
     */
    @Transactional
    public void delete(Long id, CurrentUser user) {
        Project project = findAccessible(id, user);
        projects.delete(project);
    }
}
