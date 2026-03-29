# InstituteOps Design (Step 11 Final)

## Architecture overview

InstituteOps is implemented as a modular Spring Boot monolith with clear domain boundaries and shared security/audit infrastructure.

- Runtime stack:
  - Spring Boot 3.3, Java 21, Thymeleaf, Spring Security, Spring Data JPA
  - MySQL 8.4 as primary datastore
  - Flyway for schema migrations and seeded reference data
  - Docker Compose orchestration for app + DB + migration bootstrap
- Deployment shape:
  - `app` container serves both APIs and Thymeleaf web pages
  - `mysql` container stores transactional and analytical data
  - `flyway` runs migrations on startup
  - persistent volumes for DB data and `/uploads`

## Module integration design

The final step keeps all previously delivered modules integrated through shared identity, role-based authorization, and cross-module data flows.

1. Student lifecycle
   - Student and class registration data feeds grade and recommender contexts.
   - Student identity is reused by store campaign purchasing and recommendation retrieval.

2. Grades ledger
   - Rule-versioned grading logic resolves active rulesets and computes deterministic impact.
   - Append-only ledger supports entry/edit/recalculation/manual override operations.

3. Inventory
   - Unit conversion and ingredient modeling normalize stock handling.
   - FIFO issue flow and loss reason validation enforce business controls.
   - Inventory signals feed procurement alerts/recommendations.

4. Procurement
   - Request -> approval -> purchase order -> goods receipt lifecycle implemented with status controls.
   - Uses inventory consumption trends and stock positions for replenishment insights.

5. Store and group-buy campaigns
   - SPU/SKU catalog and campaign windows drive student purchase flows.
   - Group-buy confirmation and per-student purchase limits are enforced in service layer.

6. Recommender
   - Event ingestion, model versioning, rollback, and user-facing recommendation APIs.
   - Store and dashboard surfaces consume recommendation outputs.

## Security and compliance posture

- Spring Security form login + RBAC route guards per functional module.
- Internal API client key/secret authentication for `/api/internal/**`.
- Request audit filter and operation logging retained.
- Existing sensitive-field encryption behavior remains unchanged.

## UI design decisions

- Thymeleaf-first UI with shared shell fragment for consistent navigation and role-aware module entry points.
- Module pages are purpose-built (student, grades, inventory, procurement, store manager/student, recommender admin).
- Progressive enhancement via static JS/CSS while preserving server-rendered baseline and accessibility.

## Testing strategy added in final step

- Backend unit tests for core business rules:
  - grade calculation/ledger deltas
  - inventory FIFO and loss validation
  - procurement status transitions
  - group-buy purchase limits
  - recommender event normalization/rollback
  - security filter credential behavior
- API integration tests (MockMvc) validate representative contracts across major APIs.
- Thymeleaf flow tests (MockMvc web-layer) validate login/dashboard and key module routes.
