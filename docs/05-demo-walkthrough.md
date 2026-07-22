# Demo Walkthrough — All 3 Scenarios

Run LinkForge (port 8080) and sdlc-orchestrator (port 8081) in separate
terminals first. The orchestrator's `orchestrator.target-repo` config
points at `../linkforge` by default — run `mvn spring-boot:run` for the
orchestrator from a directory laid out as:
```
day1-linkforge/
├── linkforge/         (Phase 1 core)
└── sdlc-orchestrator/ (Phase 2 - run from here)
```

## Scenario 1 — Greenfield

```bash
curl -X POST http://localhost:8081/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "scenario": "GREENFIELD",
    "requirementText": "Build a URL shortener from scratch with core APIs, analytics, and reliability features."
  }'
```

Response includes a workflow `id`. `requirements` and `design` complete
immediately (no ambiguity signals matched). `implementation` verifies the
five core LinkForge artifacts actually exist on disk. `testing` and
`documentation` then run in parallel. `release` will pause at
`AWAITING_APPROVAL` — approve it:

```bash
curl -X POST http://localhost:8081/workflows/<id>/nodes/release/approve \
  -H "Content-Type: application/json" \
  -d '{"note": "Reviewed test/doc output, approved for release"}'
```

Check final state and metrics:
```bash
curl http://localhost:8081/workflows/<id>
curl http://localhost:8081/workflows/<id>/metrics
```

## Scenario 2 — Ambiguous → Brownfield (re-planning demo)

```bash
curl -X POST http://localhost:8081/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "scenario": "AMBIGUOUS",
    "requirementText": "Make the URL shortener more reliable."
  }'
```

The `requirements` node will land in `BLOCKED_CLARIFICATION` and the
workflow status becomes `AWAITING_HUMAN`. Confirm:
```bash
curl http://localhost:8081/workflows/<id>
```
You'll see `requirements.status = BLOCKED_CLARIFICATION` and a message
listing the competing interpretations. Resolve it:

```bash
curl -X POST http://localhost:8081/workflows/<id>/nodes/requirements/clarify \
  -H "Content-Type: application/json" \
  -d '{"note": "Add per-IP rate limiting to protect against traffic abuse"}'
```

This is the re-planning trigger: `requirements` re-executes with the
clarified text, and the graph cascades forward exactly like the
brownfield scenario below — `implementation` will generate a real
`RateLimitFilter.java` inside your LinkForge repo. Check it:

```bash
ls ../linkforge/src/main/java/com/rishikanth/linkforge/filter/
```

Then approve the release the same way as Scenario 1.

## Scenario 3 — Brownfield with a forced rollback (demo hook)

```bash
curl -X POST http://localhost:8081/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "scenario": "BROWNFIELD",
    "requirementText": "Add rate limiting to protect against abuse",
    "simulateReleaseFailure": true
  }'
```

Approve the release once it reaches `AWAITING_APPROVAL`:
```bash
curl -X POST http://localhost:8081/workflows/<id>/nodes/release/approve \
  -H "Content-Type: application/json" \
  -d '{"note": "Approving to demonstrate rollback"}'
```

Because `simulateReleaseFailure` is set, `ReleaseTask` deliberately fails
even after approval. Watch the release node exhaust its retries and roll
back:
```bash
curl http://localhost:8081/workflows/<id>
```
`release.status` should read `ROLLED_BACK` and the overall workflow
`SAFE_STOPPED`. Confirm no manifest file was left behind:
```bash
ls ../linkforge/docs/generated/ | grep <id>
# should show nothing for -release-manifest.json
```

## Inspecting the full audit trail for any run
```bash
curl http://localhost:8081/workflows/<id>/audit
```
Every transition - system, agent, or human-caused - is listed with a
timestamp and message, enough to reconstruct exactly what happened and
why without re-running anything.
