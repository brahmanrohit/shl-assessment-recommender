# Rules.md — Boundaries for the AI (and for us)

> **What this file is for:** Hard rules any AI assistant must follow when
> working on this project. Prevents the AI from drifting, over-building,
> or making choices that hurt the learning goal.

---

## 1. Golden rule: this is a LEARNING project

- The owner is a career-switcher. **Every new concept introduced must be
  explained** — either in code comments or by extending `LEARN.md` with a new
  section. Code the owner cannot explain in an interview is worthless here.
- Prefer the **simple, standard way** over the clever way.

## 2. Stack rules

- Java 21, Quarkus **3.15 LTS BOM** — do not bump major versions casually.
- **No Spring dependencies.** This is a Quarkus project.
- **No Lombok.** Write getters/setters explicitly (owner must learn plain Java first).
- New libraries require a reason written in Memory.md ("added X because Y").
- MySQL only. No switching databases.

## 3. Architecture rules

- Respect the layers, strictly one direction:
  `resource → service → repository`. A resource NEVER touches a repository
  directly. A repository NEVER contains business logic.
- **Entities never cross the HTTP boundary** — every endpoint takes/returns DTOs.
- `@Transactional` lives in the **service** layer only.
- Every incoming DTO field gets validation annotations.
- All errors return the standard `ErrorResponse` JSON shape (see Design.md).
- Configuration via environment variables; **no secrets in code or git — ever.**

## 4. Process rules (how we work, phase by phase)

- **Follow Phases.md in order.** Do not start Phase N+1 while Phase N is
  incomplete. Do not "quickly also add" out-of-phase features.
- Every phase must end with: code compiles, tests pass, README/LEARN.md
  updated, **Memory.md updated**, one clean git commit.
- Small commits with clear messages, e.g. `phase-1: add JWT login endpoint`.
- Every new endpoint gets at least: 1 happy-path test + 1 error-path test.
- If a phase's plan turns out wrong, update Phases.md FIRST, then code.

## 5. What the AI should NOT do

- ❌ Don't generate a frontend, websockets, microservices, or anything in PRD non-goals.
- ❌ Don't refactor working code "for style" without being asked.
- ❌ Don't add abstractions for problems we don't have yet (no interfaces with
  one implementation, no generic frameworks).
- ❌ Don't silently change files outside the current phase's scope.
- ❌ Don't invent requirements — if something is unclear, ask or check PRD.md.

## 6. What the AI SHOULD always do

- ✅ Read `instruction/Memory.md` at the start of every new session.
- ✅ Explain the "why" of each significant decision in plain language.
- ✅ Keep error messages, naming, and JSON style consistent with existing code.
- ✅ Update Memory.md at the end of every working session.
