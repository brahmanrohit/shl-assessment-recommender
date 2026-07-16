package com.teamtask.bootstrap;

import com.teamtask.model.Comment;
import com.teamtask.model.Project;
import com.teamtask.model.Role;
import com.teamtask.model.Task;
import com.teamtask.model.TaskPriority;
import com.teamtask.model.TaskStatus;
import com.teamtask.model.User;
import com.teamtask.repository.CommentRepository;
import com.teamtask.repository.ProjectRepository;
import com.teamtask.repository.TaskRepository;
import com.teamtask.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;

/**
 * Seeds demo data at startup so the API is interesting to explore on a
 * fresh database (a demo member, a project, tasks, comments).
 *
 * WHY CODE AND NOT import.sql? Since Phase 2, tasks need a project_id and
 * projects need an owner_id - foreign keys pointing at USERS, and users only
 * exist at runtime (passwords must be BCrypt-hashed by Java code). A static
 * SQL file can't do that, so seeding moved here. In production you would
 * simply disable this with DEMO_DATA=false.
 */
@ApplicationScoped
public class DemoDataBootstrap {

    static final String DEMO_EMAIL = "demo@teamflow.local";
    static final String DEMO_PASSWORD = "demo1234";

    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final CommentRepository comments;

    @ConfigProperty(name = "app.demo-data.enabled", defaultValue = "true")
    boolean enabled;

    public DemoDataBootstrap(UserRepository users, ProjectRepository projects,
                             TaskRepository tasks, CommentRepository comments) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.comments = comments;
    }

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (!enabled || users.findByEmail(DEMO_EMAIL) != null) {
            return; // disabled, or already seeded
        }

        User demo = new User();
        demo.setEmail(DEMO_EMAIL);
        demo.setPasswordHash(BcryptUtil.bcryptHash(DEMO_PASSWORD));
        demo.setDisplayName("Demo Member");
        demo.setRole(Role.MEMBER);
        users.persist(demo);

        Project project = new Project();
        project.setName("TeamFlow Development");
        project.setDescription("Building this very API, phase by phase");
        project.setOwner(demo);
        projects.persist(project);

        Task t1 = task(project, "Set up development environment",
                "Install JDK 21, Maven and Docker Desktop",
                TaskStatus.DONE, TaskPriority.HIGH, LocalDate.of(2026, 7, 18), demo);
        Task t2 = task(project, "Learn Quarkus basics",
                "Understand REST, CDI and Panache",
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.of(2026, 7, 25), demo);
        task(project, "Design MySQL schema",
                "Users, projects, tasks, comments - with proper foreign keys",
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, LocalDate.of(2026, 7, 28), null);
        task(project, "Write the cold email",
                "Reach out to Team Computers with the project link",
                TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 8, 5), null);
        task(project, "Deploy to AWS",
                "Push the container and run it on ECS with RDS",
                TaskStatus.TODO, TaskPriority.LOW, LocalDate.of(2026, 8, 15), null);

        comment(t1, demo, "Done! Docker Desktop must be running before compose.");
        comment(t2, demo, "The SQL log is gold - you can watch every query Hibernate runs.");

        Log.infof("Demo data seeded: user %s with 1 project, 5 tasks, 2 comments", DEMO_EMAIL);
    }

    private Task task(Project project, String title, String description,
                      TaskStatus status, TaskPriority priority, LocalDate due, User assignee) {
        Task t = new Task();
        t.setProject(project);
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        t.setPriority(priority);
        t.setDueDate(due);
        t.setAssignee(assignee);
        tasks.persist(t);
        return t;
    }

    private void comment(Task task, User author, String body) {
        Comment c = new Comment();
        c.setTask(task);
        c.setAuthor(author);
        c.setBody(body);
        comments.persist(c);
    }
}
