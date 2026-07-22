# LinkForge + SDLC Orchestrator

An agentic SDLC orchestration prototype: a URL shortener service (LinkForge)
built as the target system, and an orchestration layer (sdlc-orchestrator)
that drives it through requirements → design → implementation →
testing/documentation → release as a governed, stateful dependency graph
— with human approval gates, bounded retries, fallback, and rollback.

This repository contains two runnable Spring Boot services and full
supporting documentation, built and verified end-to-end.

## Repository structure
## Start here

1. **[docs/07-final-engineering-summary.md](docs/07-final-engineering-summary.md)**
   — plan, rationale, risks, trade-offs, assumptions, limitations
2. **[docs/04-orchestration-architecture.md](docs/04-orchestration-architecture.md)**
   — the orchestrator's design: graph shape, state machine, gates, retries,
   rollback, guardrails, metrics
3. **[docs/06-scenario-evidence.md](docs/06-scenario-evidence.md)**
   — real, captured request/response evidence from three live end-to-end
   runs (greenfield, ambiguous→brownfield, forced-rollback)

## Quick start

```bash
# Terminal 1 - the target system
cd linkforge
mvn spring-boot:run   # starts on :8080

# Terminal 2 - the orchestrator
cd sdlc-orchestrator
mvn spring-boot:run   # starts on :8081
```

See **[docs/05-demo-walkthrough.md](docs/05-demo-walkthrough.md)** for
copy-paste curl commands covering all three required scenarios.

## Requirement normalization

- [Greenfield](docs/01-greenfield-requirement-normalization.md)
- [Ambiguous](docs/02-ambiguous-requirement-normalization.md)
- [Brownfield](docs/03-brownfield-requirement-normalization.md)

## Testing

Both services have passing test suites, run independently:
```bash
cd linkforge && mvn test              # 14 tests
cd sdlc-orchestrator && mvn test      # 9 tests
```

## Notes

This was built and verified iteratively — including two real defects found
and fixed along the way (documented in
[docs/architecture-decisions.md](docs/architecture-decisions.md) and
[docs/07-final-engineering-summary.md](docs/07-final-engineering-summary.md)),
rather than presented as a frictionless first pass.
