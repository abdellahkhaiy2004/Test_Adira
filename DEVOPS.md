# DevOps — exam part I

Deliverables: `Dockerfile` (root, shared by the Java modules), `frontend/Dockerfile`,
`docker-compose.yml`, `.github/workflows/ci-cd.yml`, and the short answers below.

> **Verification status.** Docker is not installed on the development machine used for this project
> and the repository has no GitHub remote, so **none of the container or pipeline files has been
> executed**. They are written against the real module names, ports and environment variables, but
> they are unverified. The Maven build, the backend test suite, the Angular build and the Angular
> tests were all run locally; those results are real.

## Images

| Image | Base | Notes |
|---|---|---|
| 7 Java services | `eclipse-temurin:21-jre-alpine` | One shared multi-stage `Dockerfile`, selected with `--build-arg MODULE=` |
| frontend | `nginx:1.27-alpine` | Built with `node:22-alpine`, served as static files |

The Java build stage copies the POMs before the sources so the dependency layer is cached while only
code changes. The runtime stage keeps a JRE and the jar, and runs as the non-root `bank` user.

```bash
docker build --build-arg MODULE=account-service -t projet-adrea/account-service .
docker compose up -d --build
```

## 1. Secrets and how they differ from configuration

**Configuration** describes *how* a service should behave and is harmless if read: ports, the Eureka
URL, the Kafka bootstrap address, `bank.transaction.max-amount`. It belongs in
`application.properties`, in the Config Server, and in `docker-compose.yml`, and it is version-
controlled on purpose so a deployment is reproducible and reviewable.

**Secrets** grant access: the LLM API key, database passwords, registry credentials. They are never
committed, never baked into an image layer, and never printed in a log. In this project the only real
secret is `OPENAI_API_KEY`; compose reads it from the environment with
`${OPENAI_API_KEY:?...}`, so the stack refuses to start rather than run with an empty one, and the
pipeline takes it from the GitHub environment's secret store. The registry login uses the
short-lived `GITHUB_TOKEN` rather than a stored password.

The practical rule: configuration can go in the repository, secrets are injected at run time. A
production deployment would move them to a dedicated manager (Vault, AWS Secrets Manager, Kubernetes
Secrets with encryption at rest) and rotate them on a schedule.

## 2. Startup, liveness and readiness checks

Spring Boot Actuator is a dependency of every module and the probes are enabled with
`management.endpoint.health.probes.enabled=true`:

- **Startup** — has the application finished booting? Compose expresses it with `start_period: 40s`,
  which stops a slow JVM start from being mistaken for a failure. Spring services need it: they wait
  on the Config Server and register with Eureka before they are usable.
- **Liveness** (`/actuator/health/liveness`) — is the process still sane? A failing liveness check
  means the container cannot recover and must be restarted. It must *not* depend on another service,
  or one slow dependency restarts the whole estate.
- **Readiness** (`/actuator/health/readiness`) — can it serve traffic *now*? This one does account for
  dependencies: a transaction-service that cannot reach account-service is alive but not ready, so it
  should be pulled out of load balancing instead of restarted.

`docker-compose.yml` chains `depends_on: condition: service_healthy` so the Config Server is serving
before the business services boot, which matters because transaction-service reads its amount ceiling
from it.

## 3. Logs, metrics and distributed traces

- **Logs** — written to stdout so the runtime collects them; never to a file inside the container.
  They should be structured (JSON) in production and carry the correlation id so one request can be
  followed across services. The project already logs the decisive banking events: `EVENT NOT
  PUBLISHED`, `COMPENSATION FAILED`, and each consumed event.
- **Metrics** — Micrometer ships with Actuator; exposing `/actuator/prometheus` and scraping it gives
  request rates, latencies, JVM and pool statistics, plus the Resilience4J circuit-breaker state,
  which is the number that says whether account-service is being shielded.
- **Traces** — a single transfer touches the gateway, transaction-service, account-service and Kafka.
  Without a shared trace id a latency problem cannot be attributed. Micrometer Tracing with an
  OpenTelemetry exporter to Tempo or Jaeger propagates the context through the Feign calls and onto
  the Kafka headers.

The three answer different questions: metrics say *something is wrong*, traces say *where*, logs say
*what exactly*.

## 4. Rollback strategy

Every image is tagged with the commit SHA as well as `latest`, so the previous version is always
addressable and a rollback is a re-deploy of a known tag rather than a rebuild.

1. The deployment job waits for the health checks; a non-UP response fails the job.
2. On failure the `if: failure()` step re-deploys the previous SHA tag and re-runs the checks.
3. Services are stateless, so rolling back the application is enough.

The real constraint is data, not code: a rollback cannot undo a Kafka event already consumed, nor a
committed balance change. Schema changes must therefore be backwards-compatible (expand, migrate,
contract) so the previous version still runs against the new schema. For a safer path still, a
blue-green or canary release shifts a fraction of traffic first and makes the rollback a routing
change with no restart at all.
