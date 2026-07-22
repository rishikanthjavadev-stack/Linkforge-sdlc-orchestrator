# SDLC Orchestrator — Phase 2

Agentic workflow orchestration layer that drives the LinkForge repo
through requirements → design → implementation → testing/documentation
(parallel) → release, with human approval/clarification gates, bounded
retries, fallback, and rollback.

## Prerequisites
- Java 17+, Maven 3.8+
- The `linkforge` project from Phase 1 checked out as a sibling directory
  (default config assumes `../linkforge` relative to where you run this)

## Run
```bash
mvn spring-boot:run
```
Starts on `http://localhost:8081`.

If your LinkForge checkout is somewhere else:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--orchestrator.target-repo=/absolute/path/to/linkforge
```

## Run tests
```bash
mvn test
```
Tests use a JUnit `@TempDir` as an isolated target repo (not your real
LinkForge checkout), so they're safe to run repeatedly without side
effects on your actual project. Covers:
- `GraphFactoryTest` — graph shape, parallel branches, join point
- `PathGuardrailTest` — write-path policy enforcement (allow + block)
- `WorkflowEngineTest` — all three scenario behaviors end-to-end,
  including the ambiguity block/clarify/re-plan path and the
  approve/rollback path

## API quick reference
| Method | Path | Purpose |
|---|---|---|
| POST | `/workflows` | Start a workflow (`scenario`, `requirementText`, optional `simulateReleaseFailure`) |
| GET | `/workflows` | List all workflow instances |
| GET | `/workflows/{id}` | Full state: nodes, context, decision lineage |
| POST | `/workflows/{id}/nodes/{nodeId}/approve` | Approve a high-impact node (e.g. `release`) |
| POST | `/workflows/{id}/nodes/{nodeId}/clarify` | Resolve an ambiguous requirement |
| GET | `/workflows/{id}/audit` | Full audit trail |
| GET | `/workflows/{id}/metrics` | Success rate, retries, rollbacks, MTTR proxy, latency |

See `docs/05-demo-walkthrough.md` for full curl walkthroughs of all
three required scenarios.

## Note on this sandbox
Same constraint as Phase 1: this was built and reviewed in an environment
without Maven Central access, so `mvn test` couldn't be run here. Run it
on your machine as the first verification step.
