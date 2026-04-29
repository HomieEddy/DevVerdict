# Research Summary: DevVerdict

**Synthesized:** 2026-04-29  
**Sources:** STACK.md | FEATURES.md | ARCHITECTURE.md | PITFALLS.md

---

## Stack Summary

| Layer | Technology | Version | Key Rationale |
|-------|-----------|---------|---------------|
| **Backend Runtime** | Java (Eclipse Temurin) | 21 LTS | Minimum for Spring Boot 3.x; virtual threads + pattern matching |
| **Backend Framework** | Spring Boot | 3.5.14 | Latest stable 3.5 patch; maps to Spring Cloud 2025.0.x. Boot 4.0.x deferred to later milestone |
| **Cloud/Gateway** | Spring Cloud / Gateway | 2025.0.2 / 4.3.4 | Verified compatibility matrix. Use **new** artifact `spring-cloud-starter-gateway-server-webflux` |
| **Frontend** | Angular | 21.2.11 | Native Signals + standalone components; LTS until 2027-05 |
| **Frontend Runtime** | Node.js | 22.14.x LTS | Angular 21 requires `^20.19 \|\| ^22.12 \|\| ^24.0` |
| **Language** | TypeScript | 5.9.x | Angular 21 requirement |
| **Database** | PostgreSQL | 17.9 | Current stable; 18.x deferred unless specific feature needed |
| **Message Broker** | Apache Kafka | 3.9.2 | Broker version aligns with Spring Boot 3.5.x managed kafka-clients 3.8.x. **KRaft mode** (no Zookeeper) |
| **Testing (BE)** | JUnit 5 / Mockito / AssertJ / Testcontainers | Managed by Boot | `spring-boot-testcontainers` + `@EmbeddedKafka` for Reviews |
| **Testing (FE)** | Vitest + jsdom | Managed by Angular CLI | Angular 21 default. **Do not use Karma** |
| **Orchestration** | Docker Compose | — | Official images with explicit patch tags; healthchecks required |

### Service Split
- **Service A (Catalog):** `spring-boot-starter-web`, `data-jpa`, `postgresql`, `validation`, `actuator` — **NO Kafka starters**. Read-heavy, simple.
- **Service B (Reviews):** Same as Catalog **plus** `spring-kafka` and `spring-kafka-test`. Event producer.
- **Frontend:** Angular CLI 21 with standalone + Signals defaults, Angular Material 21, RxJS 7.8.

### Anti-Patterns Rejected
- Spring Boot 2.7.x or older (EOL, `javax.*` namespace)
- Kafka 4.0.x broker for initial phase (breaking API removals; version skew risk)
- Zookeeper with Kafka (use KRaft)
- Deprecated `spring-cloud-starter-gateway` artifact
- Karma/Jasmine (Angular deprecated Karma)
- Manually pinning starter versions (rely on Spring Boot BOM)

---

## Feature Priorities

### Must-Have for v1 (Table Stakes)
| Feature | Notes |
|---------|-------|
| **Browse catalog** | Grid/list view; Angular Material `MatGridList` / `MatTable` |
| **View framework details** | Name, category, description, avg rating display |
| **Submit anonymous review** | 1-5 stars + comment; rate limiting + XSS prevention essential |
| **View reviews** | Paginated, sortable (newest / helpful / highest) |
| **Average rating display** | Computed value; updates asynchronously on new review |
| **Search / filter catalog** | Basic text search by name; filter by type (language vs framework) |
| **Responsive UI** | Angular Material responsive by default |
| **Sort reviews** | Backend sort by date, rating, helpfulness |

### Should-Have for v1 (Quick Wins / Differentiators)
| Feature | Notes |
|---------|-------|
| **Structured pros/cons** | Separate fields alongside free-text; low effort, high developer-value |
| **Review helpfulness voting** | Thumbs up/down; anonymous in v1 |
| **Basic search/filter** | SQL `LIKE` sufficient if catalog small |
| **Version-contextual reviews** | `versionUsed` field on review schema |
| **"Would use again" metric** | Binary toggle; adds nuance without complexity |

### Deferred to v2+
- User authentication / accounts
- Admin panel for catalog CRUD
- WebSocket real-time updates
- Use-case tagging (more UI design needed)
- Side-by-side comparison (dedicated UI component)
- Adoption indicators (external API integrations)
- Cloud deployment
- Rich text / markdown editor (XSS surface)
- Review comments / threads (social complexity)
- Photo/video attachments
- ML sentiment analysis
- Multi-language UI localization

---

## Architecture Highlights

### Component Boundaries
```
Angular ──► Gateway:8080 ──► Service A (Catalog) ──► catalog-db
                        └──► Service B (Reviews)  ──► review-db
                                    │
                                    ▼ (produces)
                              Kafka: review-updates
                                    │
                                    ▼ (consumes)
                              Service A (recalculates avg_rating)
```

- **Gateway** is the sole public face; services bind to Docker-internal ports.
- **Database-per-Service:** No shared schema, no cross-service foreign keys.
- **Async Only Between Services:** Catalog never calls Reviews over HTTP. Only integration path is Kafka.

### Kafka Topic Design
| Decision | Value |
|----------|-------|
| Topic | `review-updates` |
| Partitions | `3` (allows future consumer scale-out without migration) |
| Replication | `1` (local dev only; ≥3 in production) |
| Message Key | `frameworkId` (guarantees per-framework ordering) |
| Serialization | JSON (`JsonSerializer` / `JsonDeserializer`) |
| Consumer Group | `catalog-service-group` |
| Producer Config | `acks=all`, `enable.idempotence=true`, `retries=3` |
| Consumer Config | `auto-offset-reset=earliest`, `enable.auto.commit=false` |

### Saga / Event Flow (Review Submission)
1. Angular `POST /api/reviews` → Gateway → Service B
2. Service B: `INSERT` review into `review-db`
3. Service B: `EMIT` `ReviewCreated` event to Kafka (`review-updates`)
4. Service A (consumer): `SELECT AVG(stars) …` → `UPDATE frameworks SET avg_rating = ?`
5. Angular sees new rating on next `GET /frameworks`

### Gateway Configuration
- **Global CORS** at gateway level; never proxy `OPTIONS` downstream.
- Routes use `StripPrefix=2` (`/api/catalog/**` → `/frameworks`, `/api/reviews/**` → `/`)
- Use `lb://` scheme for future scale-out via Docker DNS.
- Set explicit `connect-timeout: 2000` and `response-timeout: 5s`.

### Error Handling & Resilience
- **Non-blocking retries** (`@RetryableTopic`) with exponential backoff.
- **Dead-Letter Topic** (`review-updates-dlt`) for exhausted retries.
- **Idempotent consumer:** check `eventId` against `processed_events` table before recalculating.
- **Kafka down (producer):** Synchronous send + exception acceptable for v1; **Transactional Outbox** recommended for v2.

---

## Critical Warnings

### 1. Dual Write Without Atomicity (DB + Kafka)
**Risk:** Review saved to DB but Kafka event never sent → `avg_rating` stale forever.  
**Mitigation:** For v1, accept synchronous send with exception (Service B returns 503 on failure). For production/v2, implement **Transactional Outbox** pattern.  
**Phase:** Saga Implementation (Phase 2)

### 2. Concurrent Average Calculation (Lost Update)
**Risk:** Two consumers process reviews for the same framework simultaneously; one update overwrites the other.  
**Mitigation:** Use database-native atomic operations (`UPDATE SET sum = sum + ?, count = count + 1`) or optimistic locking (`@Version`) with Spring Retry.  
**Phase:** Domain Model & Saga Implementation (Phase 4)

### 3. Kafka Consumer Rebalancing Storms
**Risk:** Frequent consumer group rebalances halt processing; lag spikes on deploy.  
**Mitigation:** Use `CooperativeStickyAssignor` (Kafka 2.4+), static membership (`group.instance.id`), and ensure processing time stays under `max.poll.interval.ms`.  
**Phase:** Consumer Service Setup (Phase 3)

### 4. Poison Pills (Undeserializable Messages)
**Risk:** One bad message crashes listener container or stalls a partition indefinitely.  
**Mitigation:** Wrap deserializers with `ErrorHandlingDeserializer`; configure `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`.  
**Phase:** Consumer Service Setup (Phase 3)

### 5. CORS Preflight Requests Routed Downstream
**Risk:** `OPTIONS` preflight rewritten by `StripPrefix` and forwarded to downstream → 404/403.  
**Mitigation:** Configure `globalcors` at Gateway level so preflight is handled entirely by Gateway.  
**Phase:** Gateway Configuration (Phase 5)

### 6. Docker Compose Startup Race Conditions
**Risk:** Services start before PostgreSQL/Kafka are ready; connection failures and container restarts.  
**Mitigation:** Use `depends_on` with `condition: service_healthy` and proper `healthcheck` blocks for Postgres (`pg_isready`), Kafka (`kafka-broker-api-versions.sh`), and Spring Boot Actuator (`/actuator/health`).  
**Phase:** Local Development Environment (Phase 0/1)

### 7. Angular Signals Async Context Loss
**Risk:** Signal read inside `async` function after `await` loses reactive tracking; UI stale.  
**Mitigation:** Read all signals **before** first `await`; use `resource()` for async data fetching.  
**Phase:** Angular Feature Implementation (Phase 6)

---

## Open Questions

### Research Conflict: Message Broker Choice
**FEATURES.md** recommends RabbitMQ or Redis Pub/Sub (or Spring `@Async`) for v1 async rating update, citing Kafka operational overhead. **STACK.md** and **ARCHITECTURE.md** commit fully to Kafka 3.9.2 with KRaft, detailed topic design, and Spring Kafka configuration.  
**Resolution needed:** Given the project brief explicitly requires Kafka and the architecture is designed around it, Kafka is the primary path. However, the team should confirm if local Docker Compose complexity is acceptable for v1. If Kafka proves too heavy, a fallback to Spring `@Async` + `@EventListener` in-process should be documented.

### Schema Evolution
No Schema Registry (Avro/JSON Schema) is planned for v1. With JSON serialization, schema mismatches between producer and consumer will only be caught at runtime.  
**Decision needed:** Is a shared Maven/Gradle event module sufficient for v1, or should JSON Schema validation be introduced earlier?

### Rate Limiting for Anonymous Reviews
Anonymous review submission is a v1 requirement but opens spam/abuse vectors.  
**Decision needed:** Implement IP-based rate limiting at Gateway (Resilience4J) or application layer? Is CAPTCHA/honeypot in scope for v1?

### Circuit Breaker Scope
Gateway circuit breakers are noted as optional for v1.  
**Decision needed:** Include basic `CircuitBreaker` filter with fallback URI in v1, or defer entirely to v2? Minimal config is low effort and prevents cascading failures.

### Catalog Seeding Method
No admin panel in v1. Catalog data must be seeded via script.  
**Decision needed:** Use Flyway migration scripts with `INSERT` statements, or a standalone Java seed runner? Seed data must be reproducible across `docker compose down -v` cycles.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack & Versions | **HIGH** | Verified against official release trains, GitHub tags, Docker Hub tags |
| Feature Priorities | **MEDIUM-HIGH** | Based on comparable platforms (StackShare, G2) and standard microservices patterns |
| Architecture Patterns | **HIGH** | Gateway routing, Kafka topic design, and saga flow are well-documented Spring Cloud / Confluent patterns |
| Pitfall Prevention | **HIGH** | Extensively sourced from Microservices.io, Spring Kafka reference, Confluent best practices, and Angular v21 docs |

### Research Gaps
1. **Kafka operational complexity in local dev** — real-world friction not fully quantified; fallback strategy should be documented.
2. **Anonymous rate limiting specifics** — no deep-dive into Resilience4J rate limiter vs bucket4j vs custom filter.
3. **Angular state management** — Signals + `resource()` patterns are new in Angular 21; team ramp-up time may vary.
4. **Database migration strategy** — Flyway vs Liquibase not decided; seed script approach needs confirmation.

---

## Sources (Aggregated)

- Spring Boot / Spring Cloud release notes and compatibility matrices
- Spring Cloud Gateway 4.0.9 reference documentation
- Spring for Apache Kafka reference documentation
- Angular v21 official documentation (Signals, testing, Vitest migration)
- Apache Kafka official documentation (KRaft quickstart, replication, durability)
- Confluent Spring Boot Kafka best practices blog
- Microservices.io (Saga pattern, Transactional Outbox, bounded contexts)
- PostgreSQL Docker Hub tags and official docs
- Docker Compose official documentation (startup order, healthchecks)
- StackShare.io, G2, Capterra feature analysis (MEDIUM confidence)
