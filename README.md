# Virtual Bank System

A small bank built as event-driven microservices, with one goal that most demos
skip: the money path is actually correct. A transfer cannot double-spend, cannot
drive a balance negative, is idempotent under retries, and survives a service or
broker restart without losing or inventing money. Everything else in the repo
exists to support that claim and to make it easy to see how the parts fit
together.

It is meant to be read as much as run. The code is small and the
[architecture notes](docs/architecture.md) explain not just what each service
does but why, including where consistency is hard and how the design handles it.

## What it is

Five services behind a gateway, talking over REST at the edge and over Kafka in
the core:

| Service | Responsibility |
| --- | --- |
| gateway | single entry point: validates the JWT, routes `/api/**`, aggregates the dashboard |
| user-service | registration, login, RS256 JWT issuance, JWKS endpoint |
| account-service | accounts and balances; applies a transfer atomically |
| transaction-service | the transfer ledger; orchestrates a transfer and records its outcome |
| audit-service | an event-sourced, queryable history of every transfer |

Backed by a single PostgreSQL server (a database per service) and a single KRaft
Kafka broker. An optional assistant service (OpenRouter, free model) and an
optional Tempo/Prometheus/Grafana stack can be turned on when you want them.

## Quick start

Requires Docker (or podman) and Docker Compose. Nothing else needs to be
installed; the services build from source in the images.

```bash
git clone https://github.com/Ab-Romia/Virtual-Bank-System.git
cd Virtual-Bank-System
cp .env.example .env        # adjust the Postgres password for anything real
docker compose up --build   # gateway on http://localhost:8080
```

Then walk through a transfer end to end:

```bash
# register and log in
curl -s -XPOST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"a@example.com","password":"pw123456","fullName":"Alice"}'
TOKEN=$(curl -s -XPOST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"pw123456"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
AUTH="Authorization: Bearer $TOKEN"

# open two accounts, fund one
A=$(curl -s -XPOST localhost:8080/api/accounts -H "$AUTH" -H 'Content-Type: application/json' -d '{"type":"CHECKING","currency":"USD"}' | sed 's/.*"id":"\([^"]*\)".*/\1/')
B=$(curl -s -XPOST localhost:8080/api/accounts -H "$AUTH" -H 'Content-Type: application/json' -d '{"type":"SAVINGS","currency":"USD"}'  | sed 's/.*"id":"\([^"]*\)".*/\1/')
curl -s -XPOST localhost:8080/api/accounts/$A/deposit -H "$AUTH" -H 'Content-Type: application/json' -d '{"amount":100}' >/dev/null

# transfer 30, then read the result and the audit trail
TX=$(curl -s -XPOST localhost:8080/api/transfers -H "$AUTH" -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d "{\"fromAccountId\":\"$A\",\"toAccountId\":\"$B\",\"amount\":30,\"currency\":\"USD\"}" | sed 's/.*"transferId":"\([^"]*\)".*/\1/')
curl -s localhost:8080/api/transfers/$TX -H "$AUTH"
curl -s localhost:8080/api/audit/transfers/$TX -H "$AUTH"
```

`requests.http` has the same flow if you prefer an HTTP client.

## How a transfer works

Starting a transfer returns `202 Accepted` immediately; the money moves on the
event path and the client polls for the result.

1. transaction-service writes a `PENDING` transfer and a `TransferRequested`
   event to its outbox in one transaction, then returns the transfer id.
2. A relay forwards the outbox row to Kafka. account-service consumes it, locks
   both accounts, validates, and applies the debit and credit in one atomic,
   idempotent local transaction, then emits the result through its own outbox.
3. transaction-service consumes the result and marks the transfer `COMPLETED` or
   `FAILED`. audit-service records every step independently.

The correctness rests on three things: a transactional outbox so events are
never lost or published without their state change; idempotent consumers keyed
by the transfer id so at-least-once delivery never moves money twice; and
pessimistic row locks plus a `CHECK (balance >= 0)` constraint so concurrent
transfers cannot double-spend. See [docs/architecture.md](docs/architecture.md)
for the diagrams and the full reasoning, including an honest note on where the
distributed tracing stops.

## Tech stack

Java 21 (virtual threads), Spring Boot 3.5, Spring Cloud Gateway, Spring Kafka on
a single KRaft broker, Spring Security as an OAuth2 resource server (RS256 JWT +
JWKS), Spring Data JPA with Flyway, PostgreSQL, Micrometer Tracing with
OpenTelemetry, and Testcontainers. The optional assistant uses Spring AI over
OpenRouter with a local embedding model for retrieval.

## Optional profiles

Tracing, metrics, and dashboards (Tempo, Prometheus, Grafana on
http://localhost:3000):

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d --build
```

The banking assistant (needs a free OpenRouter API key in `.env`):

```bash
OPENROUTER_API_KEY=... docker compose --profile ai up -d --build
# POST /api/assistant/chat {"message":"what is my balance?"}
```

## Tests

`./mvnw verify` runs the unit and Testcontainers integration tests, including the
concurrency test that fires twenty simultaneous transfers at one account and
proves no double-spend, and the saga, idempotency, and audit tests.

## Limitations and roadmap

This is a teaching reference, not a production bank. Deliberately out of scope
for now: multi-currency conversion, real payment rails, refresh-token rotation, a
running gateway rate limiter (it needs a shared store), and joining the HTTP
request and the asynchronous publish into a single trace (the outbox relay
decouples them on purpose). Each is a reasonable next step rather than a missing
piece.

## Layout

```
vbank-common/          shared events, the outbox, security and error handling (a Spring Boot starter)
gateway/               Spring Cloud Gateway
user-service/          identity and JWT issuance
account-service/       balances and the atomic transfer
transaction-service/   the transfer ledger and orchestration
audit-service/         event-sourced audit log
ai-assistant-service/  optional Spring AI assistant
frontend/              React single-page app
docs/                  architecture notes and diagrams
infra/                 Postgres init, Tempo, Prometheus, Grafana config
```
