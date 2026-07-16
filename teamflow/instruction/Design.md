# Design.md — API design conventions

> **What this file is for:** In a frontend project this file covers colors and
> fonts. TeamFlow is a **backend API**, so its "design" is the **API contract**:
> naming, JSON shapes, status codes, and conventions. Consistency here is what
> makes an API feel professional.

---

## 1. URL design

- Base path: `/api`
- Resources are **plural nouns**: `/api/tasks`, `/api/projects`, `/api/users`
- Nesting max one level, only for true ownership:
  `/api/projects/{id}/tasks`, `/api/tasks/{id}/comments`
- No verbs in URLs (❌ `/api/getTasks` ✅ `GET /api/tasks`)
- Auth endpoints are the exception: `/api/auth/signup`, `/api/auth/login`

## 2. JSON conventions

- Field names: **camelCase** (`dueDate`, `createdAt`)
- Dates: ISO-8601 strings (`2026-08-20`, `2026-07-16T09:30:00`)
- Enums: UPPER_SNAKE strings (`IN_PROGRESS`, `HIGH`)
- Never return: password hashes, internal ids of other users' private data,
  stack traces

## 3. Status codes (use exactly these)

| Situation | Code |
|-----------|------|
| Read OK | 200 |
| Created | 201 (+ body of created resource) |
| Deleted / no body | 204 |
| Validation failed / malformed input | 400 |
| Not logged in / bad token | 401 |
| Logged in but not allowed (role/ownership) | 403 |
| Resource doesn't exist | 404 |
| Duplicate (e.g. email already registered) | 409 |
| Unexpected server bug | 500 |

### 3.1 Body references (decided in the Phase 2 review)

When a request BODY references another row by id, the status depends on the
reference's role:
- **Container reference** (`projectId` — decides WHERE the resource lives and
  whose access boundary applies): treated as a resource lookup → **404** if
  unknown, **403** if not yours.
- **Attribute reference** (`assigneeId` — just data on the resource): a bad
  id is malformed input → **400** with a message naming the field.

## 4. Error shape (every error, no exceptions)

```json
{
  "status": 400,
  "message": "Validation failed",
  "fieldErrors": { "title": "Title is required" }
}
```
`fieldErrors` is null unless it is a validation error.

## 5. Auth conventions (Phase 1+)

- Header: `Authorization: Bearer <jwt>`
- JWT carries: user id (sub), email, role; expiry 24h
- Roles: `ADMIN`, `MEMBER`

## 6. Pagination envelope (Phase 3+)

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 143,
  "totalPages": 8
}
```
Query params: `page` (0-based), `size` (default 20, max 100),
`sort=field,asc|desc`, plus resource-specific filters (`status`, `priority`).

## 7. Naming in code

- Entities: singular (`Task`), tables: plural snake_case (`tasks`)
- DTOs: `XxxRequest` / `XxxResponse`
- Endpoints methods: `list`, `getById`, `create`, `update`, `delete`
- Tests: `methodName_condition_expectedResult`
  (e.g. `createTask_withoutTitle_returns400`)
