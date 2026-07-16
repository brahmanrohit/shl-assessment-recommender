# Memory.md — Project memory (living document)

> **What this file is for:** The running log of what has been done, what was
> decided and WHY, and what comes next. Any AI tool or new chat session reads
> this first so it never loses context, re-reads the whole codebase, or makes
> things up. **Update this at the end of every working session.**

---

## Current status

- **Phase:** 0 complete ✅ — Phase 1 (Users + JWT auth) is NEXT
- **Last updated:** 2026-07-16
- **Repo state:** all Phase 0 code committed on branch `master`
  (initial commit `a4c88b3`), `instruction/` folder added

## Environment facts (this machine)

- Windows 11, working dir `C:\Users\ROHIT SHARMA\quarkus-task-api`
- **Docker installed** (v29.5.2) but Docker Desktop must be STARTED before
  `docker compose` works (we hit "cannot connect to daemon" when it was off)
- **Java + Maven NOT installed locally** — that's fine: the multi-stage
  Dockerfile builds everything inside Docker
- Folder rename to `teamflow` failed (folder locked, likely by VS Code) —
  cosmetic only; use repo name `teamflow` on GitHub, rename locally later

## Decisions log (what + why)

| Date | Decision | Why |
|------|----------|-----|
| 2026-07-16 | Quarkus 3.15 LTS, repository-pattern Panache, layered architecture | Matches target job stack; teaches clean architecture |
| 2026-07-16 | No Lombok, explicit getters/setters | Owner is learning Java fundamentals |
| 2026-07-16 | `drop-and-create` + import.sql for now | Instant demo data while learning; switches to Flyway in Phase 5 |
| 2026-07-16 | Evolve ONE project through phases instead of many small apps | Depth impresses recruiters more than breadth |
| 2026-07-16 | Chosen phase order: auth → relations → pagination → S3 → Flyway → CI → observability | Each phase = one interview-ready skill |

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
- [ ] Project not yet pushed to GitHub (repo name to use: `teamflow`)
- [ ] Owner still working through LEARN.md sections

## Next actions (in order)

1. Owner explores the running API at http://localhost:8080/swagger-ui
2. Owner studies LEARN.md sections 0–5 against the code
3. Push to GitHub as `teamflow`
4. Begin **Phase 1** (see Phases.md): User entity → signup → login → JWT → protect endpoints

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
