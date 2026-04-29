# Pitfalls Research: DevVerdict

**Domain:** Greenfield microservices (Angular + Spring Boot + Kafka + PostgreSQL)  
**Researched:** 2026-04-29  
**Overall confidence:** HIGH

---

## Saga & Event-Driven Pitfalls

### 1.1 Dual Write (Database + Kafka Without Atomicity)

**What goes wrong:** A service updates its PostgreSQL database and then emits a Kafka event as two separate, non-atomic operations. If the application crashes between the commit and the send, or if the send fails, the database state and the event stream diverge. Downstream services never receive the event, causing the saga to hang indefinitely.

**Warning signs:**
- Saga steps that "usually" complete but occasionally stall with no compensating action triggered.
- Database row exists but no corresponding event in Kafka topic (detectable via log correlation).
- Missing events under load when producer buffer fills or broker is briefly unavailable.

**Prevention strategy:**
- Use the **Transactional Outbox pattern**: write events to an `outbox` table in the same database transaction as the business data update. A separate relay process (or Kafka Connect Debezium) polls the outbox table and publishes to Kafka.
- Alternatively, use **Kafka transactions** (`@Transactional("kafkaTransactionManager")`) with `executeInTransaction`, but be aware this only atomically batches Kafka sends—it does not bridge DB + Kafka without ChainedKafkaTransactionManager or careful sequencing.
- If bridging DB + Kafka, make the DB transaction the outer boundary and Kafka send the last step, or accept at-least-once delivery and build idempotent consumers.

**Address in phase:** Core Saga Implementation phase (likely early, when event publishing is first wired).

---

### 1.2 Event Loss Due to Missing Durability Configs

**What goes wrong:** The producer is configured with default or weak durability settings. A broker restart or leader election occurs before the message is replicated, and the in-flight message is lost. In choreography sagas, this means an entire saga step vanishes.

**Warning signs:**
- Intermittent "missing" events with no consumer-side error.
- `acks=1` or `acks=0` in producer configs (check `ProducerConfig.ACKS_CONFIG`).
- Topics created with `replication.factor=1` or `min.insync.replicas=1` in a multi-broker cluster.

**Prevention strategy:**
- Set `acks=all` on every producer (default in Kafka 3.0+, but verify explicitly).
- Configure `min.insync.replicas=2` (or higher) on broker/topic level so `acks=all` actually guarantees replication.
- Set `replication.factor=3` for all saga topics.
- Set `enable.idempotence=true` on producers to prevent duplicates on retries.

**Address in phase:** Kafka Infrastructure Setup phase.

---

### 1.3 Event Ordering Violations

**What goes wrong:** In choreography, services consume events from multiple upstream producers. Without careful partition key design, events related to the same saga instance (e.g., `OrderCreated` then `PaymentProcessed`) land on different partitions and may be processed out of order by a consumer with multiple threads.

**Warning signs:**
- Compensating transactions fire erroneously because a later event was processed before an earlier one.
- State-machine transitions reject valid events because the expected prior state was not yet reached.
- Consumer lag spikes on one partition while others are idle.

**Prevention strategy:**
- Use a **domain-correlated partition key** (e.g., `orderId`) for all events within the same saga so they route to the same partition.
- Ensure consumers use only one concurrent thread per partition (`concurrency <= partitions`) or rely on Kafka's ordering guarantee within a partition.
- If cross-partition ordering is unavoidable, build an event buffer/sorter in the consumer or switch to orchestration.

**Address in phase:** Saga Design & Topic Modeling phase.

---

### 1.4 Circular Dependencies in Choreography

**What goes wrong:** Service A listens to Service B, and Service B listens to Service A (directly or through a chain). An event loops forever, or a failure in A triggers a compensation in B that triggers a compensation in A, causing infinite oscillation.

**Warning signs:**
- Exponential message volume on a topic without corresponding business growth.
- StackOverflow or repeated compensating actions in logs.
- Saga state flipping back and forth between two states.

**Prevention strategy:**
- Maintain a **saga dependency graph** during design. Ban cycles at the architecture review level.
- Include a `causation-id` / `correlation-id` and a visited-services list in event metadata; reject events that have already been processed by the current service.
- Prefer **orchestration** for complex sagas with many steps or unclear ownership boundaries.

**Address in phase:** Architecture & Saga Design phase (before any code is written).

---

### 1.5 Missing Compensating Transaction Design

**What goes wrong:** Developers focus on the "happy path" events and forget to implement compensating transactions for failure scenarios. When a downstream service rejects an action, the saga has no way to undo prior steps, leaving the system in an inconsistent state.

**Warning signs:**
- "PENDING" records that never transition to "APPROVED" or "REJECTED."
- Manual database cleanup required after test failures.
- Business logic assumes all events succeed.

**Prevention strategy:**
- Model every saga as a state machine with explicit failure transitions before writing handlers.
- Every action event must have a corresponding compensating event (e.g., `ReserveCredit` → `ReleaseCredit`).
- Write integration tests that inject failures at each step and verify compensation.

**Address in phase:** Saga Implementation phase (as part of definition of done for each saga).

---

## Kafka Configuration Pitfalls

### 2.1 Default Producer Settings That Cause Data Loss

**What goes wrong:** Relying on Kafka producer defaults (especially pre-3.0) results in `acks=1`, no retries, or `linger.ms=0` which disables batching and hurts throughput. In addition, `max.in.flight.requests.per.connection` > 1 without idempotence can lead to message reordering on retry.

**Warning signs:**
- Message loss during broker rolling restarts.
- Out-of-order messages after transient network errors.
- Low throughput and high CPU despite small payloads.

**Prevention strategy:**
- Explicitly configure:
  - `acks=all`
  - `enable.idempotence=true`
  - `retries=Integer.MAX_VALUE` (or high number)
  - `delivery.timeout.ms` appropriate to your SLA
  - `linger.ms=5` (or higher) for batching—note Kafka 4.0 changed default from 0 to 5, but verify.
  - `max.in.flight.requests.per.connection=5` (safe when idempotence is on)
- Do not blindly copy old config examples from pre-3.0 tutorials.

**Address in phase:** Kafka Infrastructure Setup phase.

---

### 2.2 Consumer Rebalancing Storms

**What goes wrong:** Consumers join or leave the group frequently (e.g., due to auto-scaling, slow processing, or network blips). Each rebalance stops all consumers in the group from processing. In the legacy "eager" rebalance protocol, all partitions are revoked and re-assigned, causing significant processing pauses.

**Warning signs:**
- High `join rate` and `sync rate` metrics for consumer groups.
- Log flooding with "Revoked partitions" and "Rebalance started" messages.
- Consumer lag spikes that correlate with deployment times.
- Processing latency spikes with no corresponding throughput increase.

**Prevention strategy:**
- Use `partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor` (Kafka 2.4+) to enable incremental cooperative rebalancing, reducing the "stop-the-world" effect.
- Alternatively, enable the new consumer group protocol (`group.protocol=consumer` in Kafka 4.0 / Spring Kafka 4.0) for server-driven incremental assignments.
- Keep consumer session timeouts reasonable (`session.timeout.ms=10000`, `heartbeat.interval.ms=3000`)—not too short, not too long.
- Avoid frequent consumer group membership changes; use static membership (`group.instance.id`) if consumers have stable identities.
- Ensure consumer processing time is well under `max.poll.interval.ms`; otherwise the consumer is considered dead and triggers rebalance.

**Address in phase:** Consumer Service Setup phase.

---

### 2.3 Poison Pills (Undeserializable Messages)

**What goes wrong:** A malformed message or a message with an unexpected schema lands on a topic. The consumer's deserializer throws an exception. With naive configuration, this crashes the listener container or causes infinite retry loops, stalling the entire partition.

**Warning signs:**
- One partition lagging infinitely while others progress.
- Repeated `DeserializationException` or `ClassNotFoundException` in logs.
- Consumer offset not advancing on a specific partition.

**Prevention strategy:**
- Wrap deserializers with Spring Kafka's `ErrorHandlingDeserializer`. It catches deserialization errors, stores the exception in headers, and passes a `null` payload to the listener.
- Configure `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` to route poison pills to a DLT (Dead-Letter Topic).
- Use schema validation (e.g., JSON Schema, Avro with Schema Registry) before production.
- Monitor DLT topic volume with alerts.

**Address in phase:** Consumer Service Setup phase.

---

### 2.4 Auto-Commit Causing Duplicate or Lost Processing

**What goes wrong:** With `enable.auto.commit=true` (the default in many older configs), offsets are committed periodically in the background. If processing fails after the offset was committed but before the business action completed, the message is lost. Conversely, if processing succeeds but the commit hasn't happened, the message will be reprocessed.

**Warning signs:**
- Duplicate database rows or idempotency key collisions.
- Skipped messages after consumer crashes.
- Offset commits visible in logs before business logic completes.

**Prevention strategy:**
- Set `enable.auto.commit=false`.
- Use `AckMode.RECORD` or `AckMode.BATCH` in Spring Kafka and acknowledge after successful processing.
- If exactly-once semantics are required, use Kafka transactions with `isolation.level=read_committed` on consumers.

**Address in phase:** Consumer Service Setup phase.

---

### 2.5 "Read Uncommitted" Consumers Seeing Rollback Data

**What goes wrong:** When Kafka transactions are used for exactly-once semantics, consumers with default `isolation.level=read_uncommitted` will see messages from aborted transactions. This can cause consumers to act on data that was never truly committed.

**Warning signs:**
- Consumers process events that "should not exist" according to producer logs.
- Phantom saga steps that never have a corresponding committed database state.
- Inconsistent read-your-writes behavior.

**Prevention strategy:**
- Always set `isolation.level=read_committed` on consumers when producers use transactions.
- Be aware that `read_committed` consumers may have higher latency because they wait for transaction markers.

**Address in phase:** Consumer Service Setup phase.

---

## Spring Boot + Kafka Gotchas

### 3.1 Serialization Mismatch (Producer vs Consumer Types)

**What goes wrong:** The producer serializes with `JsonSerializer` but the consumer uses `StringDeserializer`, or the consumer's `JsonDeserializer` does not trust the package of the payload class. This results in deserialization failures even when the JSON itself is valid.

**Warning signs:**
- `ClassNotFoundException` or `SerializationException` on the consumer.
- Producer sends fine, consumer never receives usable object.
- Type info headers missing because `JsonSerializer.ADD_TYPE_INFO_HEADERS=false` on producer but `USE_TYPE_INFO_HEADERS=true` on consumer.

**Prevention strategy:**
- Define an internal shared event schema module (Maven/Gradle module) containing all DTOs so both producer and consumer use the same class.
- Explicitly configure `JsonDeserializer.TRUSTED_PACKAGES` to include your domain package.
- If using type mappings (different packages on producer and consumer), set `TYPE_MAPPINGS` on both sides.
- Use `ErrorHandlingDeserializer` as the top-level deserializer and delegate to the real one.

**Address in phase:** Service Bootstrap / Shared Contracts phase.

---

### 3.2 Wrong Transaction Boundary (Kafka-Only vs DB+Kafka)

**What goes wrong:** A developer annotates a service method with `@Transactional` expecting it to cover both a database update and a `kafkaTemplate.send()`. By default, Spring Boot's `@Transactional` uses the `DataSourceTransactionManager`, so the Kafka send is **not** part of the transaction. On rollback, the DB changes are undone but the Kafka message may still be published.

**Warning signs:**
- Database transaction rolls back, yet downstream services receive the event and proceed.
- Inconsistent state where compensations are triggered without a corresponding original action in the DB.

**Prevention strategy:**
- Be explicit about which transaction manager is in use. Use `@Transactional("kafkaTransactionManager")` for Kafka-only transactions.
- For coordinated DB + Kafka transactions, use `ChainedKafkaTransactionManager` (or configure `KafkaAwareTransactionManager` with the datasource transaction manager) so that both commit or both roll back. Note: Spring Kafka docs show chaining with a `DataSourceTransactionManager` bean named `dstm`.
- Alternatively, accept eventual consistency and use the **Outbox pattern** instead of distributed transactions.

**Address in phase:** Saga Implementation phase.

---

### 3.3 Listener Container Configuration Ignored

**What goes wrong:** A developer defines a custom `ConcurrentKafkaListenerContainerFactory` bean, but Spring Boot auto-configures its own and the custom one is not used because the bean name doesn't match or generics are too specific.

**Warning signs:**
- Custom error handler or concurrency settings appear to have no effect.
- `@KafkaListener(containerFactory = "...")` references a bean that doesn't exist at runtime.
- Spring Boot logs show it created a default factory despite your bean.

**Prevention strategy:**
- When overriding the factory in Spring Boot, declare the bean with wildcard generics: `KafkaListenerContainerFactory<?>`, or use the exact name `kafkaListenerContainerFactory` if you want it to be the default.
- If defining multiple factories, give them distinct bean names and reference them explicitly in `@KafkaListener`.
- Mark custom factory beans with `@Primary` if they should override the auto-configured one.

**Address in phase:** Service Bootstrap phase.

---

### 3.4 Async Processing Inside `@KafkaListener` Without Proper Error Handling

**What goes wrong:** The listener method returns a `CompletableFuture` or uses `@Async`, but exceptions inside the async pipeline are lost. The container acknowledges the message before the async work finishes, leading to message loss on failure.

**Warning signs:**
- Consumer offset advances but business side effects never occur.
- Unhandled exceptions inside `CompletableFuture` chains with no log output.
- `asyncReturns` configuration used without an `AsyncErrorHandler`.

**Prevention strategy:**
- Avoid async handoffs inside `@KafkaListener` unless using Spring Kafka's explicit async return support with a configured `AsyncErrorHandler`.
- Prefer synchronous processing within the listener and scale via consumer concurrency (`concurrency` property on the container factory) rather than thread explosion inside the listener.
- If async is truly required, ensure the returned future is completed exceptionally on error so the container can retry or send to DLT.

**Address in phase:** Consumer Service Setup phase.

---

### 3.5 Batch Listeners with Mixed Success/Failure

**What goes wrong:** A batch listener receives 50 records, processes 49 successfully, and fails on the 50th. With default settings, the entire batch is reprocessed, causing duplicates for the first 49. Alternatively, if not configured correctly, the error handler doesn't know which record failed.

**Warning signs:**
- Idempotency key collisions on retry.
- Side effects (emails, charges) firing multiple times for the same record.
- `BatchListenerFailedException` not thrown with the correct index.

**Prevention strategy:**
- Use `DefaultBatchErrorHandler` with a `DeadLetterPublishingRecoverer`.
- When a single record in a batch fails, throw `BatchListenerFailedException` with the failed record's index so the framework can route only that record to the DLT and commit the offsets for the successful ones.
- Consider whether batching is actually necessary; for saga events where each message is independent, record-level listeners are simpler and safer.

**Address in phase:** Consumer Service Setup phase.

---

## Database & JPA Issues

### 4.1 Lazy Loading in Kafka Event Listeners

**What goes wrong:** A `@KafkaListener` calls a service that loads an entity, modifies it, and publishes an event. Later in the same logical flow, the listener (or a downstream consumer) tries to access a lazy-loaded collection on that entity outside the original transaction/session. This triggers `LazyInitializationException` because the Hibernate session is closed.

**Warning signs:**
- `org.hibernate.LazyInitializationException: could not initialize proxy` in logs.
- Errors only in production (or integration tests) where the listener runs in a separate thread from the web layer.
- Works fine in unit tests that keep the session open.

**Prevention strategy:**
- Use `OpenEntityManagerInViewFilter` only for the web layer; do not rely on it for Kafka listeners.
- In Kafka listeners, explicitly fetch all needed associations within the `@Transactional` boundary using `JOIN FETCH` in the query or `EntityGraph`.
- Or, detach a fully loaded DTO from the entity before passing it to event publishing logic.
- Avoid passing unmanaged entity proxies across transactional boundaries.

**Address in phase:** Domain Model & Repository Implementation phase.

---

### 4.2 Connection Pool Exhaustion (HikariCP)

**What goes wrong:** Spring Boot defaults HikariCP to 10 connections. Under load, with multiple concurrent Kafka consumer threads, REST controllers, and long-running queries, the pool saturates. Requests block waiting for a connection and eventually time out.

**Warning signs:**
- `HikariPool-1 - Thread starvation or clock leap detected` warnings.
- `SQLTimeoutException: Connection is not available, request timed out after 30000ms`.
- HTTP 504/500 errors under load while CPU and memory are low.
- Kafka consumer lag increasing because listener threads are blocked on DB connections.

**Prevention strategy:**
- Size the pool appropriately: `maximum-pool-size` should be based on `(number of concurrent consumer threads + HTTP worker threads + other async workers) * 1.2`.
- Set `minimum-idle` equal to `maximum-pool-size` for predictable performance (or let Hikari manage it).
- Set `connection-timeout` and `idle-timeout` explicitly; do not rely solely on defaults.
- Monitor `HikariPoolMXBean` metrics (active, idle, waiting) via Micrometer.
- If using many Kafka consumer threads, consider reducing concurrency or increasing pool size rather than defaulting to high concurrency.

**Address in phase:** Service Bootstrap / Performance Tuning phase.

---

### 4.3 Concurrent Average Calculation (Lost Update)

**What goes wrong:** Two Kafka consumers process events that update an average or aggregate value concurrently. Because JPA read-modify-write is not atomic at the application level, one update overwrites the other, losing data. This is especially likely in choreography sagas where multiple services may update the same read-model.

**Warning signs:**
- Aggregate values (averages, counts, sums) that are slightly off under concurrent load.
- Race-condition bugs that disappear when debugging with a single thread.
- Optimistic locking exceptions if `@Version` is used, but without a retry strategy the update simply fails.

**Prevention strategy:**
- Use **database-native atomic operations** where possible (`UPDATE table SET sum = sum + ?, count = count + 1 WHERE id = ?`).
- Use **optimistic locking** (`@Version`) on the aggregate root and implement a retry loop (e.g., Spring Retry `@Retryable`) for `OptimisticLockingFailureException`.
- For complex aggregates, consider event sourcing or using a dedicated materialized view updated by a single-threaded consumer.
- Avoid read-modify-write in application code for hot aggregates.

**Address in phase:** Domain Model & Saga Implementation phase.

---

### 4.4 Long-Running Transactions Blocking Kafka Consumer

**What goes wrong:** A `@KafkaListener` annotated with `@Transactional` performs slow database operations. The transaction stays open while the consumer holds the Kafka poll loop. If processing exceeds `max.poll.interval.ms`, Kafka revokes the consumer's partitions, triggering a rebalance and possibly duplicate processing.

**Warning signs:**
- Consumer rebalances coinciding with slow database queries.
- `max.poll.interval.ms` exceeded logs in Kafka client.
- Duplicate processing of the same records after rebalance.

**Prevention strategy:**
- Keep listener transactions short. Do heavy lifting asynchronously or break it into smaller steps.
- If long processing is unavoidable, pause the consumer (`consumer.pause()`) while working, or use a separate worker thread pool that acknowledges manually after completion.
- Tune `max.poll.interval.ms` to be larger than your maximum expected processing time, but prefer fixing the processing time instead.

**Address in phase:** Consumer Service Setup phase.

---

## Gateway & CORS Pitfalls

### 5.1 Preflight Requests Rejected or Routed to Downstream

**What goes wrong:** Spring Cloud Gateway handles CORS at the gateway layer, but if routes are configured with `StripPrefix` or `RewritePath`, an `OPTIONS` preflight request may be rewritten and forwarded to a downstream service that doesn't handle it, resulting in a 404 or 403.

**Warning signs:**
- Browser console shows `CORS error` on POST/PUT requests while GET works.
- `OPTIONS` requests returning 404 from downstream services.
- Intermittent CORS failures depending on which gateway instance handles the request.

**Prevention strategy:**
- Configure **global CORS** in `application.yml` at the gateway level so preflight is handled entirely by the gateway and never proxied downstream:
  ```yaml
  spring:
    cloud:
      gateway:
        globalcors:
          cors-configurations:
            '[/**]':
              allowedOrigins: "http://localhost:4200"
              allowedMethods: "*"
              allowedHeaders: "*"
              allowCredentials: true
  ```
- If downstream services also add CORS headers, use the `DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin` filter to avoid duplicate headers.
- Do not rely on downstream microservices for CORS; centralize it at the gateway.

**Address in phase:** Gateway Configuration phase.

---

### 5.2 Route Ordering Causing Wrong Target

**What goes wrong:** More specific routes (e.g., `/api/orders/special`) are defined after generic catch-all routes (e.g., `/api/orders/**`). The generic route matches first and proxies the request to the wrong service.

**Warning signs:**
- Specific endpoints return generic service responses or 404s from the wrong downstream.
- `/actuator/gateway/routes` shows the generic route ranking higher.

**Prevention strategy:**
- Define routes from **most specific to least specific** in YAML/Java config.
- Use explicit `order` property on routes if programmatic configuration is used.
- Regularly inspect `/actuator/gateway/routes` during development to verify matching order.

**Address in phase:** Gateway Configuration phase.

---

### 5.3 Default / Missing Timeouts Causing Cascading Latency

**What goes wrong:** Spring Cloud Gateway defaults to no response timeout for Netty (or very long ones). If a downstream microservice hangs, the gateway thread is held indefinitely, exhausting Netty event-loop resources and causing cascading failures.

**Warning signs:**
- Gateway becomes unresponsive while downstream services show normal CPU.
- HTTP 504 Gateway Timeout only after very long waits (or never).
- Reactor Netty `io.netty.channel.ConnectTimeoutException` buried in logs.

**Prevention strategy:**
- Set **global timeouts**:
  ```yaml
  spring:
    cloud:
      gateway:
        httpclient:
          connect-timeout: 2000
          response-timeout: 10s
  ```
- Set **per-route timeouts** for services with different SLAs.
- Combine with **CircuitBreaker** filter (Resilience4J) so that timeouts trip the breaker and subsequent requests fail fast.
- Never rely on default infinite timeouts in production.

**Address in phase:** Gateway Configuration phase.

---

### 5.4 Missing Circuit Breaker Configuration

**What goes wrong:** A downstream service failure causes every request to block until timeout. Without a circuit breaker, the gateway continues hammering the failing service, wasting resources and preventing recovery.

**Warning signs:**
- All routes slow down when one downstream service fails.
- Thread dumps show many threads waiting on the same downstream connection.
- No fallback responses; users see generic 500s or long loading spinners.

**Prevention strategy:**
- Add `CircuitBreaker` filter to every route, with sensible failure-rate thresholds and slow-call thresholds.
- Provide a `fallbackUri` (e.g., `forward:/fallback`) that returns a degraded response rather than propagating the exception.
- Use `FallbackHeaders` to pass exception info to the fallback controller if needed.

**Address in phase:** Gateway Configuration / Resiliency phase.

---

## Frontend (Angular) Pitfalls

### 6.1 Unhandled Service-Down Errors in HTTP Client

**What goes wrong:** Angular's `HttpClient` throws network errors when a microservice is unreachable (gateway timeout, connection refused). Without explicit error handling, these bubble to the global `ErrorHandler`, resulting in a generic console error and a frozen UI.

**Warning signs:**
- White screen or unresponsive buttons after backend downtime.
- `HttpErrorResponse` with status 0 or 504 in console, uncaught.
- Users cannot retry or understand what failed.

**Prevention strategy:**
- Use Angular's `resource()` (v19+) or `HttpClient` with `catchError` RxJS operator to handle errors at the service layer.
- Provide user-friendly error states: show retry buttons, toast notifications, or degraded UI.
- Distinguish between client errors (4xx) and server/network errors (5xx/0) to show appropriate messaging.
- Implement a global `ErrorHandler` only for fatal/unexpected errors; handle expected HTTP errors locally.

**Address in phase:** Angular Feature Implementation phase.

---

### 6.2 Signals: Unintended Dependency Tracking in Async Code

**What goes wrong:** A developer reads a Signal inside an `async` function after an `await`. Because the reactive context is lost across async boundaries, the signal read is not tracked, and the UI does not update when the signal changes.

**Warning signs:**
- `computed()` or `effect()` does not re-run when an awaited signal changes.
- UI state is stale after async operations complete.
- Works synchronously, breaks when `fetch` or `setTimeout` is introduced.

**Prevention strategy:**
- Read all signals **before** the first `await` in async effects.
- Pass signal values as arguments to async functions rather than reading them inside callbacks.
- Use `resource()` for async data fetching; it handles the signal-reactive async boundary correctly.
- Use `untracked()` to deliberately opt out of tracking for incidental signal reads.

**Address in phase:** Angular Feature Implementation phase.

---

### 6.3 Mutable State Inside Writable Signals

**What goes wrong:** A developer places a mutable object (e.g., array or plain object) inside a `signal()` and mutates it directly. Since Signals use referential equality by default (`Object.is`), the mutation does not trigger change detection, and the UI remains stale.

**Warning signs:**
- `signal.set(...)` or `.update()` was called but the view does not refresh.
- Direct array pushes or object property assignments on signal values.
- Stale lists or forms that only update on the next unrelated change.

**Prevention strategy:**
- Always treat signal values as immutable. Use `.update(arr => [...arr, newItem])` instead of `arr.push(newItem)`.
- Provide a custom equality function only when deeply comparing large objects; prefer immutable updates.
- Use `linkedSignal` for dependent state that should reset when its source changes.

**Address in phase:** Angular Feature Implementation phase.

---

### 6.4 Over-Fetching Due to Lack of Frontend Aggregation

**What goes wrong:** In a microservices architecture, the Angular frontend makes separate HTTP calls to multiple backend services for a single screen. This increases load times, multiplies failure points, and complicates error handling.

**Warning signs:**
- A single page triggers 5+ network requests on load.
- Multiple spinners or sequential loading waterfalls.
- Complex error handling when any one of many calls fails.

**Prevention strategy:**
- Use the **Backend-for-Frontend (BFF) pattern** or dedicated aggregation endpoints behind the gateway.
- Alternatively, design gateway routes that aggregate via `CacheRequestBody` and parallel downstream calls if needed.
- Cache stable reference data in Angular signals or `localStorage` to reduce repeated fetches.

**Address in phase:** API Design / Gateway Configuration phase.

---

## Docker Compose Pitfalls

### 7.1 Ignoring Startup Order (Database Not Ready)

**What goes wrong:** `depends_on` only ensures container start order, not readiness. A Spring Boot service starts before PostgreSQL has finished initializing, causing connection failures and immediate container exit. On restart, the same race condition may or may not occur.

**Warning signs:**
- `Connection refused` or `FATAL: the database system is starting up` in Spring Boot logs on `docker compose up`.
- Service restarts multiple times before finally connecting.
- Integration tests fail intermittently in CI.

**Prevention strategy:**
- Use `depends_on` with `condition: service_healthy` (Compose v3+ with compatibility or v2 file format).
- Define a proper `healthcheck` for PostgreSQL:
  ```yaml
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s
  ```
- For Kafka, use a healthcheck that verifies broker readiness (e.g., `kafka-broker-api-versions.sh`) before starting producers/consumers.
- Alternatively, implement retry-with-backoff in Spring Boot (`spring.datasource.continue-on-error=false` is default; use a connection retry library or simply rely on Docker restart policy combined with healthchecks).

**Address in phase:** Local Development Environment Setup phase.

---

### 7.2 Missing Healthchecks on Custom Services

**What goes wrong:** Docker Compose has no way to know if a Spring Boot service is actually ready to serve traffic. Downstream services start immediately after the container launches, hitting endpoints that return 404 or connection refused.

**Warning signs:**
- Gateway routes to services that return 503 or connection refused shortly after startup.
- Angular frontend loads before the gateway is fully up, showing CORS or connection errors on first refresh.
- Kafka Connect or schema registry not fully initialized when producers start.

**Prevention strategy:**
- Expose Spring Boot Actuator and use it for healthchecks:
  ```yaml
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 60s
  ```
- Chain `depends_on` conditions so that the gateway depends on all backend services being healthy, and the frontend build depends on the gateway.

**Address in phase:** Local Development Environment Setup phase.

---

### 7.3 Volume Persistence Conflicts

**What goes wrong:** Named volumes are reused across `docker compose down` and `docker compose up`. Stale data, old schemas, or corrupted Kafka log segments from previous runs cause confusing behavior that looks like application bugs.

**Warning signs:**
- "This record already exists" errors on fresh app startup.
- Kafka consumer group offsets persisted from previous runs causing new code to skip messages.
- PostgreSQL tables from old migrations conflicting with new Flyway scripts.

**Prevention strategy:**
- Use `docker compose down -v` when you intend a clean slate, but document this clearly for the team.
- For local dev, consider ephemeral volumes or `tmpfs` for Kafka and Postgres if durability across restarts is not required.
- Version your Flyway/Liquibase scripts so old data is forward-compatible; never modify already-applied migrations.
- Reset Kafka consumer groups explicitly (`kafka-consumer-groups.sh --reset-offsets`) when consumer logic changes significantly.

**Address in phase:** Local Development Environment Setup phase.

---

### 7.4 Hardcoded Ports and Hostnames Breaking in Teams

**What goes wrong:** `application.yml` or Angular `environment.ts` contains `localhost:8080` for services. When a teammate runs the stack, or when services are deployed to Kubernetes, these assumptions break.

**Warning signs:**
- "It works on my machine" but fails for teammates.
- Services cannot find each other when Docker networks change.
- Angular tries to call `localhost:8080` from the browser where the gateway is on a different host.

**Prevention strategy:**
- Use Docker Compose service names as hostnames (e.g., `http://gateway:8080` for inter-service communication).
- For Angular, ensure all API calls go through the **same origin** or a configured proxy in `angular.json` during development; in production, use relative URLs or environment-driven configuration.
- Externalize all URLs to environment variables or Spring Cloud Config; never hardcode in source.

**Address in phase:** Local Development Environment Setup phase.

---

### 7.5 Kafka Data Loss on Container Restart

**What goes wrong:** Kafka is run in Docker without persistent volumes for its log directories. On `docker compose restart` or host reboot, all topics and messages are lost, breaking saga state and causing consumers to reprocess from scratch.

**Warning signs:**
- Topics disappear after `docker compose restart`.
- Consumer `auto.offset.reset=earliest` reprocesses everything because offsets were lost.
- ZooKeeper (or KRaft) metadata out of sync with broker state.

**Prevention strategy:**
- Mount named volumes for Kafka log directories (`/var/lib/kafka/data`).
- If using KRaft (Kafka 3.3+), also persist the metadata log (`/var/lib/kafka/data/meta.properties`).
- Set `KAFKA_LOG_RETENTION_HOURS` and `KAFKA_LOG_RETENTION_BYTES` appropriate to your disk size, but do not rely on container ephemeral storage.
- For local dev, decide explicitly whether message durability across restarts is required; if not, document how to re-seed data.

**Address in phase:** Local Development Environment Setup phase.

---

## Phase Mapping

| Pitfall | Recommended Phase | Rationale |
|---------|-------------------|-----------|
| Dual Write (Outbox needed) | **Phase 2: Saga Core** | Must be designed before any saga publishes events. |
| Event Loss (Kafka durability) | **Phase 1: Infrastructure** | Topic and producer configs are foundational. |
| Event Ordering | **Phase 2: Saga Design** | Partition key strategy is part of topic modeling. |
| Circular Dependencies | **Phase 0: Architecture** | Caught during service-boundary design, before coding. |
| Missing Compensations | **Phase 2: Saga Core** | Each saga step DOD must include failure paths. |
| Default Producer Settings | **Phase 1: Infrastructure** | Part of initial Kafka provisioning and review. |
| Consumer Rebalancing | **Phase 3: Consumer Services** | Affects every service with a `@KafkaListener`. |
| Poison Pills | **Phase 3: Consumer Services** | Error handling and DLT wiring is consumer-level. |
| Auto-Commit | **Phase 3: Consumer Services** | Part of listener container factory setup. |
| Read Uncommitted | **Phase 3: Consumer Services** | Consumer property `isolation.level`. |
| Serialization Mismatch | **Phase 1: Contracts / Phase 3: Consumers** | Shared schema module + consumer config. |
| Wrong Transaction Boundary | **Phase 2: Saga Core** | Critical when implementing DB + event atomicity. |
| Listener Container Ignored | **Phase 3: Consumer Services** | Spring Boot bean configuration issue. |
| Async Listener Errors | **Phase 3: Consumer Services** | Listener implementation detail. |
| Batch Listener Retry | **Phase 3: Consumer Services** | Only if batching is chosen. |
| Lazy Loading in Listeners | **Phase 4: Domain Layer** | JPA fetch strategy and transaction design. |
| Connection Pool Exhaustion | **Phase 3: Service Bootstrap** | HikariCP config in `application.yml`. |
| Concurrent Average Calculation | **Phase 4: Domain Layer** | Aggregate design and locking strategy. |
| Long Transactions Blocking Consumer | **Phase 3: Consumer Services** | Listener method design. |
| Preflight / CORS | **Phase 5: Gateway** | Centralized gateway configuration. |
| Route Ordering | **Phase 5: Gateway** | Route definition arrangement. |
| Missing Timeouts | **Phase 5: Gateway** | Resiliency configuration. |
| Missing Circuit Breaker | **Phase 5: Gateway** | Operational safety net. |
| Angular Service-Down Errors | **Phase 6: Frontend** | HTTP client and UI state design. |
| Signals Async Context Loss | **Phase 6: Frontend** | Angular Signals usage patterns. |
| Signals Mutable State | **Phase 6: Frontend** | State management discipline. |
| Over-Fetching | **Phase 5: Gateway / Phase 6: Frontend** | API aggregation or BFF decision. |
| Startup Order Race | **Phase 0: DevEx / Phase 1: Infra** | Docker Compose healthchecks. |
| Missing Healthchecks | **Phase 0: DevEx / Phase 1: Infra** | Service readiness detection. |
| Volume Persistence Conflicts | **Phase 0: DevEx** | Local environment consistency. |
| Hardcoded Ports/Hosts | **Phase 0: DevEx** | Team-wide reproducibility. |
| Kafka Data Loss on Restart | **Phase 1: Infrastructure** | Docker volume configuration for Kafka. |

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Saga & Event-Driven | HIGH | Verified against Microservices.io patterns and Spring Kafka transaction docs. |
| Kafka Configuration | HIGH | Verified against Apache Kafka 4.0 docs and Spring Kafka 4.0 reference. |
| Spring Boot + Kafka | HIGH | Verified against Spring Kafka 4.0 reference docs and code snippets. |
| Database & JPA | HIGH | Based on well-established Hibernate/Spring Data JPA patterns. |
| Gateway & CORS | HIGH | Verified against Spring Cloud Gateway 4.0.9 reference docs. |
| Frontend (Angular) | HIGH | Verified against Angular v21 official docs on Signals and error handling. |
| Docker Compose | HIGH | Verified against Docker Compose official docs on startup order and healthchecks. |

## Sources

- [Microservices.io — Saga Pattern](https://microservices.io/patterns/data/saga.html) (HIGH)
- [Apache Kafka Documentation — Replication & Durability](https://github.com/apache/kafka/blob/trunk/docs/design/design.md) (HIGH)
- [Spring for Apache Kafka 4.0.5 Reference — Serialization, Error Handling, Rebalancing](https://docs.spring.io/spring-kafka/reference/) (HIGH)
- [Spring Cloud Gateway 4.0.9 Reference — CORS, Timeouts, Circuit Breaker](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/) (HIGH)
- [Angular v21 Docs — Signals, Error Handling](https://angular.dev/guide/signals) (HIGH)
- [Docker Compose Docs — Control startup and shutdown order](https://docs.docker.com/compose/startup-order/) (HIGH)
- [Context7 — Apache Kafka, Spring Kafka, Spring Boot](https://context7.com/) (HIGH)
