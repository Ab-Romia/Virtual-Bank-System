# Virtual Bank System Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This is a phased master plan; each phase produces working, testable software and is built and verified before the next begins.

**Goal:** Rebuild the Virtual Bank System into a correct, secure, observable, one-command-up reference implementation of event-driven microservices, with a saga-based money transfer that is provably race-free, plus a guide, blog post, and portfolio case study.

**Architecture:** A multi-module Maven monorepo. Five core Spring Boot services (gateway, user, account, transaction, audit) plus one optional AI service, sharing a `vbank-common` module. A single Postgres server with a database per service and a single KRaft Kafka broker, all orchestrated by one `docker-compose.yml` with profiles. The transfer is an orchestration saga over Kafka using the transactional outbox pattern with compensation and dead-letter queues.

**Tech Stack:** Java 21 (virtual threads), Spring Boot 3.5.x, Spring Cloud Gateway, Spring Kafka (KRaft), Spring Security OAuth2 resource server (RS256 JWT + JWKS), Spring Data JPA, Flyway, PostgreSQL + pgvector, Spring AI over OpenRouter (a free model) with a local embeddings model, Micrometer Tracing + OpenTelemetry, Tempo, Prometheus, Grafana, Testcontainers, React 19 + Vite + TanStack Query + Tailwind v4 + Vitest.

## Global Constraints

These apply to every task. Exact values:

- Java 21; `spring.threads.virtual.enabled=true` on all blocking services.
- Spring Boot 3.5.x, unified via the parent POM; one Spring Cloud train via BOM.
- Single KRaft Kafka broker; no Zookeeper.
- One Postgres server; one database per service; pgvector extension for the AI service.
- Flyway-only schema; `spring.jpa.hibernate.ddl-auto=validate`; migrations under `db/migration`.
- Authentication: RS256 JWT issued by user-service with a JWKS endpoint; the gateway and every downstream service validate it; user id from the `sub` claim only; ownership checks on every account and transfer resource.
- One HTTP client style: `@HttpExchange` interfaces over `RestClient` with explicit connect and read timeouts plus Resilience4j; no `RestTemplate`, no OpenFeign, no blocking `WebClient` on servlet threads.
- Errors: `ProblemDetail` (RFC 9457) via `@RestControllerAdvice`.
- Observability: Micrometer Tracing + OpenTelemetry (OTLP); trace context propagated across Kafka; structured JSON logs with traceId, spanId, correlationId.
- Tests: Testcontainers (Postgres + Kafka) per service; the build is `mvn verify`.
- Secrets: only via environment variables and a committed `.env.example`; never in source; gitleaks in CI.
- Commits authored solely as `Ab-Romia <aabouroumia@gmail.com>`; no AI attribution, co-author lines, or AI references anywhere in commits, code, docs, or PRs.
- No em-dashes in any prose content; use commas, semicolons, colons, or periods.
- Diagrams authored as Mermaid and exported to SVG for the blog.

## Monorepo file structure

```
Virtual-Bank-System/
  pom.xml                      parent: modules, Java 21, Spring Boot + Cloud + AI BOMs, plugin mgmt
  vbank-common/                shared: domain events, outbox entity + relay, log/audit envelope,
                               ProblemDetail advice, CORS bean, JWT resource-server config, tracing config
  gateway/                     Spring Cloud Gateway: JWT validation, routing, rate-limit, dashboard aggregation
  user-service/                identity, registration, login, JWT issuance (RS256), JWKS endpoint
  account-service/             accounts, balances, atomic debit/credit, outbox, processed-events inbox
  transaction-service/         saga orchestrator, transfer ledger, outbox
  audit-service/               Kafka consumer to audit DB, DLQ, error handling
  ai-assistant-service/        Spring AI + Claude, tool-calling, pgvector RAG (compose profile ai)
  frontend/                    React 19 + Vite SPA
  infra/
    postgres/init-databases.sql
    otel/otel-collector-config.yaml
    prometheus/prometheus.yml
    tempo/tempo.yaml
    grafana/provisioning/{datasources,dashboards}/...
  docs/
    superpowers/{specs,plans}/...
    architecture/{c4-container,transfer-sequence,erd}.{mmd,svg}
    guide/*.md
  .github/workflows/ci.yml
  docker-compose.yml           profiles: core, observability, ai
  .env.example
  README.md
```

---

## Phase 0: Foundation (build, common module, infrastructure)

**Goal:** A multi-module Maven build with a shared `vbank-common` module, an infrastructure compose (one Postgres, one KRaft broker), and empty-but-building service modules. Exit when `mvn -q -DskipTests package` succeeds for all modules and `docker compose up -d` brings Postgres and Kafka healthy.

**Files (create):**
- `pom.xml` (parent): `<packaging>pom</packaging>`, modules list, `java.version=21`, Spring Boot parent 3.5.x, dependencyManagement importing Spring Cloud + Spring AI BOMs, build plugin management (spring-boot-maven-plugin, flyway, jib or spring-boot buildpacks for images).
- `vbank-common/pom.xml` and sources:
  - `events/`: `DomainEvent` base, `TransferRequested`, `SourceDebited`, `DestinationCredited`, `DebitFailed`, `CreditFailed`, `SourceRefunded`, with a stable `eventId`, `transferId`, `occurredAt`, `type`.
  - `outbox/`: `OutboxEntry` JPA entity, `OutboxRepository`, `OutboxRelay` (a `@Scheduled` poller publishing unsent rows to Kafka by topic + key, marking sent), `OutboxAppender` helper.
  - `audit/`: `AuditEnvelope` record + `AuditPublisher` (single Kafka producer; replaces the four duplicated `KafkaProducerService` classes).
  - `web/`: `GlobalExceptionHandler` (`@RestControllerAdvice` returning `ProblemDetail`), `CorsConfig` bean.
  - `security/`: `ResourceServerConfig` (validates RS256 JWT via JWKS URI; helper to read `sub`), `OwnershipChecks` utility.
  - `observability/`: tracing/JSON-logging config shared beans.
- `docker-compose.yml` with `core` profile: `postgres` (one server, healthcheck), `kafka` (KRaft single broker, healthcheck), plus placeholders for the service containers added per phase.
- `infra/postgres/init-databases.sql`: create `vbank_user`, `vbank_account`, `vbank_transaction`, `vbank_audit` databases; enable `pgcrypto` where needed.
- `.env.example`: all config keys (DB creds, Kafka bootstrap, JWT key material refs, `OPENROUTER_API_KEY`, `AI_MODEL`) with safe placeholder values.
- `.gitignore`: ignore `.idea/`, build output, local `.env`.

**Tasks:**
- Parent POM with modules and BOMs; `mvn -q -DskipTests package` builds an empty reactor.
- `vbank-common` with the events, outbox, audit, web, security, observability building blocks; a unit test for `OutboxRelay` selection logic and for the `ProblemDetail` advice.
- Infra compose (Postgres + KRaft Kafka) with healthchecks; `init-databases.sql`; verify `docker compose up -d` and both healthy, and that the four databases exist.
- Remove WSO2 artifacts, the `.idea/` directory, `app_wso2.txt`, `wso2_requests.http`, the Synapse XMLs, and the redundant compose files in the same commit set.

**Exit criteria / verification:**
- `mvn -q -DskipTests package` succeeds.
- `mvn -q -pl vbank-common test` passes.
- `docker compose up -d postgres kafka` then a healthcheck poll shows both healthy; `psql -l` (or a container exec) lists the four databases.

---

## Phase 1: Identity and zero-trust auth

**Goal:** A real login that issues an RS256 JWT, a JWKS endpoint, the gateway validating tokens and routing, and at least one downstream service enforcing the token and ownership. Exit when an unauthenticated request to a protected route through the gateway returns 401 and an authenticated one succeeds, proven by a Testcontainers test.

**Files:** `user-service/*` (registration with BCrypt, login issuing JWT, `/.well-known/jwks.json`, Flyway `V1__users.sql`), `gateway/*` (routes, `SecurityWebFilterChain` validating JWT via user-service JWKS, rate-limit filter, dashboard aggregation controller stub), `vbank-common/security` wiring in user/account services.

**Tasks:** user registration + login + JWT issuance (RS256 keypair from env or generated for dev); JWKS endpoint; gateway JWT validation + routing to user-service; ownership check helper; tests asserting 401 vs 200 and that a forged `sub` cannot read another user.

**Exit criteria:** Testcontainers integration test: register, login, call a protected endpoint with and without the token (200 vs 401), and an ownership-violation test (403).

---

## Phase 2: The transfer saga (centerpiece)

**Goal:** A correct, idempotent, compensating money transfer over Kafka with the outbox pattern and DLQs. This is the project's reason to exist.

**Files:** `account-service/*` (Account entity with balances, `V*__account.sql` including `CHECK(balance >= 0)`, atomic conditional debit/credit, processed-events inbox table, outbox, Kafka command consumer, event producer), `transaction-service/*` (Transfer entity + ledger, `POST /transfers` with Idempotency-Key, outbox emitting `TransferRequested`, consumer of account events, finalize + compensation logic), Kafka topic config, DLQ error handlers in both.

**Tasks:** account-service atomic debit (`UPDATE ... WHERE balance >= :amt`) + credit in one tx, idempotent by event id; outbox relay publishing; transaction-service orchestration (initiate, consume results, mark COMPLETED/FAILED, compensate on credit failure); DLQ via `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`; remove the parallel `account_transactions` ledger.

**Exit criteria:**
- Saga integration tests (Testcontainers Postgres + Kafka): happy path COMPLETED; insufficient funds FAILED with no balance change; compensation path FAILED (refunded) with source restored.
- Idempotency test: replaying the same command does not double-debit.
- Concurrency test: N parallel transfers from one account, asserting no double-spend and no negative balance.
- Chaos test: account-service restarted mid-saga; final state consistent (no money created or destroyed).

---

## Phase 3: Audit service

**Goal:** The audit/logging consumer no longer swallows errors. Exit when a malformed message lands in the DLQ and well-formed messages persist, proven by a test.

**Files:** `audit-service/*` (Kafka consumer of `audit.log`, audit DB schema via Flyway with `CREATE EXTENSION pgcrypto`, DLQ handler, no catch-and-commit). Remove the redundant `LogMessage` DTO, `JacksonConfig`, `KafkaConsumerConfig` that duplicate Boot auto-config.

**Exit criteria:** Testcontainers test: a valid audit event persists; a poison message routes to `audit.log.DLT`; offsets are not committed past a failed message silently.

---

## Phase 4: Observability stack

**Goal:** One transfer is one distributed trace from gateway through Kafka to audit, viewable in Grafana/Tempo, with Prometheus metrics and pre-provisioned dashboards.

**Files:** `infra/otel/otel-collector-config.yaml`, `infra/tempo/tempo.yaml`, `infra/prometheus/prometheus.yml`, `infra/grafana/provisioning/*`, OTLP exporter + Micrometer Tracing config in `vbank-common`, Kafka trace propagation config, `docker-compose.yml` `observability` profile.

**Exit criteria:** With `--profile observability`, run a transfer; confirm a single trace spans the services and the Kafka hop in Tempo; capture the traced-failed-transfer screenshot for the README. Actuator health and Prometheus scrape verified.

---

## Phase 5: AI assistant (on-brand, real)

**Goal:** A Spring AI assistant over OpenRouter (a free, env-configurable model) that answers balance questions via JWT-scoped tool-calling and policy questions via pgvector RAG, with no data-exfil hole. No paid key required.

**Files:** `ai-assistant-service/*` (Spring AI `ChatClient` with the OpenAI client pointed at `https://openrouter.ai/api/v1`, model from `AI_MODEL`, key from `OPENROUTER_API_KEY`; tools `getAccounts`, `getRecentTransactions`, `getBalance` calling the gateway with the user's token; pgvector `VectorStore`; a local in-process embeddings model, for example all-MiniLM-L6-v2, since OpenRouter has no embeddings API; a bank-policy corpus + an embedding loader; prompt-injection guards; graceful degradation returning a clear "AI not configured" message when `OPENROUTER_API_KEY` is absent), pgvector in the `ai` compose profile. Do not use Anthropic or OpenAI directly.

**Exit criteria:** Integration test (or recorded transcript) showing a balance answer via tool-calling scoped to the authenticated user, a policy answer via retrieval, a refusal of cross-user access, and clean behavior when no API key is set.

---

## Phase 6: Frontend modernization

**Goal:** A clean SPA with real JWT auth routed through the gateway, the dashboard, transfer, and assistant working end to end, plus a demo GIF.

**Files:** `frontend/*` (axios interceptor attaching the Bearer token and handling 401, all calls to the gateway, TanStack Query, Tailwind v4, restrained professional UI, Vitest tests). Remove the localStorage auth boolean, the dead Vite proxy or hardcoded URLs, and `date-fns` if unused.

**Exit criteria:** `npm run build` succeeds; Vitest passes; manual or Playwright walkthrough of register, login, dashboard, transfer, assistant; record the demo GIF.

---

## Phase 7: Repo hygiene and history rewrite

**Goal:** No secrets anywhere in code or history; clean repo; CI green.

**Files:** `.github/workflows/ci.yml` (build, test, gitleaks, image build), README rewrite (honest, diagram-led, no emoji marketing, accurate facts, security model, limitations and roadmap), `requests.http` updated to the new API.

**Tasks:** tag a backup of the pre-rewrite state; `git filter-repo` to purge committed secrets across history; rotate secrets; prune the `origin/yassin` and `origin/romia` stray branches; force-push; verify gitleaks passes on the full history.

**Exit criteria:** gitleaks reports clean on the full history; CI is green; `git clone` then `docker compose up` yields a healthy stack with a seed script.

---

## Phase 8: Guide, diagrams, blog, portfolio case study

**Goal:** The teaching narrative that makes it stand out, in the owner's voice.

**Files:** `docs/architecture/*` (C4 container, transfer sequence happy + compensation, per-DB ERD as Mermaid + SVG), `docs/guide/*` (concept-by-concept: why DB-per-service, why a BFF/gateway, where consistency breaks and how the saga fixes it, idempotency, outbox, DLQ, tracing, each linked to the exact files), romia.dev `src/data/blog.ts` (a guide-style case study post with custom pure-SVG diagrams and the demo GIF), romia.dev `src/data/resume.ts` (rewrite the Virtual Banking project `caseStudy`, links, categories).

**Exit criteria:** The blog post and case study render on romia.dev (build passes); diagrams display on GitHub; prose reads as the owner's, with no AI tells and no AI attribution; a final human-voice review pass.

---

## Self-review (against the spec)

- Spec coverage: every spec section maps to a phase. Auth -> Phase 1; saga + correctness proofs -> Phase 2; audit/DLQ -> Phase 3; observability -> Phase 4; AI -> Phase 5; frontend -> Phase 6; secrets/history/CI/README -> Phase 0 (removal) + Phase 7; docs/blog/case study -> Phase 8; modernization sweep -> threaded through Phases 0 to 6.
- Dependency order: Phase 0 underpins all; Phase 1 (auth) precedes services enforcing it; Phase 2 depends on common + Kafka + auth; Phase 4 depends on services existing; Phase 5 depends on auth + gateway; Phase 7 after the code stabilizes; Phase 8 last.
- Each phase has concrete exit criteria expressed as runnable verification, not assertions.
- The honesty framing (saga steps vs single ACID tx) is captured in Phase 2 and surfaced in Phase 8.
