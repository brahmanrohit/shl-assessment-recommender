# Task Manager API

A backend REST API for managing tasks, built with **Java 21 + Quarkus**, backed by **MySQL**, and fully **containerized with Docker**. It demonstrates a clean, layered backend architecture and is ready to deploy to the cloud (AWS ECS / Kubernetes).

> Built as a portfolio project to demonstrate production-style backend development: REST API design, relational data modelling, validation, error handling, and containerized deployment.

---

## Tech stack (and why it matters)

| Area | Technology |
|------|------------|
| Language | Java 21 |
| Framework | Quarkus (fast startup, low memory, container-first) |
| API | RESTful endpoints (JAX-RS) with JSON |
| Persistence | Hibernate ORM with Panache |
| Database | MySQL 8 |
| Validation | Hibernate Validator (Jakarta Bean Validation) |
| Security | JWT (SmallRye JWT, RSA-signed) + BCrypt password hashing + role-based access |
| Docs | OpenAPI + Swagger UI |
| Build | Maven |
| Packaging | Docker (multi-stage build) + Docker Compose |
| Orchestration | Kubernetes manifest included |
| Testing | JUnit 5 + REST Assured |

---

## Architecture

The code is split into layers, each with one job. Requests flow top to bottom:

```
HTTP request
    |
    v
TaskResource      (REST layer  - speaks HTTP, maps URLs to methods)
    |
    v
TaskService       (business logic - applies the rules, transactions)
    |
    v
TaskRepository    (data access  - the only layer that touches the DB)
    |
    v
Task (entity)  <->  MySQL "tasks" table
```

DTOs (`TaskRequest`, `TaskResponse`) define the JSON shape going in and out, so the database structure is never exposed directly to clients.

---

## Run it (only Docker required)

You do **not** need Java or Maven installed — the multi-stage Dockerfile builds everything inside a container.

```bash
# from the project folder
docker compose up --build
```

Then open the interactive API docs in your browser:

**http://localhost:8080/swagger-ui**

To stop:

```bash
# press Ctrl+C, then:
docker compose down
```

---

## API endpoints

### Auth (public)

| Method | Path | Description | Success code |
|--------|------|-------------|--------------|
| POST | `/api/auth/signup` | Register (always role MEMBER) | 201 |
| POST | `/api/auth/login` | Get a JWT for your credentials | 200 |

### Projects (require `Authorization: Bearer <token>`)

| Method | Path | Description | Success code |
|--------|------|-------------|--------------|
| GET | `/api/projects` | My projects (admin: all) | 200 |
| GET | `/api/projects/{id}` | One project (owner/admin) | 200 |
| GET | `/api/projects/{id}/tasks` | Tasks inside a project | 200 |
| POST | `/api/projects` | Create (owner = me) | 201 |
| PUT | `/api/projects/{id}` | Update (owner/admin) | 200 |
| DELETE | `/api/projects/{id}` | Delete + cascade tasks/comments | 204 |

### Tasks (require `Authorization: Bearer <token>`)

| Method | Path | Description | Success code |
|--------|------|-------------|--------------|
| GET | `/api/tasks` | My tasks (optional `?status=TODO`) | 200 |
| GET | `/api/tasks/{id}` | Get one task by id | 200 |
| POST | `/api/tasks` | Create a task (`projectId` required) | 201 |
| PUT | `/api/tasks/{id}` | Update a task | 200 |
| DELETE | `/api/tasks/{id}` | Delete a task (project owner or admin) | 204 |

### Comments (require `Authorization: Bearer <token>`)

| Method | Path | Description | Success code |
|--------|------|-------------|--------------|
| GET | `/api/tasks/{id}/comments` | Comments on a task | 200 |
| POST | `/api/tasks/{id}/comments` | Add a comment (author = me) | 201 |

### Attachments (require `Authorization: Bearer <token>`)

| Method | Path | Description | Success code |
|--------|------|-------------|--------------|
| POST | `/api/tasks/{id}/attachments` | Multipart upload (field `file`, ≤5 MB, png/jpeg/pdf/txt) | 201 |
| GET | `/api/tasks/{id}/attachments` | List a task's attachments | 200 |
| GET | `/api/attachments/{id}/link` | 15-min presigned S3 download URL | 200 |

Files live in **S3** (LocalStack locally — started by docker-compose);
MySQL stores only metadata. Upload with curl:

```bash
curl -X POST http://localhost:8080/api/tasks/1/attachments \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@notes.txt;type=text/plain"
```
> Presigned URLs generated inside compose use the `localstack` hostname —
> replace it with `localhost` when downloading from your browser/host.

**Pagination (all list endpoints):** `?page=0&size=20&sort=field,desc` —
`size` caps at 100; sort fields are whitelisted per resource (tasks:
`id,title,status,priority,dueDate,createdAt`; projects: `id,name,createdAt`);
tasks also filter by `?status=` and `?priority=`. Lists return an envelope:

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 5, "totalPages": 1 }
```

**Access model:** no token → **401**. Everything is scoped to the caller:
you only see projects you own (and the tasks/comments inside them) — touching
someone else's returns **403**. Admins see everything.
An ADMIN account is created at startup (credentials via `ADMIN_EMAIL` /
`ADMIN_PASSWORD`; dev default `admin@teamflow.local` / `admin1234`).
Demo data is **opt-in** (`DEMO_DATA=true`): docker-compose sets it for local
development, seeding a demo MEMBER (`demo@teamflow.local` / `demo1234`) with
a sample project, tasks and comments. Deployments without that flag stay clean.

### Try it with curl

```bash
# 1. Sign up
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"me@example.com","password":"secret-pass-1","displayName":"Me"}'

# 2. Log in and grab the token from the response
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"me@example.com","password":"secret-pass-1"}'

# 3. Call the API with the token (replace <TOKEN>)
curl http://localhost:8080/api/tasks -H "Authorization: Bearer <TOKEN>"

# List only in-progress tasks
curl "http://localhost:8080/api/tasks?status=IN_PROGRESS" -H "Authorization: Bearer <TOKEN>"

# Create a project, then a task inside it
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Job hunt","description":"Interview prep work"}'

curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Prepare for interview","projectId":1,"priority":"HIGH","dueDate":"2026-08-20"}'

# Comment on a task
curl -X POST http://localhost:8080/api/tasks/1/comments \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"body":"Focus on JPA relations and the N+1 problem"}'

# Update a task
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated title","status":"DONE","priority":"LOW"}'

# Delete a task (needs an ADMIN token)
curl -X DELETE http://localhost:8080/api/tasks/1 -H "Authorization: Bearer <TOKEN>"
```

> Tip: in Swagger UI, click **Authorize**, paste your token, and every
> "Try it out" call sends it automatically.

### Example: validation error (blank title)

Request body `{"title":""}` returns **HTTP 400**:

```json
{
  "status": 400,
  "message": "Validation failed",
  "fieldErrors": { "title": "Title is required" }
}
```

### Example: not found

`GET /api/tasks/999` returns **HTTP 404**:

```json
{
  "status": 404,
  "message": "Task with id 999 was not found"
}
```

---

## Project structure

```
teamflow/
├── pom.xml                       # Maven build + dependencies
├── Dockerfile                    # Multi-stage build (Maven -> small runtime)
├── docker-compose.yml            # App + MySQL together
├── generate-jwt-keys.sh          # Creates local JWT keys (keys are gitignored)
├── k8s/
│   └── deployment.yaml           # Kubernetes Deployment + Service
├── src/main/resources/
│   ├── application.properties    # Configuration (DB, JWT, HTTP, etc.)
│   ├── jwt/                      # RSA keypair (generated, never committed)
│   └── import.sql                # Sample seed data
├── src/main/java/com/teamtask/
│   ├── model/                    # Task + User entities, enums (map to DB)
│   ├── security/                 # JWT creation + bootstrap admin
│   ├── repository/               # Database access (Panache)
│   ├── service/                  # Business logic + transactions
│   ├── dto/                      # Request/response JSON shapes
│   ├── exception/                # Custom errors + HTTP mappers
│   └── resource/                 # REST endpoints
└── src/test/java/com/teamtask/
    └── TaskResourceTest.java     # Integration tests
```

---

## Local development (after installing JDK 21 + Maven)

```bash
# Live-reload dev mode (auto-restarts on code change,
# and auto-starts a MySQL container for you via Dev Services)
mvn quarkus:dev

# Run the tests (needs Docker running)
mvn test

# Build a runnable app
mvn clean package
```

---

## Cloud deployment (AWS)

The container produced here maps directly onto AWS services:

- **Amazon ECS** (or **EKS** for Kubernetes) runs the container.
- **Amazon RDS for MySQL** is the managed database (swap the `DB_URL`).
- **Amazon S3** would store any file uploads/attachments (natural next feature).

Because the app reads its database settings from environment variables, moving from local Docker to AWS is just a matter of pointing `DB_URL`, `DB_USER`, and `DB_PASSWORD` at RDS.

---

## What this project demonstrates

- Designing clean, correctly-status-coded RESTful APIs
- Relational schema design and querying with MySQL
- Layered architecture (resource / service / repository)
- Input validation and consistent error handling
- Containerization with Docker and multi-container orchestration
- Awareness of Kubernetes and cloud (AWS) deployment
- Automated testing
