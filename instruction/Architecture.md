# Architecture.md — How the app is built

> **What this file is for:** The technical map. App flow, layers, folder
> structure, database schema, and the tech stack. Read after PRD.md.

---

## 1. Tech stack

| Concern | Choice | Why |
|---------|--------|-----|
| Language | Java 21 | Modern LTS |
| Framework | Quarkus 3.15 (LTS BOM) | Container-first, fast startup, low memory |
| API style | REST + JSON (JAX-RS via quarkus-rest) | Industry standard |
| ORM | Hibernate ORM + Panache (repository pattern) | Less boilerplate, still real JPA |
| Database | MySQL 8.4 | Job requirement |
| Validation | Hibernate Validator (Jakarta Bean Validation) | Declarative rules on DTOs |
| Auth (Phase 1) | SmallRye JWT (quarkus-smallrye-jwt) + BCrypt | Stateless, standard |
| Migrations (Phase 5) | Flyway | Versioned schema, production-style |
| Files (Phase 4) | AWS S3 (LocalStack for local dev) | Job requirement |
| Docs | OpenAPI + Swagger UI | Live, clickable API docs |
| Packaging | Docker multi-stage build + Docker Compose | Job requirement |
| CI/CD (Phase 6) | GitHub Actions | Free, standard |
| Tests | JUnit 5 + REST Assured (@QuarkusTest) | Real integration tests |

## 2. Request flow (the one-breath explanation)

```
HTTP request (JSON)
      |
      v
 Resource layer   -- speaks HTTP: routes, status codes, @Valid triggers
      |
      v
 Service layer    -- business rules, @Transactional boundaries
      |
      v
 Repository layer -- ONLY layer that touches the database (Panache)
      |
      v
 Entities <-> MySQL tables (Hibernate ORM)
```

DTOs (`dto/`) define the JSON shapes in/out. Entities are NEVER returned
directly to clients. Exceptions are converted to clean JSON errors by
mappers in `exception/`.

## 3. Folder structure

```
quarkus-task-api/            (GitHub repo name: teamflow)
├── instruction/             <- project brain: PRD, architecture, rules, phases, design, memory
├── k8s/                     <- Kubernetes manifests
├── .github/workflows/       <- CI pipeline (Phase 6)
├── src/main/resources/
│   ├── application.properties
│   ├── import.sql           (dev seed data; replaced by Flyway in Phase 5)
│   └── db/migration/        (Flyway scripts, Phase 5: V1__init.sql, ...)
└── src/main/java/com/teamtask/
    ├── model/               <- entities + enums (DB mapping)
    ├── repository/          <- Panache repositories
    ├── service/             <- business logic + transactions
    ├── dto/                 <- request/response records & classes
    ├── exception/           <- custom exceptions + HTTP mappers
    ├── resource/            <- REST endpoints
    └── security/            <- (Phase 1) JWT generation, password hashing
```

## 4. Database schema

### Current (Phase 0)
```
tasks (id PK, title, description, status, priority, due_date, created_at)
```

### Target (after Phase 2)
```
users     (id PK, email UNIQUE, password_hash, display_name, role, created_at)
projects  (id PK, name, description, owner_id FK -> users.id, created_at)
tasks     (id PK, project_id FK -> projects.id, assignee_id FK -> users.id NULL,
           title, description, status, priority, due_date, created_at)
comments  (id PK, task_id FK -> tasks.id, author_id FK -> users.id,
           body, created_at)
attachments (id PK, task_id FK -> tasks.id, uploader_id FK -> users.id,
           file_name, s3_key, content_type, size_bytes, created_at)   [Phase 4]
```
Relations: User 1—N Project (owner), Project 1—N Task, Task 1—N Comment,
Task 1—N Attachment. Index every FK column and `tasks.status`.

## 5. Configuration strategy

All environment-specific values (DB URL, credentials, JWT keys, S3 bucket)
come from **environment variables** with sensible local defaults in
`application.properties` (pattern: `${DB_URL:jdbc:mysql://localhost:3306/taskdb}`).
Docker Compose sets them locally; ECS/K8s sets them in the cloud. Secrets are
NEVER committed.

## 6. Deployment shape

- **Local:** `docker compose up --build` → app container + MySQL container.
- **CI:** GitHub Actions builds + tests + builds the Docker image on each push.
- **Cloud (target):** image in ECR → runs on ECS/EKS → RDS MySQL → S3 for files.
