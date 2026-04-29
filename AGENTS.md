# DevVerdict — Agent Instructions

This file contains context and workflow guidance for AI agents working on the DevVerdict project.

## Project Context

**DevVerdict** is a centralized platform where developers browse programming languages and frameworks, read community reviews, and view aggregated star ratings to help them choose their next tech stack.

**Core value:** Developers can reliably discover and evaluate tech stacks through honest community reviews and real-time aggregated ratings.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | Angular | 21.2.11 (standalone components, Signals, Angular Material) |
| Service A (Catalog) | Spring Boot | 3.5.14 + PostgreSQL 17.9 + JPA |
| Service B (Review) | Spring Boot | 3.5.14 + PostgreSQL 17.9 + JPA + Spring Kafka |
| Gateway | Spring Cloud Gateway | 4.3.4 (via Spring Cloud 2025.0.2) |
| Messaging | Apache Kafka | 3.9.2 (KRaft mode, no Zookeeper) |
| DevEx | Docker Compose | Local orchestration of all services |
| Testing (BE) | JUnit 5, Mockito, Testcontainers, @EmbeddedKafka |
| Testing (FE) | Vitest + jsdom (Angular 21 default) |

## Architecture Overview

**Choreography-based Saga (simplified):**
- Service B (Review) tells the world "Hey, a review happened" via Kafka
- Service A (Catalog) listens and updates its records

**Data Flow:**
1. Angular → Gateway (localhost:8080) → Service A: GET frameworks
2. Angular → Gateway → Service B: POST review
3. Service B saves review to DB, emits `ReviewCreated` event to Kafka `review-updates`
4. Service A consumes event, recalculates average rating, updates DB
5. Angular reflects updated rating on next page load

**Gateway Routes:**
- `/api/catalog/**` → Service A
- `/api/reviews/**` → Service B

## Git Workflow (MANDATORY)

### Branch-per-Phase
- Each phase is developed on its own branch: `phase/N-short-name`
- Branch from latest `main` at the start of each phase
- Never commit phase work directly to `main`

### Atomic Commits
- Commit after **every file modification**
- Use clear, concise commit messages in present tense
- Example: `feat(catalog): add Framework entity and repository`
- Example: `fix(gateway): correct StripPrefix filter configuration`

### Pull Request Merges
- When a phase is complete, open a PR to `main`
- PR description must summarize:
  - Phase goal
  - Key changes made
  - Requirements covered (REQ-IDs)
  - Success criteria status
- Merge only after PR review / approval

### Planning Artifacts (Local-Only)
- `.planning/` and `.opencode/` directories are in `.gitignore`
- Never commit planning docs to the repository
- Planning artifacts live only in the local working directory

## Constraints & Decisions

- **Anonymous reviews for v1** — No authentication. Rate limiting and validation are the v1 spam defense.
- **Seed script for catalog data** — No admin panel in v1. Use SQL seed script or Flyway migration.
- **Angular Material for UI** — Stick to Material Design defaults. Custom theming can be deferred.
- **Kafka is the message broker** — Do not substitute RabbitMQ or Redis Pub/Sub. KRaft mode, topic `review-updates`.
- **No WebSocket in v1** — Async updates via Kafka + page refresh. WebSocket deferred to v2.
- **Database-per-service** — No shared schema, no cross-service foreign keys.
- **Gateway stays "dumb" in v1** — Route + CORS only. No auth, rate limiting, or circuit breakers in v1.

## Critical Pitfalls to Avoid

1. **Dual write without atomicity** — Service B saves review then emits Kafka event. If Kafka is down, the event is lost. For v1, accept synchronous send with exception. Transactional Outbox is v2.
2. **Concurrent average calculation** — Use atomic SQL update or optimistic locking with retry. Avoid read-modify-write in application code.
3. **CORS preflight routed downstream** — Handle CORS entirely at Gateway level with `globalcors`. Never proxy `OPTIONS` to downstream services.
4. **Docker Compose startup races** — Use `depends_on` with `condition: service_healthy` and proper `healthcheck` blocks.
5. **Angular Signals async context loss** — Read signals before first `await`. Use `resource()` for async data fetching.

## GSD Workflow Commands

- `/gsd-discuss-phase N` — Gather context before planning a phase
- `/gsd-plan-phase N` — Create detailed plan for a phase
- `/gsd-execute-phase N` — Execute a phase plan
- `/gsd-verify-work` — Validate built features through conversational UAT
- `/gsd-code-review` — Review source files for bugs and quality issues

## Current State

- **Milestone:** v1
- **Phase:** Ready to start Phase 1
- **Requirements:** 19 v1 requirements mapped to 7 phases
- **Roadmap:** `.planning/ROADMAP.md`
- **Requirements doc:** `.planning/REQUIREMENTS.md`

---
*Last updated: 2025-04-29 after project initialization*
