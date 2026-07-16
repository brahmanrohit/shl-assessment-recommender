package com.teamtask.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Project groups tasks and belongs to one owner (a User).
 *
 * THIS is where relations start (Phase 2):
 *
 *  - @ManyToOne owner: MANY projects can point to ONE user. In the database
 *    this is simply an owner_id column with a FOREIGN KEY to users.id.
 *    FetchType.LAZY = don't load the owner row until someone actually asks
 *    for it (the default EAGER would join it on every query - wasteful).
 *
 *  - @OneToMany tasks: the mirror side. "mappedBy" says the Task entity's
 *    'project' field owns the relationship (the FK lives in the tasks table).
 *    cascade REMOVE + orphanRemoval = deleting a project deletes its tasks
 *    (and each task cascades to its comments).
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Convenience view of the project's tasks. Deletion is handled at the
     * DATABASE level: the tasks.project_id FK is created with
     * ON DELETE CASCADE (see @OnDelete in Task), so deleting a project is
     * ONE delete statement and MySQL removes tasks + comments itself.
     */
    @OneToMany(mappedBy = "project")
    private List<Task> tasks = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ----- Getters and setters -----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
