# Virtual Bank System: Rebuild Design

Status: approved
Date: 2026-06-19
Owner: Abdelrahman Abouroumia (Ab-Romia)

## Goal

Turn the Virtual Bank System from an undersold, operationally painful, partly
broken microservices demo into a reference implementation of event-driven
microservices that gets distributed money-transfer correctness right. The
project should teach the hard parts (idempotency, the outbox pattern,
compensation, dead-letter handling, distributed tracing) honestly, run with one
command, and read as the work of an engineer who understands distributed
systems.

Positioning line: "A reference implementation of event-driven microservices
that gets distributed money-transfer correctness right, the thing most demos
fake."

## Why this rebuild

The deep audit (2026-06-19) established the ground truth:

- The README's two marquee claims are false. Kafka is fire-and-forget audit
  logging only (one `logging-topic`); money movement is synchronous REST with a
  TOCTOU race, no idempotency, no locking, no saga.
- No authentication or authorization anywhere. Every `SecurityConfig` ends in
  `permitAll()`. Login returns no token. The frontend "auth" is a localStorage
  boolean. IDOR across every endpoint, including moving money between arbitrary
  account UUIDs.
- Committed live secrets (WSO2 tokens, Postgres password, `admin/admin`) and an
  unauthenticated `/system/*` endpoint that dumps account rows with balances.
- Does not run in one command: no Dockerfiles, a 6-terminal startup, docs that
  contradict the code on Java version, DB count, and ports.
- Pervasive AI-paste tells: four byte-identical `KafkaProducerService` classes,
  `// Add this line` comments, kitchen-sink unused dependencies, an emoji
  marketing README, a "RAG" service that does no retrieval.

The bones are good: clean service decomposition, database-per-service, a real
reactive aggregation layer. The git history is genuinely human. So this is a
cleanup-plus-correctness rebuild, not a from-scratch rewrite, and all authorship
stays with the owner.

## Decisions (locked)

1. Event model: true event-driven saga. The transfer becomes a real
   orchestration saga over Kafka with a transactional outbox, compensation, and
   a dead-letter topic.
2. AI assistant: rebuilt for real and on-brand. Spring AI talking to OpenRouter
   (a free model, OpenAI-compatible API), identity from the JWT, tool-calling for
   live account data, and pgvector RAG over a bank-policy corpus so "retrieval"
   is true. Embeddings run via a small local model so no paid key is ever needed.
3. Observability and testing: standard. Micrometer Tracing plus OpenTelemetry to
   Tempo, Prometheus, and Grafana; Testcontainers integration tests; a
   concurrency test and a chaos test.
4. Gateway: Spring Cloud Gateway as the single front door that validates the
   JWT. WSO2 removed entirely.
5. Footprint: five core services plus one optional AI service; one Postgres
   server with a database per service; a single KRaft Kafka broker; one
   `docker-compose.yml` with profiles.
6. Deliverables: rebuilt repo, an in-repo `/docs` concept guide with diagrams, a
   romia.dev blog post, a romia.dev portfolio case study rewrite, and a demo
   artifact (GIF plus a traced-failed-transfer screenshot).
7. Authorship: all commits solely as `Ab-Romia <aabouroumia@gmail.com>`, no AI
   attribution anywhere, prose matched to the owner's existing blog voice. No
   em-dashes in any content.
8. History: rewrite git history with `git filter-repo` to purge committed
   secrets, rotate them, and force-push. Authorized by the owner.

## Target architecture

Five core services plus one optional AI service. Only the gateway is published on
the host; everything else is internal to the compose network.

| Service | Role | Key point |
|---|---|---|
| gateway (Spring Cloud Gateway, reactive) | single public entry: JWT validation, routing, rate-limit, dashboard aggregation (reactive `Mono.zip`) | replaces WSO2; no service is reachable without a verified token |
| user-service | identity and JWT issuance (RS256 with a JWKS endpoint) | BCrypt passwords; a real login that returns a signed token |
| account-service | balances; saga participant: atomic, idempotent, locked debit and credit; transactional outbox; processed-events (inbox) table | the actual money mutation, race-free |
| transaction-service | saga orchestrator; transfer ledger; its own outbox | owns the transfer lifecycle, consumes account events, finalizes or compensates |
| audit-service (was logging-service) | Kafka consumer to an audit database; DLQ; real error handling | the audit trail no longer silently drops messages |
| ai-assistant-service (compose profile `ai`) | Spring AI plus Claude; JWT identity; tool-calling for live data; pgvector RAG over a bank-policy corpus | honest dual-pattern AI |

The dashboard aggregation lives in the gateway app (a reactive controller that
fans out with `Mono.zip`). If clarity is later preferred over leanness it can be
split into a dedicated bff-service; for now it stays folded into the gateway for
a lean one-command demo.

### Compose profiles

One `docker-compose.yml` with profiles, replacing the three contradictory compose
files:

- `core` (default): gateway, user, account, transaction, audit, one Postgres
  server, one KRaft Kafka broker.
- `observability`: OpenTelemetry Collector, Tempo, Prometheus, Grafana.
- `ai`: ai-assistant-service plus a pgvector-enabled Postgres (or the pgvector
  extension on the main server).

`docker compose up` brings core. `--profile observability --profile ai` brings
the full stack.

## The transfer saga (core teaching artifact)

Orchestration saga over a single KRaft Kafka broker, with the transactional
outbox pattern so events are never lost.

Flow:

1. `POST /transfers` with an `Idempotency-Key` header and a body of
   `{fromAccountId, toAccountId, amount, currency}`. The JWT identifies the user.
   transaction-service verifies the source account belongs to the caller.
2. transaction-service writes a `PENDING` transfer row and a `TransferRequested`
   row to its outbox in one local transaction; returns `202 Accepted` with a
   transfer id and a status URL. The idempotency key makes a retried request
   return the same transfer rather than create a new one.
3. A polling outbox relay publishes unsent outbox rows to Kafka topic
   `transfer.commands`, keyed by transfer id for per-transfer ordering, then
   marks them sent. Debezium CDC is documented as the production upgrade.
4. account-service consumes the command, runs an atomic conditional debit
   (`UPDATE accounts SET balance = balance - :amt WHERE id = :from AND balance >=
   :amt`) then a credit, all in one local transaction, guarded by a
   processed-events table keyed by event id so reprocessing is a no-op. It emits
   `SourceDebited` and `DestinationCredited`, or a `...Failed` event with a
   reason, via its own outbox.
5. transaction-service consumes the result and marks the transfer `COMPLETED` or
   `FAILED`. On a credit failure after a successful debit it issues a
   compensating `RefundRequested`, account-service credits the source back, and
   the transfer ends `FAILED (refunded)`.
6. Poison messages land in DLQs (`*.DLT`) via Spring Kafka's `DefaultErrorHandler`
   plus `DeadLetterPublishingRecoverer` with bounded retry and backoff.

Topics (KRaft, keyed for ordering):

- `transfer.commands`: orchestrator to account-service (DebitRequested,
  RefundRequested), keyed by transfer id.
- `transfer.events`: account-service to orchestrator (SourceDebited,
  DestinationCredited, DebitFailed, CreditFailed, SourceRefunded), keyed by
  transfer id.
- `audit.log`: all services to audit-service, keyed by correlation id.
- DLQs: `transfer.commands.DLT`, `transfer.events.DLT`, `audit.log.DLT`.

Honesty note (documented in the repo and the blog): an intra-bank transfer where
both accounts share a database could be a single ACID transaction. We
deliberately model debit and credit as saga steps to demonstrate idempotency,
the outbox, compensation, and DLQ handling, the techniques required the moment
the destination is another service or bank. Stating this plainly is itself a
senior signal.

Proof artifacts:

- A concurrency test fires N parallel transfers against the same source account
  and asserts no double-spend and no negative balance.
- A chaos test kills account-service mid-saga and asserts eventual consistency:
  no money created or destroyed.

## Security model

- RS256 JWT issued by user-service, which exposes a JWKS endpoint.
- The gateway and every downstream service validate the JWT (defense in depth,
  zero trust). User id is derived from the `sub` claim, never from the client.
- Ownership checks: a caller can only read or move money from accounts they own.
  This removes the IDOR across every endpoint.
- BCrypt password hashing.
- CORS locked to the frontend origin; no wildcard with credentials.
- The `/system/*` diagnostics endpoint and the hardcoded personal UUID are
  deleted.
- All secrets via environment variables and a committed `.env.example`. gitleaks
  runs in CI. History is purged of secrets and they are rotated.
- The JWT is propagated on internal calls; internal services are not published on
  the host.

## Observability (standard)

- Micrometer Tracing plus OpenTelemetry (OTLP) to an OpenTelemetry Collector,
  then Tempo (traces), Prometheus (metrics), and Grafana (dashboards as code).
- Trace context propagated across Kafka so one transfer is a single trace from
  gateway to transaction to account and back to audit.
- Structured JSON logging (Logback) with traceId, spanId, and correlation id.
- Actuator health, readiness, and liveness on all services.
- The README includes a screenshot of a traced failed transfer.

## Testing (standard)

- Testcontainers (Postgres and a KRaft Kafka image, or Redpanda for speed)
  integration tests per service.
- The concurrency and chaos tests described above.
- A Kafka round-trip test (produce, consume, persist).
- Saga tests: happy path, insufficient funds, and the compensation path.
- Frontend: Vitest unit and component tests for the auth and transfer flows.
- GitHub Actions CI: `mvn verify` (Testcontainers), frontend tests, gitleaks,
  and Docker image builds.

## Modernization sweep

- Unify on Spring Boot 3.5.x and Java 21 with virtual threads enabled.
- Single KRaft Kafka broker; remove Zookeeper.
- One HTTP client: `@HttpExchange` over `RestClient` with timeouts plus
  Resilience4j; remove RestTemplate, unused OpenFeign, and blocking WebClient on
  servlet threads.
- Flyway-only with `ddl-auto=validate`; fix the broken and contradictory
  migrations; correct the `db.migration` path and the missing pgcrypto extension.
- `ProblemDetail` (RFC 9457) for errors via `@RestControllerAdvice`; fix the
  broken `@ExceptionHandler` type mismatch.
- springdoc-openapi generated from controllers, replacing the drifted hand-written
  `openapi.yaml`.
- A `vbank-common` module for the Kafka producer or outbox relay, the log
  envelope, the CORS bean, and shared enums, removing the four duplicated
  producer classes and the copy-pasted CORS blocks.
- Per-service multi-stage Dockerfiles; remove the `version` key from compose.
- Remove kitchen-sink unused dependencies (webflux where unused, OpenFeign and
  the spring-cloud BOM where unused, spring-restdocs and asciidoctor, devtools,
  date-fns in the frontend), the empty start.spring.io POM stubs, the no-op
  try/catch wrappers, `System.out.println`, the commented-out `DevSecurityConfig`,
  duplicate `TestSecurityConfig`, and dead repository methods.
- Drop the parallel `account_transactions` ledger; transaction-service owns the
  one transfer ledger, account-service owns balances plus an idempotency journal
  keyed by the real transfer id.
- Remove the `AccountInactiveJob` or make its threshold realistic and
  config-driven.

## AI assistant (rebuilt real)

- Spring AI `ChatClient` over OpenRouter (the Spring AI OpenAI client pointed at
  `https://openrouter.ai/api/v1`), using a free, configurable model (for example
  a free Llama or Qwen instruct model that supports tool-calling). The model id
  is an environment variable so it is easy to swap. Graceful degradation if
  `OPENROUTER_API_KEY` is absent (the assistant returns a clear "AI is not
  configured" message rather than crashing). Do not use Anthropic or OpenAI
  directly; the demo must run free.
- Embeddings run via a small local model (Spring AI in-process transformers, for
  example all-MiniLM-L6-v2) because OpenRouter does not expose an embeddings API.
  This keeps RAG fully free with no embeddings key.
- Identity from the JWT, never a client-supplied user id.
- Tool-calling (function calling): read-only tools such as `getAccounts`,
  `getRecentTransactions`, and `getBalance` scoped to the authenticated user, hit
  through the gateway. The response returns the model's answer, not the raw
  assembled context.
- pgvector RAG over a small bank-policy and FAQ corpus (fees, limits, how
  transfers work, security) so retrieval is real. Balance questions go through
  tool-calling; policy questions go through retrieval.
- Prompt-injection mitigations: a constrained system prompt, JWT-scoped tools,
  read-only actions, and no acting on instructions found in retrieved or
  account data.

## Frontend modernization

- Keep React 19 and Vite.
- Real JWT auth: token attached by an axios interceptor; a 401 triggers logout;
  all traffic routes through the gateway.
- TanStack Query for data fetching and caching; Tailwind v4.
- A clean, restrained, professional UI (no AI-generated look): dashboard,
  transfer, and the assistant.
- Vitest tests for the auth and transfer flows.
- A demo GIF of the end-to-end flow.

## Deliverables

1. Rebuilt repo: secure, correct, observable, tested, one command up.
2. `/docs` concept guide: a C4 container diagram, the transfer-saga sequence
   (happy and compensation), a per-database ERD, a concept-to-file map, and an
   honest limitations and roadmap section. Diagrams as Mermaid plus exported SVG.
3. romia.dev blog post: a guide-style case study in `src/data/blog.ts` with
   custom pure-SVG diagrams and a demo GIF, in the owner's voice.
4. romia.dev portfolio case study: rewrite the existing Virtual Banking project
   entry and its links and categories.
5. Demo artifact: a 60 to 90 second GIF of the flow plus the traced
   failed-transfer screenshot. The live experience is `git clone` then
   `docker compose up`.

## Out of scope (for now)

- A live publicly hosted running stack (cost of six JVMs plus Kafka). The demo is
  a GIF plus screenshots plus a flawless local one-command run.
- Multi-currency FX, real payment rails, refresh-token rotation beyond a documented
  roadmap item, and Kubernetes manifests (mentioned as roadmap, not built).
- Consumer-driven contract tests and a full security regression suite (these were
  the "full" testing tier; standard tier is the agreed scope, with the option to
  add them later).

## Risks and mitigations

- Resource footprint: many JVMs plus Kafka plus observability on a laptop.
  Mitigation: virtual threads, tuned heaps, and compose profiles so the default
  run is lean.
- History rewrite: force-push is irreversible. Mitigation: a tagged backup of the
  pre-rewrite state before `git filter-repo`, and secret rotation regardless.
- Scope: this is a large rebuild. Mitigation: a phased implementation plan with
  each service and cross-cutting concern as an independently buildable, testable
  unit; the core saga correctness path is the priority, the AI service is the
  last and optional core piece.

## Success criteria

- `git clone` then `docker compose up` yields a healthy stack; a seed script
  creates demo users and accounts.
- A transfer can be initiated and traced end to end; a failed transfer shows a
  single trace and a clean compensation.
- The concurrency test proves no double-spend; the chaos test proves no money
  lost or created.
- No secrets in the repo or its history; gitleaks passes in CI.
- The README leads with diagrams, states exactly what is event-driven versus
  synchronous, documents the real security model, and includes limitations.
- The blog post and portfolio case study read as authored by the owner, with no
  AI tells and no AI attribution.
