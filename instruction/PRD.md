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

### 🎯 To build (in order — see Phases.md for details)
1. **User accounts + JWT auth** — signup, login, BCrypt password hashing,
   roles (ADMIN / MEMBER), protected endpoints
2. **Projects & comments (relations)** — Project → has many Tasks → has many
   Comments; tasks belong to projects; proper foreign keys
3. **Pagination, filtering, sorting** — no endpoint ever returns unbounded lists
4. **File attachments via S3** — upload/download task attachments
   (LocalStack locally, real S3 on AWS)
5. **Flyway migrations** — versioned schema changes, production-style
6. **CI/CD with GitHub Actions** — every push builds + tests + produces a Docker image
7. **Health checks + metrics** — /q/health for Kubernetes probes, Prometheus metrics

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
