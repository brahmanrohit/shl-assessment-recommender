-- ============================================================
-- V1: the complete TeamFlow schema (Phase 5).
-- Captured from the Hibernate-generated schema, then tidied.
-- From here on the schema is OWNED by Flyway migrations; Hibernate
-- only VALIDATES that the entities still match it at startup.
-- Never edit an applied migration - add V2__, V3__, ... instead.
-- (MySQL auto-indexes FK columns; only extra indexes are declared.)
-- ============================================================

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          ENUM ('ADMIN','MEMBER') NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE projects (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(120)  NOT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    owner_id    BIGINT        NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE tasks (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    title       VARCHAR(150)  NOT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    status      ENUM ('DONE','IN_PROGRESS','TODO') NOT NULL,
    priority    ENUM ('HIGH','LOW','MEDIUM')       NOT NULL,
    due_date    DATE          DEFAULT NULL,
    project_id  BIGINT        NOT NULL,
    assignee_id BIGINT        DEFAULT NULL,
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tasks_status (status),
    CONSTRAINT fk_tasks_project  FOREIGN KEY (project_id)  REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users (id)
);

CREATE TABLE comments (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    task_id    BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    body       VARCHAR(1000) NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_task   FOREIGN KEY (task_id)   REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE TABLE attachments (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    task_id      BIGINT       NOT NULL,
    uploader_id  BIGINT       NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    s3_key       VARCHAR(512) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attachments_task     FOREIGN KEY (task_id)     REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_uploader FOREIGN KEY (uploader_id) REFERENCES users (id)
);
