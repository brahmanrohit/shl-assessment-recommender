# Phases.md — The build roadmap

> **What this file is for:** Breaks the project into ordered, manageable steps.
> We build ONE phase at a time. A phase is "done" only when its Definition of
> Done is fully met. Current status lives in Memory.md.

---

## Phase 0 — Foundation: Task CRUD API ✅ DONE

Task entity + CRUD REST endpoints, MySQL, validation, error mappers,
Swagger UI, Docker multi-stage build, Docker Compose, integration tests.

---

## Phase 1 — Users + JWT authentication ✅ DONE (2026-07-16)

**Goal:** Real signup/login; every task endpoint requires a valid token.

> Shipped as designed, plus: Swagger "Authorize" button, generate-jwt-keys.sh,
> Docker build generates its own keypair (keys gitignored). Verified live 9/9.

**Scope**
- `User` entity (email unique, passwordHash, displayName, role ADMIN|MEMBER)
- `POST /api/auth/signup` — validates input, hashes password with BCrypt, saves user
- `POST /api/auth/login` — verifies password, returns a signed JWT
- Add `quarkus-smallrye-jwt` + `quarkus-smallrye-jwt-build`; generate RSA keypair for signing
- Protect all `/api/tasks` endpoints: `@RolesAllowed({"ADMIN","MEMBER"})`
- Only ADMIN may DELETE tasks *(superseded in Phase 2: the ownership rule
  applies to deletes too — project owner or admin)*
- Never return passwordHash in any response

**What you LEARN:** password hashing vs encryption, what a JWT actually is
(header.payload.signature), stateless auth, roles/authorization vs authentication.

**Definition of Done**
- Signup → login → call /api/tasks with `Authorization: Bearer <token>` works
- Calling without a token returns 401; MEMBER calling DELETE returns 403
- Duplicate-email signup returns 409 with standard error shape
- Tests for all the above; LEARN.md gains an "Auth" section; Memory.md updated

---

## Phase 2 — Projects & Comments (entity relations) ✅ DONE (2026-07-16)

> Shipped as designed, then hardened by a multi-agent adversarial review
> (4 lenses, 3 skeptics per finding): DB-level ON DELETE CASCADE instead of
> row-by-row JPA cascade, list queries slimmed (no pointless project fetch),
> task DELETE aligned to the ownership rule, demo data made opt-in
> (DEMO_DATA=true only in docker-compose), @Positive on assigneeId, extra
> ownership tests. Known accepted dev-only risk: stale JWTs across
> drop-and-create restarts can map to reused ids — closed by Phase 5.

**Goal:** Real relational schema: Project → Tasks → Comments.

**Scope**
- `Project` entity (name, description, owner → User)
- Task gets `project_id` (required) and optional `assignee_id`
- `Comment` entity (task, author, body, createdAt)
- Endpoints: CRUD projects; `GET /api/projects/{id}/tasks`;
  `POST /api/tasks/{id}/comments`; `GET /api/tasks/{id}/comments`
- Ownership rule (decided at build time): a project and everything inside it
  is visible/modifiable by its OWNER and by ADMINs — other members get 403.
  (A member-list/sharing table is future work, not this phase.)
- Replace import.sql with a code-based DemoDataBootstrap (seed rows now need
  FK-valid users, which only exist at runtime)

**What you LEARN:** @ManyToOne/@OneToMany, foreign keys, LAZY vs EAGER
fetching, the N+1 query problem and how to spot it in the SQL log.

**Definition of Done:** relations visible in MySQL (`SHOW CREATE TABLE tasks`),
endpoints tested, no accidental N+1 in the list endpoints.

---

## Phase 3 — Pagination, filtering, sorting

**Goal:** No endpoint returns unbounded lists.

**Scope**
- `GET /api/tasks?page=0&size=20&sort=dueDate,asc&status=TODO&priority=HIGH`
- Standard page envelope: `{ content, page, size, totalElements, totalPages }`
- Panache `.page(Page.of(page, size))` + dynamic sort; cap max size at 100

**What you LEARN:** why unbounded queries kill real systems, OFFSET/LIMIT SQL,
query-parameter design.

**Definition of Done:** list endpoints paginated + tested (incl. out-of-range page).

---

## Phase 4 — File attachments via S3

**Goal:** Upload/download attachments on tasks; files in S3, metadata in MySQL.

**Scope**
- Add AWS S3 SDK (quarkus-amazon-s3); run **LocalStack** in docker-compose for local dev
- `POST /api/tasks/{id}/attachments` (multipart upload) → store in S3, save Attachment row
- `GET /api/attachments/{id}` → presigned download URL
- Limits: max 5 MB, whitelist content types

**What you LEARN:** object storage vs database, S3 keys/buckets, presigned URLs,
why files don't belong in MySQL.

**Definition of Done:** upload + download works locally against LocalStack, tested.

---

## Phase 5 — Flyway migrations

**Goal:** Production-style schema management.

**Scope**
- Add Flyway; write `V1__init.sql` capturing the full current schema (+ indexes)
- Switch `database.generation` to `validate`; seed data via `V2__seed.sql` (dev only)

**What you LEARN:** why prod DBs are never auto-generated, migration versioning.

**Definition of Done:** clean `docker compose up` builds schema via Flyway only.
Also closes the accepted Phase 2 risk: without drop-and-create, ids are never
reused, so stale JWTs can no longer map onto a different user after restarts.

---

## Phase 6 — CI/CD with GitHub Actions

**Goal:** Every push is automatically built and tested.

**Scope**
- `.github/workflows/ci.yml`: checkout → JDK 21 → `mvn verify` (tests use
  Dev Services MySQL) → build Docker image
- Badge in README; optional: push image to a registry on tags

**What you LEARN:** what CI actually is, pipeline-as-code, why teams refuse to
merge red builds.

**Definition of Done:** green run visible on GitHub Actions tab; badge in README.

---

## Phase 7 — Health checks + metrics (observability)

**Goal:** The service can prove it is alive and expose metrics.

**Scope**
- `quarkus-smallrye-health`: `/q/health/live` + `/q/health/ready`
  (readiness includes a DB check)
- `quarkus-micrometer-registry-prometheus`: `/q/metrics`
- Wire the K8s manifest's liveness/readiness probes to these endpoints

**What you LEARN:** liveness vs readiness, why K8s needs them, what metrics
matter (request rate, latency, errors).

**Definition of Done:** endpoints respond correctly; K8s yaml updated; README updated.

---

## Backlog (only after all phases)
Rate limiting, refresh tokens, search endpoint, Testcontainers-pinned tests,
deploy for real on AWS ECS + RDS + S3, native image build.
