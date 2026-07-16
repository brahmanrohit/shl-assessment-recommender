# PRD.md — Product Requirements Document

> **What this file is for:** Defines WHAT we are building, WHO it is for, and
> WHICH features it must have. Any AI tool (or human) starting work on this
> project reads this first to understand the goal. It never contains code.

---

## 1. Product name

**TeamFlow** — a production-style backend REST API for team project management.

## 2. The problem it solves

Small teams need one place to organize work: projects, tasks inside those
projects, discussion on tasks, and file attachments — with proper access
control so only team members can see and change things.

## 3. Why this project exists (the real goal)

This is a **portfolio project** built by a career-switcher learning backend
development. Every feature is chosen to demonstrate a production backend skill
that the target job (Java + Quarkus + MySQL + Docker/K8s + AWS) requires.
Learning value > feature count. Depth > breadth.

## 4. Target users

| User | What they do |
|------|--------------|
| **Admin** | Creates/deletes projects, manages members, full control |
| **Member** | Sees their projects, creates/updates tasks, comments, uploads files |
| **Anonymous** | Can only sign up or log in — everything else is locked |

## 5. Features

### ✅ Done (Phase 0 — the foundation)
- Task CRUD over REST (create, read, update, delete)
- MySQL persistence with Hibernate/Panache
- Input validation + consistent JSON error responses
- Swagger UI documentation
- Docker + Docker Compose (app + MySQL)
- Integration tests

### ✅ Done (Phase 1 — auth, completed 2026-07-16)
- User accounts: signup (always MEMBER) + login, BCrypt password hashing
- RS256-signed JWTs (24h) carrying the role; keys generated, never committed
- All task endpoints require a token; DELETE is ADMIN-only (401 vs 403)
- Bootstrap ADMIN created at startup from env-configurable credentials
- Verified live: 9/9 security behaviors, incl. 409 duplicate email and
  no-password-hash-leak guarantees

### ✅ Done (Phase 2 — relations, completed 2026-07-16)
- Projects → Tasks → Comments with real foreign keys + DB-level cascade
- Per-owner access control (403 walls between members; admins see all)
- Hardened by multi-agent adversarial review before push

### ✅ Done (Phase 3 — pagination, completed 2026-07-16)
- Every list endpoint returns the PageResponse envelope; size capped at 100
- Whitelisted sorting (400 otherwise) + status/priority filters

### ✅ Done (Phases 4–7, completed 2026-07-16)
- **S3 attachments** — multipart upload, presigned downloads, LocalStack locally
- **Flyway migrations** — schema owned by versioned SQL, Hibernate validates
- **CI/CD** — GitHub Actions runs all 43 tests + Docker build on every push
- **Health + metrics** — /q/health/live, /q/health/ready (DB check), /q/metrics

### 🏁 ROADMAP COMPLETE — remaining work lives in the Phases.md backlog
(real AWS deployment, refresh tokens, rate limiting, S3 orphan cleanup, ...)

## 6. Non-goals (explicitly OUT of scope)

- ❌ No frontend/UI — this is a backend portfolio project (Swagger UI is the demo surface)
- ❌ No microservices split — one well-built service first
- ❌ No real-time features (websockets), no email sending
- ❌ No multi-tenancy / billing / anything enterprise-fancy

## 7. Success criteria

- The owner can **explain every file and every decision** out loud (interview-ready)
- `docker compose up --build` works first try on a clean machine
- CI is green; tests cover every endpoint's happy path + main error paths
- README makes a recruiter understand the project in 60 seconds
