# Stack Research: DevVerdict

**Researched:** 2026-04-29
**Confidence:** HIGH for core versions (verified against official release trains and GitHub release tags)

---

## Runtime Versions

| Technology | Version | Purpose | Rationale |
|------------|---------|---------|-----------|
| **Java** | 21 LTS (Eclipse Temurin) | JVM runtime for Spring Boot services | Minimum for Spring Boot 3.x; LTS stability with modern language features (virtual threads, pattern matching). Boot 3.5.x supports Java 17-24, but 21 is the pragmatic LTS sweet spot. |
| **Spring Boot** | 3.5.14 | Core framework for both microservices | Latest stable patch in the 3.5 line (released 2026-04-23). Mature, well-supported, and explicitly mapped to Spring Cloud 2025.0.x (Northfields). Avoid 4.0.x for now—it is stable but less than 6 months old and ecosystem libraries are still catching up. |
| **Spring Cloud** | 2025.0.2 | Release train BOM for Gateway/Netflix/Config | Latest service release (2026-04-02). Verified compatibility matrix: 2025.0.x ↔ Spring Boot 3.5.x. |
| **Spring Cloud Gateway** | 4.3.4 | API Gateway (via Spring Cloud 2025.0.2) | Bundled in 2025.0.2 release train. Use the **new** artifact name: `spring-cloud-starter-gateway-server-webflux`. The old `spring-cloud-starter-gateway` artifact is deprecated as of Gateway 4.3.x. |
| **Angular** | 21.2.11 | Frontend framework | Latest stable patch (released 2026-04-29). Active support window until 2026-05-19, LTS until 2027-05-19. Signals and standalone components are native defaults. |
| **Node.js** | 22.14.x LTS (or 20.19.x+) | Angular build/runtime | Angular 21 requires `^20.19.0 \|\| ^22.12.0 \|\| ^24.0.0`. Node 22 LTS is the recommended target for new projects. |
| **TypeScript** | 5.9.x | Angular compilation | Angular 21.0.x requires `>=5.9.0 <6.0.0`. |
| **RxJS** | 7.8.x | Angular reactive streams | Angular 21 supports `^6.5.3 \|\| ^7.4.0`. Use 7.8.x for latest bug fixes and tree-shaking improvements. |
| **PostgreSQL** | 17.9 | Relational database for both services | Current stable, widely deployed, excellent tool compatibility. PostgreSQL 18.3 is available (`postgres:latest`) but released Feb 2026; defer to 18.x in a future milestone unless a specific 18 feature is required. |
| **Apache Kafka** | 3.9.2 | Event streaming / messaging backbone | Stable release that aligns cleanly with the Kafka client version managed by Spring Boot 3.5.x (3.8.x clients). Kafka 4.0.x/4.1.x brokers are stable but introduce breaking API removals; use 3.9.2 to minimize client/broker version skew. |

---

## Service A (Catalog) Dependencies

Use Spring Boot 3.5.14 starter BOM. Do **not** declare versions for starters managed by the BOM.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.14</version>
</parent>

<dependencies>
    <!-- Web / REST -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Data -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Actuator / Observability -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- DevEx -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Key choices:**
- `spring-boot-starter-web` (Tomcat 10.1.x embedded) — synchronous MVC stack is sufficient for Catalog reads.
- `spring-boot-starter-data-jpa` + Hibernate 6.6.x (managed by Boot) — standard, well-documented.
- `spring-boot-starter-validation` — Jakarta Bean Validation for DTOs.
- `spring-boot-starter-actuator` — health probes and metrics out of the box.
- Do **not** add Kafka starters here; Catalog is a read-heavy service and should remain simple unless event publishing is explicitly required later.

---

## Service B (Review) Dependencies

Same parent BOM. Kafka is first-class here.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.14</version>
</parent>

<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Data -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Validation + Actuator + Devtools (same as Catalog) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Kafka version alignment notes:**
- Spring Boot 3.5.14 manages `spring-kafka` 3.3.15 and `kafka-clients` 3.8.x.
- **Do not override `kafka.version` to 4.0.x unless you have a specific reason.** The 3.8.x client works perfectly against a 3.9.2 broker.
- If you later need Kafka Streams, add `org.apache.kafka:kafka-streams` (version managed by Boot).

---

## Frontend Dependencies

Generate the workspace with Angular CLI 21.2.x (install globally or use `npx`).

```bash
# Global CLI (optional)
npm install -g @angular/cli@21.2.11

# New app with standalone + Signals defaults
ng new devverdict-ui --routing --style=scss --ssr=false
```

**Core dependencies (managed by `package.json`):**

| Package | Version | Purpose |
|---------|---------|---------|
| `@angular/core` | ~21.2.11 | Framework core |
| `@angular/common` | ~21.2.11 | Common directives/pipes |
| `@angular/router` | ~21.2.11 | Client-side routing |
| `@angular/forms` | ~21.2.11 | Template-driven & reactive forms |
| `@angular/platform-browser` | ~21.2.11 | DOM renderer |
| `@angular/platform-browser-dynamic` | ~21.2.11 | JIT bootstrap (dev) |
| `@angular/material` | ~21.2.11 | UI component library |
| `@angular/cdk` | ~21.2.11 | Component Dev Kit (harnesses, a11y, overlays) |
| `rxjs` | ~7.8.0 | Observables for HTTP and async pipes |
| `tslib` | ^2.8.0 | TS runtime helpers |
| `zone.js` | ~0.15.0 | Change detection (still required in Angular 21) |

**Build toolchain (managed by Angular CLI):**
- `@angular/build` (application builder, Vite-based, replaces legacy `@angular-devkit/build-angular` webpack path)
- `typescript` ~5.9.2

---

## Infrastructure (Docker)

Use **Docker Compose** for local orchestration. Prefer official images with explicit patch tags.

```yaml
# docker-compose.yml (excerpt)
services:
  postgres-catalog:
    image: postgres:17.9-alpine
    environment:
      POSTGRES_DB: catalog
      POSTGRES_USER: devverdict
      POSTGRES_PASSWORD: devverdict
    ports:
      - "5432:5432"
    volumes:
      - pg_catalog_data:/var/lib/postgresql/data

  postgres-reviews:
    image: postgres:17.9-alpine
    environment:
      POSTGRES_DB: reviews
      POSTGRES_USER: devverdict
      POSTGRES_PASSWORD: devverdict
    ports:
      - "5433:5432"
    volumes:
      - pg_reviews_data:/var/lib/postgresql/data

  kafka:
    image: apache/kafka:3.9.2
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
```

**Docker image rationale:**
- `postgres:17.9-alpine` — Alpine variant is ~35 MB smaller than Debian-based; patch tag ensures reproducible builds.
- `apache/kafka:3.9.2` — Official ASF image. **Use KRaft mode** (`broker,controller`) — no Zookeeper container required. This is the modern default since Kafka 3.3+.
- No need to containerize the Angular dev server; `ng serve` proxies to the Gateway locally. If CI build artifact containerization is needed later, use `node:22-alpine` as a build stage.

---

## Testing Stack

### Backend (Both Services)

| Library | Version | Source | Purpose |
|---------|---------|--------|---------|
| **JUnit 5 (Jupiter)** | 5.12.x | `spring-boot-starter-test` | Unit & integration test runner |
| **Mockito** | 5.17.x | `spring-boot-starter-test` | Mocking framework |
| **AssertJ** | 3.27.x | `spring-boot-starter-test` | Fluent assertions |
| **Testcontainers** | 2.0.5 | Managed by Boot 3.5.14; `spring-boot-testcontainers` | PostgreSQL & Kafka integration tests |
| **Spring Kafka Test** | 3.3.15 | Managed by Boot 3.5.14 | Embedded Kafka broker for `@EmbeddedKafka` tests |

**Testing patterns:**
- Use `@DataJpaTest` for repository slices (Catalog & Reviews).
- Use `@WebMvcTest` for controller slices.
- Use `@SpringBootTest` + Testcontainers for end-to-end service tests.
- For Reviews service, use `@EmbeddedKafka` (from `spring-kafka-test`) to test consumers without a running Docker broker in unit-test suites.

### Frontend

| Library | Version | Purpose |
|---------|---------|---------|
| **Vitest** | ^3.0.x (managed by `@angular/build`) | Default test runner for Angular 21 CLI projects. Replaces Karma. |
| **jsdom** | ^26.x (managed by CLI) | DOM emulation for Node-based unit tests |
| `@angular/build:unit-test` | ~21.2.11 | Angular CLI builder target |
| **@vitest/browser-playwright** | optional | Browser-mode component tests (headed/headless) |
| **@angular/cdk/testing** | ~21.2.11 | Component harnesses for Material component interaction tests |

**Important:** Angular 21 CLI generates Vitest by default. Do **not** install Karma or Jasmine for new code. If you have legacy Karma tests, migrate using the [Angular Vitest migration guide](https://angular.dev/guide/testing/migrating-to-vitest).

---

## Anti-Recommendations

| Anti-Pattern / Technology | Why Avoid | What To Do Instead |
|---------------------------|-----------|-------------------|
| **Spring Boot 2.7.x or older** | End of life; uses `javax.*` namespace; no Jakarta EE support; incompatible with modern Spring Cloud. | Use Spring Boot 3.5.14 (Jakarta EE 9+ baseline). |
| **Spring Boot 4.0.x (for this phase)** | Released recently (4.0.6, Apr 2026). Stable, but Spring Cloud 2025.1.x (Oakwood) alignment is newer and some third-party starters may lag. | Defer Boot 4.0 migration to a later milestone; start on 3.5.14 for maximum ecosystem maturity. |
| **Angular < 17** | No native Signals; NgModule boilerplate; EOL or near-EOL. | Use Angular 21 with standalone components and Signals. |
| **Karma + Jasmine** | Angular deprecated Karma. CLI no longer generates Karma configs by default in v21. | Use Vitest + jsdom (default). Add Playwright only if browser-mode testing is required. |
| **Zookeeper with Kafka** | Adds unnecessary operational complexity for a single-node local dev setup. | Use KRaft mode (`broker,controller` in one process) via `apache/kafka` official image. |
| **Synchronous HTTP calls between services for event semantics** | Tight coupling, retries, and circuit-breaker complexity that Kafka already solves. | Use Kafka topics for fire-and-forget events (e.g., "review submitted" → downstream aggregation). Use REST only for request/response queries. |
| **Manually pinning starter versions** | Fragile; creates drift with Spring Boot BOM. | Let the `spring-boot-starter-parent` manage all `org.springframework.boot` and `org.springframework.kafka` versions. |
| **Deprecated Spring Cloud Gateway artifacts** | `spring-cloud-starter-gateway` and `spring-cloud-gateway-server` are deprecated in 2025.0.x. | Use `spring-cloud-starter-gateway-server-webflux` and the `spring.cloud.gateway.server.webflux.*` property prefix. |
| **PostgreSQL 16 or older** | Still supported, but 17 has better performance (improved vacuum, JSON optimizations) and longer support horizon. | Use PostgreSQL 17.9 (or 18.3 if you need the absolute latest). |
| **Kafka 4.0.x/4.1.x broker (for initial phase)** | Stable, but removes many deprecated APIs. Spring Boot 3.5.x manages kafka-clients 3.8.x; while backward-compatible, version skew can complicate debugging. | Start with Kafka 3.9.2 broker; upgrade to 4.x in a later milestone after explicit compatibility testing. |

---

## Confidence Levels

| Recommendation | Confidence | Evidence |
|----------------|------------|----------|
| Spring Boot 3.5.14 | **HIGH** | GitHub release tag verified (2026-04-23). Spring Cloud compatibility matrix explicitly lists 2025.0.x ↔ 3.5.x. |
| Spring Cloud 2025.0.2 | **HIGH** | Official release notes wiki verified (2026-04-02). Includes Gateway 4.3.4. |
| Angular 21.2.11 | **HIGH** | GitHub releases page verified (2026-04-29). Angular docs confirm Node 22 + TS 5.9 compatibility. |
| Angular Material 21.2.x | **HIGH** | Material releases are synchronized with Angular core on the same cadence. |
| PostgreSQL 17.9 | **HIGH** | Docker Hub tags verified; official PostgreSQL docs list 17.9 as current stable alongside 18.3. |
| Kafka 3.9.2 | **HIGH** | Docker Hub tags verified; Spring Kafka 3.3.15 docs confirm 3.8.x client compatibility with 3.9.x brokers. |
| Vitest as default test runner | **HIGH** | Angular 21 official testing guide explicitly states Vitest is the default for new CLI projects. |
| KRaft mode (no Zookeeper) | **HIGH** | Official Kafka Docker image README and Kafka 3.3+ docs recommend KRaft for new deployments. |
| Java 21 LTS | **HIGH** | Eclipse Temurin provides stable builds; Spring Boot 3.5.x officially supports Java 21. |
| Testcontainers 2.0.5 | **HIGH** | Listed in Spring Boot 3.5.14 release notes dependency upgrades. |

---

## Sources

- Spring Boot releases: https://github.com/spring-projects/spring-boot/releases (v3.5.14, v4.0.6)
- Spring Cloud release train compatibility: https://spring.io/projects/spring-cloud (table verified 2025.0.x ↔ Boot 3.5.x)
- Spring Cloud 2025.0 release notes: https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.0-Release-Notes (Gateway 4.3.4, artifact renames)
- Spring Kafka dependency override docs: Context7 `/spring-projects/spring-kafka` (Boot 3.5.x uses kafka-clients 3.8.x)
- Angular releases: https://github.com/angular/angular/releases (v21.2.11)
- Angular version compatibility: https://angular.dev/reference/versions (Node 22, TS 5.9, RxJS 7.4)
- Angular testing guide: https://angular.dev/guide/testing (Vitest default)
- PostgreSQL Docker Hub tags: https://hub.docker.com/_/postgres/tags (17.9, 18.3)
- Apache Kafka Docker Hub tags: https://hub.docker.com/r/apache/kafka/tags (3.9.2, 4.0.2, 4.1.2)
- Kafka KRaft quickstart: https://kafka.apache.org/documentation/#quickstart (no Zookeeper)
