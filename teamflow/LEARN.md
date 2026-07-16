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
(`TaskNotFoundException`) that each carry their HTTP status, and ONE
`ApiExceptionMapper` turns them all into the standard error JSON.
(JAX-RS picks the mapper of the nearest superclass — so one mapper for the
`ApiException` base covers every subclass.) Consistent error shape =
professional API, without seven copy-pasted mapper classes.

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

---

## 13. Authentication & Authorization (Phase 1) — the big one

Interviews LOVE this topic. Learn it from your own working code.

### The two words (never mix them up)

- **Authentication** = *who are you?* (login proves identity)
- **Authorization** = *what may you do?* (roles decide permissions)

Getting 401 means "I don't know who you are" (no/bad token).
Getting 403 means "I know who you are, but you're not allowed" (wrong role).
Try both against the API and watch the difference.

### Password storage — hashing, not encryption

We NEVER store the password. `AuthService` stores a **BCrypt hash**:

- Hashing is **one-way**: you can't get the password back from the hash.
  Encryption is two-way (that's why it's wrong for passwords).
- BCrypt is deliberately **slow** and adds a random **salt** per password,
  making stolen hashes extremely expensive to crack.
- Login never says *what* was wrong ("Invalid email or password") — precise
  errors would let attackers discover which emails have accounts.

### JWT — the token that carries its own proof

After login we hand out a **JWT**: `header.payload.signature`

- The **payload** holds claims: user id (`sub`), email (`upn`), role
  (`groups`), expiry (`exp`), issuer (`iss`). Anyone can *read* it —
  never put secrets inside.
- The **signature** is made with our **private key**. The server verifies it
  with the **public key** on every request. Change one character of the
  payload and the signature breaks.
- This makes auth **stateless**: no session storage on the server. Any
  instance of the app (think: 5 pods on Kubernetes) can verify the token
  alone — that's why JWTs scale so well.

Paste your token into jwt.io and look inside — genuinely eye-opening.

### How a protected request flows

```
Authorization: Bearer <jwt>
      |
      v
Quarkus verifies signature (public key) + expiry + issuer   -> else 401
      |
      v
@RolesAllowed({"ADMIN","MEMBER"}) checks the groups claim   -> else 403
      |
      v
TaskResource method finally runs
```

We never wrote that filter code — declaring the *rules* (`@RolesAllowed`,
`@PermitAll`) is enough. Declarative security = fewer bugs.

### Design decisions worth explaining in an interview

1. **Signup can't choose a role** (no `role` field in SignupRequest) —
   otherwise anyone could register as ADMIN. Privilege escalation, blocked.
2. **The first admin comes from the system** (`AdminBootstrap` at startup,
   credentials from env vars) — solves the chicken-and-egg problem cleanly.
3. **Keys are never committed** — gitignored; generated by script locally and
   inside the Docker build. In real production they'd come from a secret
   manager (AWS Secrets Manager / Vault).
4. **`UserResponse` has no passwordHash field** — DTOs are the guarantee that
   sensitive columns can't leak, even by accident.

### New interview questions you can now answer

7. **How does login work in your project?**
   -> Signup stores a BCrypt hash. Login verifies the password against the
   hash and returns a signed JWT carrying the user's id and role. Every later
   request sends `Authorization: Bearer <token>`; the server verifies the
   signature and expiry, and @RolesAllowed enforces the role.

8. **Why JWT instead of sessions?**
   -> Stateless: no server-side session store, so it scales horizontally —
   any instance (or pod) can verify the token with just the public key.

9. **Difference between 401 and 403?**
   -> 401 = not authenticated (missing/invalid token). 403 = authenticated
   but not authorized (e.g. touching a project that belongs to someone else).

10. **Why BCrypt and not SHA-256 for passwords?**
    -> SHA-256 is fast — attackers can try billions of guesses per second.
    BCrypt is deliberately slow and salted, exactly what you want for passwords.

---

## 14. Entity relations & the N+1 problem (Phase 2)

This is THE core database skill the job posting asks for. Learn it from
`Project.java`, `Comment.java`, and the updated `Task.java`.

### The schema (say it while drawing arrows)

```
users ──owns──> projects ──contain──> tasks ──have──> comments
                                        └── assignee ──> users (optional)
```

Every arrow is a **foreign key** (FK): a column holding the id of a row in
another table (`tasks.project_id -> projects.id`). The database REFUSES rows
that point at nothing — that's *referential integrity*. Run
`SHOW CREATE TABLE tasks;` in MySQL and see the constraints Hibernate made.

### The annotations that build those arrows

- `@ManyToOne` — MANY tasks point to ONE project. This side owns the FK
  column (`@JoinColumn(name = "project_id")`).
- `@OneToMany(mappedBy = "project")` — the mirror side, a Java convenience
  list. "mappedBy" = "the FK lives over there, don't make a second one."
- `cascade = REMOVE, orphanRemoval = true` — delete a project and its tasks
  die with it; each task takes its comments along. One DELETE, whole subtree.

### LAZY vs EAGER (interviewers love this)

`fetch = FetchType.LAZY` = don't load the related row until someone calls a
getter needing its DATA. We use LAZY everywhere because EAGER would join in
the owner/project/assignee on EVERY query whether needed or not.
Subtlety worth quoting in an interview: on a lazy proxy, `.getId()` does
NOT hit the database (the id was already in the FK column), but
`.getDisplayName()` DOES.

### The N+1 problem — and our fix

Naive code: load 100 tasks (1 query), then map each to JSON touching
`task.getAssignee().getDisplayName()` → 100 extra SELECTs. Total: 101
queries. That's N+1, the most common real-world ORM performance bug.

Our fix: the repository queries say `join fetch` —
`select t from Task t join fetch t.project left join fetch t.assignee` —
one SQL query loads everything. Proof: watch the SQL log while calling
`GET /api/tasks`; you'll see ONE select, not dozens. (Why `left` join fetch
for assignee? A plain join would silently DROP tasks with no assignee.)

### Two-level authorization (roles + ownership)

Phase 1 answered "are you logged in, and what role?" (@RolesAllowed).
Phase 2 adds *object-level* checks: "is this specific project YOURS?" —
`ProjectService.findAccessible()` is the single gatekeeper; tasks and
comments delegate to it, so the rule lives in exactly one place. Skipping
this check is the OWASP #1 API vulnerability (BOLA/IDOR): being logged in
would let you read ANYONE's data by guessing ids. Try it: create two users
and fetch the other's project — you get our clean 403.

### New interview questions you can now answer

11. **How do you model one-to-many in JPA?**
    -> @ManyToOne on the child (owns the FK column) + mappedBy @OneToMany on
    the parent; LAZY fetching; cascade only where lifecycle is truly shared.

12. **What is the N+1 problem and how do you fix it?**
    -> 1 query for the list + N lazy loads while mapping rows. Fix: fetch the
    needed relations in the same query (join fetch / EntityGraph), verify in
    the SQL log.

13. **How do you stop users reading each other's data?**
    -> Object-level authorization: every read/write resolves the resource,
    then checks the owner against the JWT's user id before proceeding —
    404 if it doesn't exist, 403 if it isn't yours.

14. **Why did your seed data move from import.sql to Java code?**
    -> Phase 2 rows need BCrypt-hashed users and an FK chain
    (users → projects → tasks); a static SQL file can't hash passwords, so a
    startup bean seeds it — and can be disabled by env var in production.

---

## 15. Pagination, filtering, sorting (Phase 3)

### Why unbounded lists kill real systems

`GET /tasks` returning EVERYTHING works with 5 rows. With 5 million it:
loads them all into memory (OOM), serializes megabytes of JSON per request,
and locks the database longer per query. Rule: **every list endpoint is
bounded**. Ours cap at 100 rows per page, no exceptions.

### The envelope

```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 143, "totalPages": 8 }
```
Metadata travels WITH the data so clients can render "page 3 of 8" without
guessing. One generic record (`PageResponse<T>`) serves every endpoint.

### What happens underneath: LIMIT/OFFSET + a COUNT

`.page(Page.of(2, 20))` becomes SQL `LIMIT 20 OFFSET 40`. And the total?
That's a SECOND query (`SELECT COUNT(*)` with the same WHERE). Two queries
per page is normal — we measured exactly 2 in the SQL log. Interview bonus:
for huge datasets OFFSET itself gets slow (the DB still walks the skipped
rows) — the fix is *keyset pagination* ("everything after id X"), worth
mentioning even though we don't need it here.

### Two details seniors check for

1. **Stable ordering:** every ORDER BY ends with `, t.id` as a tiebreaker.
   Without it, rows with equal sort values can shuffle between pages —
   users see duplicates/gaps while paging. Subtle, classic, real.
2. **The sort whitelist (security!):** `?sort=` input ends up inside an
   ORDER BY clause. We map allowed names (`dueDate` → `t.dueDate`) through
   a whitelist and REJECT everything else with 400. Concatenating raw user
   input into any query string — even just ORDER BY — is how injection
   starts. Try `?sort=passwordHash`: clean 400, and the input never
   touches the query.

### Parameter hygiene

`page < 0` → 0. `size` missing → 20, `size > 100` → 100 (clamped — a client
asking for a million rows gets 100). Out-of-range page → `content: []` with
correct metadata, still 200 (a valid question with an empty answer).
All parsing lives in ONE place (`PageParams`) so every endpoint behaves
identically.

### New interview questions you can now answer

15. **How do you paginate an API?**
    -> page/size params (0-based, size capped), LIMIT/OFFSET underneath,
    a count query for totals, and a standard envelope with metadata.

16. **How do you let clients sort safely?**
    -> `sort=field,dir` validated against a whitelist that maps API names to
    column paths; anything else is a 400. User input never reaches the query.

17. **Why does your ORDER BY end with the id?**
    -> Deterministic tiebreaker: equal values would otherwise shuffle
    between pages and clients would see duplicates or gaps.

18. **What's the cost of pagination?**
    -> Two queries per page (rows + count); for very deep pages OFFSET
    degrades and keyset pagination is the scalable alternative.
