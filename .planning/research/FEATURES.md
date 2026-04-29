# Feature Landscape

**Domain:** Developer-focused review & rating platform for programming languages and frameworks
**Researched:** 2026-04-29
**Confidence:** MEDIUM (based on analysis of comparable platforms StackShare, G2, Capterra; microservices patterns from authoritative sources)

---

## Table Stakes

Features users expect from any review/rating platform. Missing these makes the product feel broken or incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Browse catalog** | Core purpose of the platform; users must discover what they can review | Low | Grid/list view with name, type, avg rating. Angular Material `MatGridList` / `MatTable` ideal. |
| **View framework details** | Users need context before trusting reviews (description, category, metadata) | Low | Name, type, description, average rating display. Simple GET endpoint. |
| **Submit anonymous review** | v1 explicit requirement; table stakes for any review system | Low-Med | 1-5 star rating + comment text. Needs input validation (XSS prevention, length limits, profanity filter optional). Rate limiting essential even for anonymous users. |
| **View reviews for a framework** | Users come to read others' opinions; this is the primary content | Low | Paginated list. Sort by newest/helpful/highest. Angular Material `MatPaginator`, `MatSort`. |
| **Average rating display** | Star ratings are the universal summary signal for review platforms | Low | Display aggregated value (e.g., 4.2/5). Can be computed on read in v1; async update is v1 requirement per spec. |
| **Search / filter catalog** | At >20 items, browsing becomes unwieldy; users expect to find specific frameworks quickly | Low-Med | Basic text search by name. Filter by type (language vs framework). Can defer full-text search to v2 if catalog is small initially. |
| **Responsive UI** | Modern web apps must work on mobile and desktop | Low | Angular Material is responsive by default with flex layout. |
| **Sort reviews** | Users want to surface most helpful, newest, or highest-rated reviews | Low | Standard sort controls. Backend sort by date, rating, or "helpful" (if implemented). |

---

## Differentiators

Features that would set DevVerdict apart from generic review platforms. Not expected, but valued by developer audiences.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Structured pros/cons** | Developers think in tradeoffs; unstructured paragraphs bury key insights. Format popularized by StackShare. | Low-Med | Separate fields for "Pros" and "Cons" alongside free-text comment. Makes reviews scannable. Easy to implement in v1 schema. |
| **Use-case tagging** | "Best for startups" vs "Enterprise-grade" matters more than raw stars for developers choosing tools | Low-Med | Predefined tags (e.g., "Startup-friendly", "Enterprise", "Learning", "Side projects", "Production"). Reviewers select applicable tags. Enables filtering. |
| **Side-by-side comparison** | Developers rarely choose one tool in isolation; they compare React vs Vue, Python vs Go | Medium | Select 2-3 frameworks, view specs and ratings side-by-side. Requires dedicated UI component. High value, moderate effort. |
| **Version-contextual reviews** | "This was true in v2 but v3 fixed it" — framework reviews go stale quickly | Medium | Allow reviewers to note version used. Display "Reviewed on version X". Helps readers assess relevance. |
| **Adoption indicators** | Raw star ratings lack context; is this a niche tool or industry standard? | Medium | Pull external signals (GitHub stars, StackOverflow questions, npm downloads) or display "Most reviewed this month". Requires external API integrations or tracking. Defer to v2+. |
| **"Would use again" metric** | More predictive than satisfaction; separates "tolerated it" from "actively choose it" | Low | Simple binary toggle on review form. Adds nuance without complexity. |
| **Review helpfulness voting** | Surface quality reviews without building full moderation | Low | Thumbs up/down per review. Lightweight trust signal. Can be anonymous in v1. |

---

## Anti-Features

Features to explicitly NOT build in v1 — and the rationale for deferral or rejection.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **User authentication / accounts** | Explicitly deferred to v2 per project scope. Anonymous reviews are the v1 mandate. | Build anonymous submission with CAPTCHA or honeypot + IP-based rate limiting to prevent spam. |
| **Admin panel for catalog CRUD** | Deferred to v2. Adds UI complexity, authorization logic, and separate user roles. | Use a database seed script (explicitly in v1 scope) to populate catalog. Direct DB edits for v1 catalog changes. |
| **WebSocket real-time updates** | Deferred to v2. Adds infrastructure complexity (STOMP, SockJS, or raw WS) and client state management. | Use optimistic UI updates on review submission. Async rating recalculation via message queue (RabbitMQ/Kafka) is sufficient for v1. |
| **Cloud deployment** | Deferred to v2. Docker Compose local stack is v1 scope. | Focus on containerized local development. Ensure `docker-compose.yml` is production-ready in structure for future migration. |
| **Rich text / markdown editor for reviews** | Increases XSS surface area and UI complexity. Plain text is sufficient for MVP. | Use `<textarea>` with plain text. Sanitize output with DOMPurify or similar. Consider markdown in v2. |
| **Review comments / threads** | Social features dramatically increase complexity (notifications, threading UI, moderation). | Keep reviews flat. One review per anonymous user session. |
| **Photo/video attachments** | Storage, moderation, and CDN concerns. Not relevant for developer tool reviews. | Text-only reviews in v1. |
| **Paid/sponsored listings** | Monetization is premature for MVP; corrupts trust signals. | Build organic engagement first. |
| **Machine learning sentiment analysis** | Over-engineering for v1. Adds model serving infrastructure, training data needs. | Structured pros/cons fields capture sentiment explicitly and more accurately for this domain. |
| **Multi-language UI localization** | Angular supports i18n, but adds translation overhead and testing surface. | English-only v1. Architect with i18n pipes in mind for v2. |

---

## Complexity & Dependencies

### Feature Dependency Graph

```
Catalog Service (seed data)
    ├── Browse catalog ──→ View framework details
    │                        └── View reviews
    │                            └── Submit anonymous review ──→ Async rating update
    │                                                               └── Average rating display (refreshed)
    └── Search/filter catalog

API Gateway
    └── Routes all traffic to above services

Angular Material UI
    └── Consumes all Gateway endpoints
```

### Complexity Matrix

| Feature | Frontend | Backend | Data | Infrastructure | Overall |
|---------|----------|---------|------|----------------|---------|
| Browse catalog | Low | Low | Low | Low | **Low** |
| View framework details | Low | Low | Low | Low | **Low** |
| Submit anonymous review | Low | Low-Med | Low | Low | **Low-Med** |
| View reviews | Low | Low | Low | Low | **Low** |
| Average rating (async update) | Low | Med | Med | Med | **Medium** |
| API Gateway routing | N/A | Med | N/A | Med | **Medium** |
| Search/filter catalog | Low | Low-Med | Low-Med | Low | **Low-Med** |
| Structured pros/cons | Low | Low | Low | Low | **Low** |
| Use-case tagging | Low-Med | Low | Low | Low | **Low-Med** |
| Side-by-side comparison | Med | Low | Low | Low | **Medium** |
| Version-contextual reviews | Low | Low | Low | Low | **Low** |
| Review helpfulness voting | Low | Low | Low | Low | **Low** |

### Async Rating Update — Design Note

The requirement states: *"Average rating updates asynchronously when new reviews arrive."*

This implies an event-driven pattern:
1. Review Service publishes `ReviewCreated` event to message bus
2. Catalog Service (or Rating Service) consumes event and recalculates average
3. Updated average is persisted and served on subsequent reads

**Recommended approach for v1:** Use RabbitMQ or Redis Pub/Sub as lightweight message broker. Avoid Kafka for v1 (operational overhead too high). Spring Cloud Stream with RabbitMQ binder is a natural fit in the Spring ecosystem.

**Alternative (simpler):** If message bus infrastructure is undesirable for v1, use Spring `@Async` with `@EventListener` for in-process async handling. This satisfies the async requirement without external broker, at the cost of less durability. Given Docker Compose local stack scope, in-process async may be pragmatic — but microservices best practices favor explicit message passing between services.

---

## MVP Recommendation

### Prioritize for v1 (Core Value)

1. **Browse catalog** — Table stakes. Angular Material grid/table.
2. **View framework details + average rating** — Core value proposition.
3. **Submit anonymous review** — The primary user action. Include structured pros/cons fields to differentiate from generic review platforms (low incremental cost, high value).
4. **View reviews** — Content consumption. Paginated, sortable.
5. **Async average rating update** — Required by spec. Implement via message queue or async event handling.
6. **API Gateway** — Required by spec. Spring Cloud Gateway with simple route predicates.

### Include if Time Permits (Quick Wins)

7. **Review helpfulness voting** — Low effort, improves content quality signal.
8. **Basic search/filter** — Low effort if catalog is small; use simple SQL `LIKE` or in-memory filter.
9. **Version-contextual reviews** — Single extra field (`versionUsed`) on review schema. Very low effort.

### Explicitly Defer to v2

- User authentication & admin panel
- WebSocket real-time updates
- Use-case tagging (requires more UI design)
- Side-by-side comparison (dedicated UI work)
- Adoption indicators (external API integrations)
- Cloud deployment

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Anonymous review submission | Spam / abuse without auth | Rate limiting by IP, CAPTCHA or honeypot, profanity filter library, input length validation |
| Async rating updates | Race conditions on concurrent reviews | Optimistic locking on rating aggregate, or serialize updates per framework ID |
| API Gateway | Premature complexity (auth, rate limiting, transformation) | Keep v1 gateway dumb: route only. Add cross-cutting concerns in v2. |
| Angular Material | Over-customizing components | Stick to Material Design defaults. Custom theming can be deferred. |
| Review schema design | Flat comment field only | Include `pros`, `cons`, `rating`, `versionUsed` from day one — adding columns to live reviews table is painful. |
| Microservice data boundaries | Catalog and Review services querying each other's DB | Enforce bounded contexts. Catalog service owns framework data; Review service owns reviews. Use API composition or events, not direct DB access. |

---

## Sources

- StackShare.io feature analysis (pros/cons, discussions, adoption, alternatives) — MEDIUM confidence, observed patterns
- Martin Fowler, "Microservices" — architecture patterns, bounded contexts, decentralized data management — HIGH confidence
- Spring Cloud Gateway Docs (v4.0.9) — API Gateway capabilities — HIGH confidence
- Angular Material Components — UI component availability — HIGH confidence
- Docker Compose Docs — local orchestration patterns — HIGH confidence
- G2 / Capterra review platform patterns (industry knowledge) — MEDIUM confidence

