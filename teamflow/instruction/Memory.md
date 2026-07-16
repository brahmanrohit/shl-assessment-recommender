# Memory.md — Project memory (living document)

> **What this file is for:** The running log of what has been done, what was
> decided and WHY, and what comes next. Any AI tool or new chat session reads
> this first so it never loses context, re-reads the whole codebase, or makes
> things up. **Update this at the end of every working session.**

---

## Current status

- **Phase:** 2 complete ✅ (Projects & Comments relations + ownership) — Phase 3 (pagination/filtering/sorting) is NEXT
- **Last updated:** 2026-07-16
- **CANONICAL LOCATION:** `C:\Users\ROHIT SHARMA\shl-assessment-recommender\teamflow\`
  — merged into the `shl-assessment-recommender` repo as a subfolder (owner's
  choice, via `git subtree add`, history preserved). ALL future work happens HERE.
- Repo branch layout: local branch `main` pushes to `origin/deploy`
  (the GitHub default branch is `deploy`)
- The old standalone folder `C:\Users\ROHIT SHARMA\quarkus-task-api\` is now
  DEPRECATED — don't edit it; owner may delete it after `docker compose down`

## Environment facts (this machine)

- Windows 11; work in `C:\Users\ROHIT SHARMA\shl-assessment-recommender\teamflow\`
- **Docker installed** (v29.5.2) but Docker Desktop must be STARTED before
  `docker compose` works (we hit "cannot connect to daemon" when it was off)
- **Java + Maven NOT installed locally** — that's fine: the multi-stage
  Dockerfile builds everything inside Docker
- Git identity for ALL commits: `brahmanrohit <rohitsharma20941@gmail.com>`
  (owner's request: no AI co-author tags; commits must show as owner's)
- `gh` CLI has multiple accounts on this machine — the `brahmanrohit` account
  must be active for pushes (`gh auth switch -u brahmanrohit`)

## Decisions log (what + why)

| Date | Decision | Why |
|------|----------|-----|
| 2026-07-16 | Quarkus 3.15 LTS, repository-pattern Panache, layered architecture | Matches target job stack; teaches clean architecture |
| 2026-07-16 | No Lombok, explicit getters/setters | Owner is learning Java fundamentals |
| 2026-07-16 | `drop-and-create` + import.sql for now | Instant demo data while learning; switches to Flyway in Phase 5 |
| 2026-07-16 | Evolve ONE project through phases instead of many small apps | Depth impresses recruiters more than breadth |
| 2026-07-16 | Chosen phase order: auth → relations → pagination → S3 → Flyway → CI → observability | Each phase = one interview-ready skill |
| 2026-07-16 | Merged TeamFlow into `shl-assessment-recommender` repo as `teamflow/` subfolder | Owner's choice (single repo); mentor recommended separate repo — owner decided; subtree merge kept all commits |

## What exists right now (Phase 0 inventory)

- `model/` Task + TaskStatus + TaskPriority
- `repository/` TaskRepository (Panache)
- `service/` TaskService (transactions, business logic)
- `dto/` TaskRequest (validated) / TaskResponse (record)
- `exception/` ErrorResponse + 404 & validation mappers
- `resource/` TaskResource — full CRUD + status filter
- `Dockerfile` (multi-stage), `docker-compose.yml` (app + MySQL 8.4)
- `k8s/deployment.yaml`, tests in `TaskResourceTest`
- Docs: `README.md` (recruiter-facing), `LEARN.md` (study guide)

## Known issues / open items

- [x] ~~Phase 0 not yet run end-to-end~~ ✅ VERIFIED 2026-07-16 (see session log)
- [x] ~~Not yet pushed to GitHub~~ ✅ merged into shl-assessment-recommender, push in progress
- [ ] Owner still working through LEARN.md sections
- [ ] Old `quarkus-task-api/` folder to be cleaned up by owner (run
      `docker compose down` there first if containers still running)

## Next actions (in order)

1. Owner studies LEARN.md sections 13 (auth) + 14 (relations/N+1) — the two
   most interview-critical sections — and plays with the two-user 403 demo
2. Owner does the LEARN.md exercises to cement Phases 0–2
3. Begin **Phase 3** (see Phases.md): pagination, filtering, sorting

## Session log

### 2026-07-16 — Session 1
- Built entire Phase 0 (23 files), committed `a4c88b3`
- Docker build attempt failed only because Docker Desktop daemon wasn't running
- Created `instruction/` folder: PRD, Architecture, Rules, Phases, Design, Memory

### 2026-07-16 — Session 2: PHASE 0 VERIFIED ✅
- Owner started Docker Desktop and ran `docker compose up --build` successfully
- Maven build compiled clean inside Docker (~79s); image `quarkus-task-api-app` built
- Both containers healthy: app on :8080, MySQL on :3306
- End-to-end tests all passed against the live API:
  - GET /api/tasks → 200, returned all 5 seeded rows from MySQL
  - POST valid task → 201, id 6 created, status defaulted to TODO
  - POST blank title → 400 `{"fieldErrors":{"title":"Title is required"}}`
  - GET /api/tasks/999 → 404 clean error JSON
  - GET /api/tasks?status=IN_PROGRESS → 200, filtered to 2 rows
- Lesson learned by owner: `docker compose` must run from the folder containing
  docker-compose.yml; PowerShell `cd` alone only prints the current directory

### 2026-07-16 — Session 3: MERGED INTO GITHUB REPO ✅
- Re-authored all commits to `brahmanrohit <rohitsharma20941@gmail.com>` so they
  link to the owner's GitHub profile (no AI co-author tags, per owner's request)
- `git subtree add --prefix=teamflow` merged the project into
  `shl-assessment-recommender` (history preserved: 3 commits + merge commit)
- Added a pointer to `teamflow/` in the SHL repo's root README
- Push target: local `main` → `origin/deploy` (GitHub default branch)
- Owner wants to revisit architecture together before starting Phase 1

### 2026-07-16 — Session 4: PHASE 1 COMPLETE ✅ (Users + JWT auth)
- Owner confirmed instruction/ plan stands as-is; built Phase 1 per Phases.md
- New: User entity (unique email, BCrypt password_hash, Role ADMIN|MEMBER),
  UserRepository, AuthService, TokenService (RS256 JWT, 24h, issuer=teamflow),
  AdminBootstrap (creates admin from ADMIN_EMAIL/ADMIN_PASSWORD at startup),
  AuthResource (POST /api/auth/signup 201, /api/auth/login 200),
  DuplicateEmail→409 + InvalidCredentials→401 mappers
- TaskResource now @RolesAllowed({ADMIN,MEMBER}); DELETE is ADMIN-only
- Keys NOT committed (gitignored): generate-jwt-keys.sh locally; Dockerfile
  generates a fresh pair per image build (openssl in build stage)
- Swagger UI has an Authorize button (quarkus.smallrye-openapi.security-scheme=jwt)
- Verified live via curl (9/9): 401 no-token, 201 signup (no hash leak,
  role=MEMBER), 409 duplicate, 401 wrong password (vague msg), token issued,
  200 with token, 403 member DELETE, 204 admin DELETE, 201 create with token
- Tests written (AuthResourceTest, AuthTestSupport, TaskResourceTest updated)
  but NOT executed locally (no Maven on machine; Dev Services needs local mvn) —
  they will run in CI (Phase 6). Honest gap, noted deliberately.
- LEARN.md gained section 13 (auth deep-dive + 4 new interview Q&As)

### 2026-07-16 — Session 5: PHASE 2 COMPLETE ✅ (relations + ownership)
- Schema is now relational: users → projects (owner_id) → tasks (project_id,
  optional assignee_id) → comments (task_id, author_id); cascade REMOVE +
  orphanRemoval down the chain (verified live: deleting a project 404s its task)
- New: Project + Comment entities, ProjectRepository/CommentRepository
  (all list queries use join fetch), ProjectService (findAccessible = single
  ownership gatekeeper: 404 unknown / 403 not-yours / admin bypass),
  CommentService, ProjectResource (/api/projects + /{id}/tasks),
  CommentResource (/api/tasks/{taskId}/comments), security/CurrentUser
  (@RequestScoped JWT wrapper), AccessDenied→403 + ProjectNotFound→404 +
  InvalidReference→400 mappers
- TaskService/TaskResource reworked: every operation scoped to caller;
  task create/update verifies target project ownership (incl. cross-project
  move); assigneeId validated (unknown → 400)
- import.sql DELETED → bootstrap/DemoDataBootstrap (demo@teamflow.local /
  demo1234, 1 project, 5 tasks, 2 comments; DEMO_DATA=false disables)
- Verified live 11/11: seeded relations render (ownerName/assigneeName),
  foreign project read/create → 403, scoped lists ([] for new user),
  own project+task+comment → 201s, cascade delete, admin-sees-all,
  unknown assignee → 400
- N+1 proof: GET /api/tasks (5 tasks + project + assignee) = EXACTLY 1 SQL
  select, measured in the Hibernate log (before/after counter delta)
- Multi-agent adversarial review (4 lenses × finders, 3 skeptics per finding,
  34 agents): 9 confirmed, 1 rejected. Fixes applied before push:
  1. Task DELETE aligned to ownership rule (owner-or-admin; was admin-only,
     contradicting our own Phase 2 rule) — Phase 1 note marked superseded
  2. DB-level ON DELETE CASCADE (@OnDelete) replaced JPA cascade REMOVE —
     project delete is now ONE SQL statement, not 1+N selects + M+N+1 deletes
  3. List queries slimmed: no `join fetch t.project` (DTO only reads the id,
     which the lazy proxy has for free); plain join only for owner filtering
  4. Demo data now OPT-IN (default false; docker-compose sets DEMO_DATA=true)
     — committed default-on demo credentials were CWE-1188
  5. @Positive added to assigneeId (Rules.md: every DTO field validated)
  6. 6 missing endpoint tests added (project list/get-404/put-403/delete-403/
     tasks-403, delete-own-task-204)
  7. Design.md now documents the body-reference status convention
     (container ref projectId→404/403; attribute ref assigneeId→400)
  8. ACCEPTED dev-only risk (documented): stale 24h JWTs survive
     drop-and-create restarts and ids restart from 1, so an old token can map
     to a new user. Real fix = Phase 5 Flyway (no more drop-and-create).
     ALSO backlog: admin default password (admin1234) should become env-only
     before any real deployment.
  Rejected by skeptics: "assigneeId lets members enumerate display names"
  (signup is open; names already visible in comments; nothing gained).
- 21 tests across Auth/Task/Project/Comment suites incl. two-member ownership
  boundaries (still CI-pending, Phase 6)
