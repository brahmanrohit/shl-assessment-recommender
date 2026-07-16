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

Base URL: `http://localhost:8080/api/tasks`

| Method | Path | Description | Success code |
|--------|------|-------------|--------------|
| GET | `/api/tasks` | List all tasks (optional `?status=TODO`) | 200 |
| GET | `/api/tasks/{id}` | Get one task by id | 200 |
| POST | `/api/tasks` | Create a task | 201 |
| PUT | `/api/tasks/{id}` | Update a task | 200 |
| DELETE | `/api/tasks/{id}` | Delete a task | 204 |

### Try it with curl

```bash
# List all tasks
curl http://localhost:8080/api/tasks

# List only in-progress tasks
curl "http://localhost:8080/api/tasks?status=IN_PROGRESS"

# Get one task
curl http://localhost:8080/api/tasks/1

# Create a task
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Prepare for interview","priority":"HIGH","dueDate":"2026-08-20"}'

# Update a task
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated title","status":"DONE","priority":"LOW"}'

# Delete a task
curl -X DELETE http://localhost:8080/api/tasks/1
```

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
task-manager-api/
├── pom.xml                       # Maven build + dependencies
├── Dockerfile                    # Multi-stage build (Maven -> small runtime)
├── docker-compose.yml            # App + MySQL together
├── k8s/
│   └── deployment.yaml           # Kubernetes Deployment + Service
├── src/main/resources/
│   ├── application.properties    # Configuration (DB, HTTP, etc.)
│   └── import.sql                # Sample seed data
├── src/main/java/com/teamtask/
│   ├── model/                    # Task entity + enums (maps to DB)
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
