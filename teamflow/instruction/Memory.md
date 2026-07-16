# Memory.md — Project memory (living document)

> **What this file is for:** The running log of what has been done, what was
> decided and WHY, and what comes next. Any AI tool or new chat session reads
> this first so it never loses context, re-reads the whole codebase, or makes
> things up. **Update this at the end of every working session.**

---

## Current status

- **Phase:** 0 complete ✅ — Phase 1 (Users + JWT auth) is NEXT
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

1. Owner explores the running API at http://localhost:8080/swagger-ui
2. Owner studies LEARN.md sections 0–5 against the code
3. Revisit project architecture together with owner (owner requested), then
   begin **Phase 1** (see Phases.md): User entity → signup → login → JWT → protect endpoints

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
