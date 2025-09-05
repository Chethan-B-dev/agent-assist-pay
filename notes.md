# Project Notes

I thoroughly enjoyed building this **PayNow Agent Assist** project!  
It was a great opportunity to combine technologies I already knew with some new ones I was excited to try out.

---

## Tools & Technologies I Used

- **Architecture Diagram**: I used ChatGPT to generate an ASCII architecture diagram and included it in the `README.md`. This helped me explain the flow visually without relying on external tools.
- **Business Logic**: I built a simulation of the `getRiskSignals` logic. To save time, I experimented with lovable.dev to generate the initial structure and then tweaked it for this demo project.
- **API Gateway**: For the first time, I implemented an API gateway using **Spring Cloud Gateway with WebFlux**. At my company we usually use Netflix Zuul, so this was a good chance to learn the newer stack.
- **Observability Stack**: Instead of Datadog (what I use at work), I set up a lightweight ELK stack (Elasticsearch, Logstash, Kibana) locally for logs and dashboards.
- **Risk Service**: I created a mock risk evaluation service. In production, this would call an external risk provider’s API, but I wanted to simulate realistic responses for testing.
- **Agent Implementation**: I wired services together with REST APIs. If I were doing this in production, I’d probably use an MCP server for tool orchestration, but REST kept the demo simple.
- **Database**: I used H2 so the project could run quickly without extra setup. In real deployments I’d pick PostgreSQL or another production-grade database.
- **Project Structure**: I structured the repo as a multi-module Maven project so I could manage all microservices in one place rather than juggling multiple repos.
- **Containerization**: I used Docker Compose to containerize everything. It’s not as heavy as Kubernetes but perfect for spinning up all services for a demo.
- **Evaluation**: I wrote a small Python script to run test scenarios and measure decision outcomes of the agent engine.
- **Documentation**: I captured screenshots of the services running locally and added them under `/images` to make the README more approachable.
- TODO: i have not yet added any UI for this, just focused on the backend side.
---

## Performance Optimizations

### P95 Latency Improvements
- I used `CompletableFuture` to call **getBalance** and **getRiskSignals** in parallel.
- This cut overall latency by about **40%** compared to sequential calls.
- I also made sure each path had proper error handling so one failure wouldn’t break the flow.

### Request Body Caching
- At the gateway, I implemented a `ServerHttpRequestDecorator` to cache request bodies.
- This avoided multiple reads of the same input stream (important for rate limiting).
- It was especially useful because reactive streams normally only allow one read.

### Redis-Based Caching
- I implemented idempotency caching with a configurable TTL.
- Duplicate requests (with the same `idempotencyKey`) got the cached response.
- I used the **reactive Redis client** so everything remained non-blocking.

### Pre-validation
- I annotated DTOs with Jakarta validation.
- This way, bad requests fail fast before hitting downstream services.
- It reduced unnecessary processing and improved throughput.

### Asynchronous Event Publishing
- I used `CompletableFuture.runAsync()` to publish events without blocking.
- This let the main payment flow complete faster while still ensuring events went out.

---

## Security Measures

### PII Protection
- I added a `redactCustomerId()` utility to mask sensitive IDs (e.g., `cu_****78`).
- DTOs have a `toRedactedString()` method so logs never contain real PII.
- I applied consistent redaction rules across all services.

### Secure Logging
- I separated event logs from application logs.
- In production, I would use structured JSON logging (via `logback-spring.xml`) and forward to ELK.
- Sensitive fields are explicitly excluded from serialization.

### Authentication & Authorization
- At the gateway, I implemented **API key authentication**.
- Keys are configurable via environment variables, and invalid requests return 401 with a standardized JSON error.
- I centralized all security at the gateway, protecting every endpoint except health checks and OPTIONS requests.
- I also added **rate limiting by customer ID**.

---

## Observability Stack

### Logging
- I configured **structured JSON logging** with service name, version, and correlation IDs.
- Logs are forwarded to the ELK stack via TCP.
- Each request gets a unique correlation ID that propagates across services and is also returned in the `X-Request-ID` response header.

### Metrics
- I created a `PaymentMetrics` class integrated with Micrometer.
- It tracks counters for different decision outcomes (allow, review, block).
- Latency is measured with Micrometer Timers.

- I exposed all metrics via Spring Boot Actuator for **Prometheus** scraping.
- I added P95 latency calculations to capture realistic performance.

### Distributed Tracing
- I built an **agent trace** feature that records each tool call (with timestamps and durations).
- This trace is returned in the API response for debugging.
- In Kibana, I set up a dashboard where I can query logs by correlation ID and filter by service or severity.

---

## Agent Implementation

### Tools
- **Account Tool**: Fetches account balance and reserves funds. I used optimistic locking to handle concurrency.
- **Risk Tool**: Simulates a risk score, level, and factors. I made it behave like a real risk service by varying outputs.
- **Case Tool**: Creates review/block cases with context. I included reasons and risk factors to simulate a manual review workflow.

### Retries
- I configured retries with Spring Retry using `@Retryable`.
- Each retry does 2 attempts with a 500ms backoff.
- This made the agent more resilient to transient failures.

### Error Handling
- I designed a simple exception hierarchy.
- If the agent fails, it safely falls back to a REVIEW decision rather than blocking everything.
- Errors are logged with detailed context for debugging.

---

## Guardrails

- **Balance Reservation**: I made balance reservations transactional to prevent double-spending. Unused reservations automatically expire.
- **Rate Limiting**: I implemented a token bucket algorithm, with limits enforced per customer ID. Under load, the system degrades gracefully instead of failing hard.
- **Decision Rules**: I kept decision rules auditable and transparent. There are thresholds based on risk level and transaction amount so that business stakeholders can review them.

---
