---
name: mamxanh-backend-development
description: >
  Implement, review, or validate Backend changes for the Mâm Xanh Spring Boot application.
  Use repository-first discovery, preserve the modular-monolith package boundaries and API
  contract, and apply evidence-backed validation, security, persistence, migration, and test
  guidance. Use for Java, Controller, Service, Repository, Entity, DTO, REST API, validation,
  auth, query, transaction, database, Flyway, migration, SQL, and Backend test changes.
metadata:
  category: project-local-domain
  project: mamxanh
  source: adapted-from-harness-skills
  version: "v1.0.0"
---

# Mâm Xanh Backend Development

This is the project-local Backend domain skill. It supplies Backend expertise to the existing
`.agents/workflows/implement-fr-issue.md` workflow and never replaces project governance or
creates a competing harness workflow.

## Authority and hard gate

- Follow the current task, `AGENTS.md`, `CONTRIBUTING.md`, the current SRS/BR/NFR,
  `docs/architecture/ARCHITECTURE.md`,
  `docs/architecture/BACKEND-PACKAGE-STRUCTURE-PROPOSAL.md`, API contract, and database guide
  before generic guidance.
- Read `.agents/repo-contract.yml` only when the task needs machine-readable policy detail not
  already available in `AGENTS.md`.
- Do not change Backend code until the governing workflow confirms the Issue/FR scope and
  Acceptance Criteria, or the user explicitly requests implementation.
- Preserve the approved modular monolith and its existing layered architecture. Do not
  introduce microservices, a new framework, an architectural layer beyond the approved
  Controller/Service/Repository/Entity/DTO structure, or unnecessary abstractions without a
  demonstrated project need.
- `docs/diagrams/` remains read-only unless the current task explicitly authorizes named
  diagram artifacts.

## Confirmed project baseline

Verify manifests and implementation before relying on this list. The Backend uses the project's
confirmed Java/Spring Boot/Maven/JPA/Flyway/SQL Server/Spring Security stack. JUnit 5 and Spring
test support are present. Use JaCoCo only when coverage tooling is actually configured in the
current Maven build; do not infer coverage support from project documentation alone.

Exact framework and library versions are owned by `pom.xml` and
`docs/architecture/TECHNOLOGY-STACK.md`. Never select or upgrade a version from this skill text.

Backend code lives under `app/mamxanh-backend/`.

The approved Backend Package Structure Specification defines business-capability modules such
as `auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription`, and `admin`, with
layered packages created only when the corresponding vertical slice is implemented. Do not
treat a documented package as existing source code.

When an analogous implemented vertical slice exists, use it as the primary implementation
template. If none exists yet, use the active package specification, SRS, API contract, database
baseline, and project conventions rather than inventing conventions from nonexistent code.

## Required context order

1. Read `AGENTS.md`, `CONTRIBUTING.md`, and any nested `AGENTS.md` on the target path. Read
   `.agents/repo-contract.yml` only when the task needs machine-readable policy detail not
   already available in `AGENTS.md`.
2. Read `app/mamxanh-backend/README.md`, the Maven `pom.xml`, and the mandatory Backend package
   structure proposal.
3. Confirm the FR/BR/NFR in the current SRS registry and read only the relevant detailed
   requirements, API contract, architecture section, and `database/README.md`.
4. Find the nearest implemented Controller -> Service -> Repository -> Entity/DTO slice and
   its tests when one exists. Follow its exception, response, validation, authorization, naming,
   and query conventions before designing a new slice. If no comparable slice exists yet, fall
   back to the approved Backend Package Structure Specification and authoritative contract
   documents.
5. Inspect existing migrations and model constraints only when the Issue needs persistence.

## Implementation principles

- Keep business rules in the appropriate service boundary, not duplicated in Controller or
  Frontend. Validate request shape at the boundary and enforce authorization server-side.
- Keep HTTP request/response DTOs separate from persistence entities at the REST boundary. Never
  return JPA entities directly to clients. Reuse the existing DTO and mapping conventions when
  they exist; if no comparable slice exists yet, follow the approved Backend Package Structure
  Specification.
- Preserve the existing HTTP success and error contract defined by OpenAPI and the active
  Backend Package Structure Specification. Do not introduce a generic response envelope unless
  the contract explicitly requires one. Single-resource responses normally return the defined
  response DTO directly; paginated lists use the project's `PageResponse<T>` convention where
  applicable; errors follow the centralized `GlobalExceptionHandler` contract. Inspect generated
  `/v3/api-docs` (or the same-commit CI artifact) before implementing/reviewing API behavior.
  Keep implementation annotations and DTOs accurate so generated OpenAPI represents runtime.
  Update `docs/api/API.md` when shared integration conventions change. During migration,
  `docs/api/openapi.yaml` is a planned/reference contract only for endpoints not yet implemented;
  do not maintain it as a duplicate authority for runtime endpoints.
- Choose transaction boundaries deliberately around an atomic business operation. Consider
  duplicate requests, retries, idempotency, concurrent updates, and partial external failure
  when the operation requires them; do not add infrastructure without evidence.
- Model JPA relationships, fetch behavior, cascade/orphan semantics, nullability, indexes,
  and query shape intentionally. Avoid N+1 queries, unbounded lists, entity graph surprises,
  and schema auto-update as a substitute for migration history.
- Pagination, filtering, and search must have bounded inputs and explicit ordering. Preserve
  SQL Server compatibility and use the repository's existing query style.
- Flyway migrations are append-only under
  `app/mamxanh-backend/src/main/resources/db/migration/`. Keep `database/schema.sql` aligned
  when the project rule requires it, but do not rewrite applied migrations or edit diagrams
  automatically.
- Keep logs useful but redact passwords, tokens, secrets, payment data, personal data, and
  sensitive payloads. Do not return stack traces or internal topology to clients.

## Capability routing

When the workflow identifies a changed scope, state the route before implementation:

`Routing: backend (<changed files or surface>) -> mamxanh-backend-development`

Use this skill for `*.java`, Controller/Service/Repository/Entity/DTO code, backend config,
`*.sql`, Flyway migrations, database access, REST/API contract, authentication/authorization,
and Backend tests. A full-stack change loads this skill together with
`mamxanh-frontend-development`.

## Testing and validation

Select the cheapest evidence that directly proves the changed risk, then widen when practical:

- Business rule or service behavior: focused JUnit/Mockito unit test.
- HTTP status, validation, authorization, serialization, or error contract: Controller/API
  integration test including negative paths.
- JPA mapping, query, transaction, Flyway, or SQL Server behavior: integration test with the
  relevant real/controlled database engine; do not call a mock-backed test database evidence.
- Regression: preserve a test that reproduces the failure before the fix.
- Build and coverage: run the narrowest relevant Maven test first, then the repository's
  Maven build/verify command when practical so JaCoCo evidence is produced where configured.

Run commands from `app/mamxanh-backend/` using the repository's Maven wrapper or documented
Maven command. Report exact pass/fail/skipped/unavailable results. Never infer coverage or
database correctness from compilation alone.

Before handoff, review changed files for API drift, missing validation/authorization, transaction
gaps, migration ordering, SQL Server incompatibility, N+1/unbounded queries, secret leakage,
unrelated refactors, generated artifacts, and missing contract/test evidence.

## Provenance

Adapted from `Unibean9/harness-skills`:

- Upstream path: skills/hs-backend-development/SKILL.md.
- Upstream references consulted: the `architecture`, `api-design`, `authentication`, `security`,
  `testing`, `technologies`, `performance`, `code-quality`, and `debugging` reference documents
  under the upstream Backend skill.
- Upstream shared references consulted: `domain-routing`, `evidence-policy`, and `hard-gate`.

This adaptation intentionally specializes the generic principles to Mâm Xanh's confirmed
Spring/Maven/SQL Server/Flyway baseline and omits upstream workflow skills, installer, hooks,
subagents, DevOps content, runtime configuration, and generic architecture that conflicts with
the repository's modular-monolith and governance documents. Preserve applicable upstream
attribution when materially copying or adapting content.
