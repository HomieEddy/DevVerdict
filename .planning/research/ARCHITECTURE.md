# Architecture Research: DevVerdict

**Researched:** 2025-04-29
**Confidence:** HIGH for Gateway / stack; MEDIUM-HIGH for Kafka resilience (verified against Confluent best-practices and Spring Kafka docs structure)

---

## Component Boundaries

| Component | Owns | API Surface | Must NOT Do |
|-----------|------|-------------|-------------|
| **API Gateway** (Spring Cloud Gateway) | Routing, CORS, load-balancing façade | `localhost:8080` proxy | Contain business logic; access databases directly |
| **Service A – Catalog** | `frameworks` table (PostgreSQL) | `GET /frameworks`<br>`GET /frameworks/{id}` | Write reviews; call Service B synchronously; expose DB directly |
| **Service B – Reviews** | `reviews` table (PostgreSQL) | `POST /reviews`<br>`GET /reviews?frameworkId={id}` | Read or mutate `frameworks` table; bypass Kafka for rating updates |
| **Frontend** (Angular) | Browser UI | Calls Gateway at `:8080` only | Call services directly; store secrets |
| **Kafka Broker** | `review-updates` topic (and retry/DLT topics) | Internal only | Act as a request/response bus (fire-and-forget events only) |

### Boundary Rules
1. **Database-per-Service** – Each service connects exclusively to its own PostgreSQL instance. No shared schema, no cross-service foreign keys.
2. **Async Only Between Services** – Catalog never calls Review service over HTTP. The only integration path is the Kafka topic.
3. **Gateway is the Sole Public Face** – All Angular traffic goes through `localhost:8080`. Services bind to Docker-internal ports.

---

## Data Flow

### 1. Read Flow (Browse Catalog)
```
Angular ──GET /api/catalog/frameworks──► Gateway ──► Service A ──► catalog-db
                                    ◄── JSON list ◄──────────────
```

### 2. Read Flow (View Reviews)
```
Angular ──GET /api/reviews?frameworkId=101──► Gateway ──► Service B ──► review-db
                                          ◄── JSON list ◄───────────
```

### 3. Write Flow (Submit Review → Saga)
```
Angular ──POST /api/reviews──► Gateway ──► Service B
                                         │
                                         ▼
                                    1. INSERT review INTO review-db
                                    2. EMIT event to Kafka `review-updates`
                                         │
                                         ▼
                                    Kafka Broker (append to topic)
                                         │
                                         ▼
                                    Service A (consumer)
                                    3. SELECT AVG(stars) … WHERE framework_id=?
                                    4. UPDATE frameworks SET avg_rating = ?
                                         │
                                         ▼
                                    Angular sees new rating on next GET /frameworks
```

### Event Payload
Service B publishes a JSON record keyed by `frameworkId`:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "frameworkId": 101,
  "reviewId": 42,
  "newStars": 5,
  "timestamp": "2025-04-29T12:34:56Z"
}
```
- `eventId` enables idempotency on the consumer side.
- `frameworkId` used as Kafka **message key** guarantees ordering per framework.

---

## Database Schema

### Service A – `catalog-db`

```sql
CREATE TABLE frameworks (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    category    VARCHAR(50)  NOT NULL,
    description TEXT,
    avg_rating  NUMERIC(2,1) DEFAULT 0.0 CHECK (avg_rating BETWEEN 0 AND 5),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_frameworks_category ON frameworks(category);
```

**Notes**
- `avg_rating` is a cached/derived value updated by the Kafka consumer. It is **not** the source of truth for individual reviews.
- Index on `category` supports future filtering without table scans.

### Service B – `review-db`

```sql
CREATE TABLE reviews (
    id           BIGSERIAL PRIMARY KEY,
    framework_id BIGINT       NOT NULL,
    user_name    VARCHAR(100),          -- anonymous for v1
    comment      TEXT,
    stars        SMALLINT     NOT NULL CHECK (stars BETWEEN 1 AND 5),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_framework_id ON reviews(framework_id);
```

**Notes**
- No foreign key to `frameworks` because the table lives in a different service/database.
- `framework_id` is indexed because the dominant query pattern is `GET /reviews?frameworkId=X`.

---

## Kafka Design

| Decision | Recommendation | Rationale |
|----------|----------------|-----------|
| **Topic name** | `review-updates` | Matches project brief; descriptive enough for v1 |
| **Partitions** | `3` | Single partition works for v1, but 3 allows horizontal scaling of the Catalog consumer without a topic migration later |
| **Replication** | `1` (Docker Compose local) | Acceptable for local dev; must be ≥ 3 in any real environment |
| **Message key** | `frameworkId` (String) | Ensures all updates for a given framework land in the same partition, preserving per-framework ordering |
| **Consumer group** | `catalog-service-group` | Explicit, service-scoped group ID |
| **Serialization** | JSON (`JsonSerializer` / `JsonDeserializer`) | Human-readable, easy debugging with `kafka-console-consumer`; no Schema Registry needed for v1 |
| **Auto-create topics** | `false` | Topics should be declared via `NewTopic` beans or Docker init to avoid surprises |

### Spring Kafka Producer (Service B)
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                 # durability guarantee
      retries: 3
      enable-idempotence: true  # exactly-once semantics for producer
```

### Spring Kafka Consumer (Service A)
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: catalog-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.devverdict.catalog.event"
```

### Retry & Dead-Letter Strategy
Use **non-blocking retries** (`@RetryableTopic`) so a poison message for one framework does not block the whole partition.

```java
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2),
    include = {IllegalStateException.class},
    topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX
)
@KafkaListener(topics = "review-updates", groupId = "catalog-service-group")
public void onReviewUpdated(ReviewUpdateEvent event) {
    // recalc average, update frameworks table
}
```
- After 3 attempts the message lands on `review-updates-dlt`.
- A DLT listener can log/monitor without crashing the main consumer.

---

## Gateway Configuration

Spring Cloud Gateway is **non-servlet** (Netty/WebFlux). Route via `application.yml`.

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      # ── Global CORS for Angular dev server ──
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:4200"
            allowedMethods:
              - GET
              - POST
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: false
        add-to-simple-url-handler-mapping: true

      # ── Routes ──
      routes:
        - id: catalog-service
          uri: lb://catalog-service
          predicates:
            - Path=/api/catalog/**
          filters:
            - StripPrefix=2   # /api/catalog/frameworks → /frameworks

        - id: review-service
          uri: lb://review-service
          predicates:
            - Path=/api/reviews/**
          filters:
            - StripPrefix=2   # /api/reviews → /

      # Optional: default timeouts
      httpclient:
        connect-timeout: 2000
        response-timeout: 5s

# Static list for Spring Cloud LoadBalancer (no Eureka needed for v1)
spring.cloud.loadbalancer.configurations: default
```

### Load Balancing
- Even with a single container per service, use the `lb://` scheme.
- Spring Cloud LoadBalancer resolves service names via Docker’s embedded DNS when running in Compose.
- Enables future scale-out (e.g., `docker compose up --scale catalog-service=2`) without changing gateway config.

### Resilience (Optional but Recommended)
Add `spring-cloud-starter-circuitbreaker-reactor-resilience4j` and a fallback route:
```yaml
filters:
  - name: CircuitBreaker
    args:
      name: catalogCb
      fallbackUri: forward:/fallback/catalog
```
For v1 a simple 504/503 response is acceptable; circuit breakers are a stretch enhancement.

---

## Build Order

### Dependency Graph
```
Kafka ──► Service A
   │         │
   │         ▼
   └────► Service B
            │
            ▼
         Gateway ◄── Frontend
```

### Recommended Docker Compose Startup Order
1. **Infrastructure**
   - `zookeeper` (or KRaft controller)
   - `kafka`
   - `catalog-db`
   - `review-db`
2. **Service A (Catalog)**
   - `depends_on` Kafka & catalog-db
   - Runs flyway/seed script on startup
3. **Service B (Reviews)**
   - `depends_on` Kafka & review-db
4. **Gateway**
   - `depends_on` Service A & Service B (healthcheck or `service_started`)
5. **Frontend**
   - `depends_on` Gateway (or independent; just needs port 8080 reachable)

**Tip:** Use `healthcheck` blocks in Docker Compose so services wait for their dependencies to be *ready*, not just *started*.

---

## Error Handling & Resilience

### 1. Kafka Is Down (Producer Side – Service B)
**Scenario:** Review is saved to `review-db`, but `KafkaTemplate.send()` fails because the broker is unreachable.

**Risk:** Catalog never receives the event; `avg_rating` is stale.

**Mitigation Options**
| Approach | Complexity | Recommendation |
|----------|------------|----------------|
| **Synchronous send + exception** | Low | `kafkaTemplate.send(...).get()` (or callback) throws; Service B returns HTTP 503. Review is saved but event is lost. **Acceptable for v1 MVP.** |
| **Transactional Outbox** | Medium-High | Insert event into `outbox` table in same DB transaction as review. Separate relay process polls and publishes to Kafka. **Recommended for production/v2.** |
| **Kafka Transactions** | Medium | `KafkaTransactionManager` gives exactly-once producer semantics, but does **not** atomically commit DB + Kafka. Not a full fix for this split-brain scenario. |

**Research-verified guidance:** The [Transactional Outbox pattern](https://microservices.io/patterns/data/transactional-outbox.html) is the canonical solution for atomically updating a database and publishing events without 2PC.

### 2. Kafka Consumer Failure (Service A)
**Scenario:** A malformed/poison message causes the Catalog consumer to throw repeatedly.

**Mitigation**
- Enable **non-blocking retries** (`@RetryableTopic`) with exponential backoff.
- Route exhausted messages to a **Dead-Letter Topic** (`review-updates-dlt`).
- Implement **idempotency** in the consumer: check `eventId` (or `reviewId`) against a local `processed_events` table before recalculating the average.

```java
// Idempotent consumer pseudo-code
if (eventRepository.existsByEventId(event.getEventId())) {
    return; // already processed
}
// … recalc & update …
eventRepository.save(new ProcessedEvent(event.getEventId()));
```

### 3. Service Is Down
| Failure | Behaviour | Client Impact |
|---------|-----------|---------------|
| **Service B down** | Gateway times out / connection refused | Angular receives 503; show "Unable to submit review" toast |
| **Service A down** | Gateway 503; catalog unreadable | Angular shows skeleton/error state; reviews still readable if Service B is up |
| **Gateway down** | Nothing proxied | Entire UI unreachable; local dev restart required |

### 4. Database Connection Failure
- Spring Boot’s `HikariCP` will retry connections automatically.
- Configure `spring.datasource.hikari.connection-timeout=20000` and `initialization-fail-timeout=-1` to prevent immediate crash on startup race conditions.

### 5. Angular Polling / Refresh Strategy
Because v1 explicitly defers WebSockets, the frontend should:
1. After a successful `POST /reviews`, optimistically update local UI state (show the new review).
2. Re-fetch framework list (or just the single framework) on a short delay (e.g., 1–2 s) or rely on the user refreshing the page.

---

## Sources

- **Spring Cloud Gateway** – Official reference (v4.0.9) for route predicates, `StripPrefix`, CORS, and `ReactiveLoadBalancerClientFilter`  
  <https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/>
- **Microservices.io – Saga pattern** – Choreography definition, benefits, and compensating-transaction caveat  
  <https://microservices.io/patterns/data/saga.html>
- **Microservices.io – Transactional Outbox** – Atomically updating DB + publishing events without 2PC  
  <https://microservices.io/patterns/data/transactional-outbox.html>
- **Confluent – Spring Boot Kafka Best Practices** – Non-blocking retries (`@RetryableTopic`), DLQ, error handlers, idempotence, and exactly-once producer settings  
  <https://www.confluent.io/blog/spring-for-apache-kafka-deep-dive-part-2-apache-kafka-spring-cloud-stream/>
- **Baeldung – Spring Cloud Gateway** – Practical `lb://` load-balancing and routing examples  
  <https://www.baeldung.com/spring-cloud-gateway>
