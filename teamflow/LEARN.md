# LEARN.md — Your study guide for this project

This is your personal cheat-sheet. Read the code with this file open next to it.
By the end you should be able to explain **every file** and answer the common
interview questions at the bottom. That is what "confidence" really means: not
memorizing, but being able to explain *why* each piece exists.

Study order: read a section, open the matching file, then say the explanation
out loud in your own words. If you can teach it, you know it.

---

## 0. The big picture (say this in one breath)

> "It's a REST API for managing tasks. A request comes in over HTTP, hits the
> **Resource** layer, which calls the **Service** layer for business logic,
> which uses the **Repository** to read/write **MySQL**. Data going in and out
> is shaped by **DTOs**, and it all runs in a **Docker** container."

If you can say that and point at each folder, you already sound like a backend developer.

---

## 1. What is Quarkus? (and why not plain Java?)

Quarkus is a Java framework for building backend apps, like Spring Boot but
newer. It is **"container-first"**: it starts in milliseconds and uses little
memory, which is perfect for Docker and cloud (you pay for memory in the cloud).

**Interview answer to "why Quarkus?":**
"Fast startup and low memory footprint because it does a lot of work at build
time instead of at runtime. That makes it ideal for containers, Kubernetes, and
serverless where quick startup and small size matter."

> If you already know Spring Boot, 90% transfers: `@RestController` -> Resource,
> `@Service` -> Service, `@Repository` -> Repository, `application.yml` ->
> `application.properties`.

---

## 2. REST API basics (the vocabulary you MUST own)

REST = a style for building web APIs using HTTP. Key ideas:

- **Resource**: a thing you manage. Here it's a *task*. URL: `/api/tasks`.
- **HTTP methods** = the verb (what you want to do):
  - `GET` = read, `POST` = create, `PUT` = update, `DELETE` = remove.
- **Status codes** = the result:
  - `200 OK`, `201 Created`, `204 No Content`,
  - `400 Bad Request` (client sent bad data), `404 Not Found`, `500` (server bug).
- **JSON** = the text format for the data in requests/responses.

Look at `TaskResource.java` — each method is one method+URL combination, and it
returns the correct status code. **Interviewers love correct status codes.**

---

## 3. The layers (this is "clean architecture")

| Layer | File | One-line job |
|-------|------|--------------|
| Resource | `resource/TaskResource.java` | Speak HTTP. Map URLs to methods. |
| Service | `service/TaskService.java` | Business rules + transactions. |
| Repository | `repository/TaskRepository.java` | Talk to the database only. |
| Entity | `model/Task.java` | A Java object that maps to a DB row. |
| DTO | `dto/TaskRequest`, `dto/TaskResponse` | The JSON shape in/out. |

**Why bother splitting it?** So each part has one reason to change and can be
tested on its own. If you switched from MySQL to PostgreSQL, only the repository
area cares. If you change the JSON format, only the DTOs/resource care.

**Interview answer to "why separate service and repository?":**
"Separation of concerns. The repository only knows about data access; the
service holds business logic. It keeps code testable and easy to change."

---

## 4. How Java talks to MySQL (ORM + Panache)

You almost never write raw SQL for simple things. Instead:

- **JPA/Hibernate (ORM = Object-Relational Mapping)** maps a Java class to a
  table. `Task.java` with `@Entity` + `@Table(name="tasks")` becomes the
  `tasks` table. Each field (`title`, `status`, ...) becomes a column.
- **Panache** is Quarkus's helper that gives you ready-made methods:
  `persist()`, `findById()`, `listAll()`, `deleteById()`. You saw them used in
  the service — you never wrote the SQL for them.

Key annotations in `Task.java` to be able to explain:
- `@Id` = primary key. `@GeneratedValue(IDENTITY)` = MySQL auto-increments it.
- `@Column(nullable=false, length=150)` = column rules (NOT NULL, VARCHAR(150)).
- `@Enumerated(STRING)` = store the enum as text ("TODO") not a number.
- `@PrePersist` = a hook that runs right before insert (we stamp createdAt).

**You must still understand real SQL** (the job asks for it). This project runs
`SELECT`, `INSERT`, `UPDATE`, `DELETE` under the hood — turn on
`quarkus.hibernate-orm.log.sql=true` (already on) and WATCH the SQL scroll past
in the logs. That's a great way to connect Java code to actual SQL.

---

## 5. Dependency Injection (DI) — the "magic" explained

Notice `TaskService` never does `new TaskRepository()`. Instead the repository
arrives through the constructor. Quarkus **creates the objects and wires them
together for you**. That's dependency injection (via CDI / the "Arc" engine).

- `@ApplicationScoped` = "make one shared instance of this and reuse it."
- Constructor injection = "give me what I need when you build me."

**Why it's good:** loose coupling and easy testing (you can swap in a fake repo
in a test). This is one of the most common interview topics — be ready for it.

---

## 6. Validation — never trust client input

In `TaskRequest.java`, `@NotBlank`, `@Size`, `@FutureOrPresent` are rules. The
`@Valid` in `TaskResource` triggers them. If the data breaks a rule, the request
is rejected with `400` *before* any business logic runs.

`ValidationExceptionMapper` turns those failures into a clean JSON message.

**Interview answer to "how do you validate input?":**
"Bean Validation annotations on the DTO, triggered by @Valid. Invalid requests
get a 400 with field-level messages, so bad data never reaches the database."

---

## 7. Transactions — all-or-nothing

`@Transactional` on the service methods means a group of DB operations either
**all succeed or all roll back**. If something fails halfway, the database is
left unchanged. That's what keeps data consistent (e.g. you never half-create
a record).

---

## 8. Error handling — Exception Mappers

Instead of leaking ugly stack traces, we throw meaningful exceptions
(`TaskNotFoundException`) and map them to proper HTTP responses
(`TaskNotFoundExceptionMapper` -> 404). Consistent error shape = professional API.

---

## 9. Docker — "it works on my machine" solved

- A **container** is your app + everything it needs, packaged together, so it
  runs the same everywhere.
- The **Dockerfile** is the recipe. Ours is **multi-stage**: stage 1 builds with
  Maven, stage 2 keeps only the finished app + a small Java runtime. Smaller,
  safer final image.
- **Docker Compose** runs multiple containers together — here the **app** and
  **MySQL** — and connects them on a network. Notice the app reaches the DB at
  host `mysql` (the service name), not `localhost`.

**Interview answer to "why containers?":**
"Consistency and portability — the same image runs on my laptop, in CI, and in
the cloud. And they start fast, which suits scaling."

---

## 10. Kubernetes & AWS — the words to know

You don't need deep skill yet, just correct vocabulary:

- **Kubernetes (K8s)** runs and manages many containers across many machines.
  - **Pod** = one running instance of your container.
  - **Deployment** = "keep N copies running, restart them if they die."
  - **Service** = one stable address in front of those pods.
  - See `k8s/deployment.yaml`.
- **AWS** services this maps to:
  - **ECS / EKS** = run your container (EKS = managed Kubernetes).
  - **RDS** = managed MySQL database (instead of running MySQL yourself).
  - **S3** = store files/objects (images, uploads, backups).

**Interview answer to "how would you deploy this to AWS?":**
"Build the Docker image, push it to ECR (the registry), run it on ECS or EKS,
and point the DB settings at an RDS MySQL instance. File uploads would go to S3."

---

## 11. Your 6 most-likely interview questions (rehearse out loud)

1. **Walk me through what happens when a POST /api/tasks request arrives.**
   -> Resource receives JSON -> mapped to TaskRequest -> @Valid checks it ->
   Service.create runs in a transaction -> Repository.persist inserts into MySQL
   -> returns 201 with the created task as JSON.

2. **Why did you separate the layers?**
   -> Separation of concerns; testability; each layer has one responsibility.

3. **What is an ORM / what does Hibernate do?**
   -> Maps Java objects to database tables so I work with objects, not raw SQL.

4. **How do you handle invalid input?**
   -> Bean Validation on the DTO + @Valid; returns 400 with field errors.

5. **What is a transaction and why does it matter?**
   -> A group of DB operations that all succeed or all roll back; keeps data
   consistent.

6. **Why Docker, and what is a multi-stage build?**
   -> Portability/consistency; multi-stage builds in one image and ships a small
   runtime image, keeping the final image lean and secure.

---

## 12. Small exercises to make it truly yours

Do these after it runs — they force real understanding:

1. Add a new field to `Task` (e.g. `assignee` / a person's name). Follow it
   through: entity -> DTO -> and see the new column appear in MySQL.
2. Add an endpoint `GET /api/tasks/count` that returns how many tasks exist.
   (Hint: `repository.count()`.)
3. Add a validation rule: description must be at least 5 characters if provided.
4. Connect to the running MySQL (port 3306, user `taskuser` / `taskpass`) with
   a tool like DBeaver or MySQL Workbench and run `SELECT * FROM tasks;`.
5. Break something on purpose (misspell a column) and read the error — learning
   to read errors is a real skill.

When you can do all five without help, you understand this project cold.
