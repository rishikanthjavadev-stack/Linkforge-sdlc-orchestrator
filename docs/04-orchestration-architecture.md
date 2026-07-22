# Orchestration Architecture — Phase 2

## Purpose
This layer coordinates the SDLC lifecycle (requirements → design →
implementation → testing/documentation → release) as an explicit,
stateful dependency graph — not a linear prompt chain. It runs as its own
Spring Boot service (`sdlc-orchestrator`, port 8081) and drives real
actions against the LinkForge repo built on Phase 1: it writes real Java
files, runs real `mvn test`, and writes real release manifests. Nothing
here is a mocked simulation of orchestration behavior; the retries and
fallback described below engage because of real conditions (e.g. the
Maven Central access restriction this environment itself has).

## Graph Shape
All three required scenarios (greenfield, brownfield, ambiguous) share
one topology:

```
requirements → design → implementation → ┬─ testing ──────┐
                                          └─ documentation ─┴→ release
```

`testing` and `documentation` both depend only on `implementation` and
run **in parallel** on independent executor threads; `release` is a
**synchronization join** that only becomes eligible once both have
reached a terminal success state. This is the concrete meaning of
"non-linear, stateful execution" in the assignment brief: two branches
genuinely execute concurrently and must both complete before the graph
proceeds, rather than one task simply calling the next in sequence.

What differs between scenarios is **task behavior**, not graph shape:
- `ImplementationTask` verifies existing Phase 1 artifacts for greenfield,
  but generates real new source files (a rate-limiting servlet filter)
  for brownfield/ambiguous.
- `RequirementsTask` only produces an `AMBIGUOUS` outcome when the
  scenario is `AMBIGUOUS` and the input text matches known ambiguity
  signals (see `RequirementsTask.AMBIGUITY_SIGNALS`).

## State Machine
Each node moves through `NodeStatus`:

```
PENDING → RUNNING → COMPLETED
                  → FAILED → RETRYING → RUNNING        (bounded by maxRetries)
                  → FAILED → SAFE_STOPPED               (critical node, retries exhausted)
PENDING → AWAITING_APPROVAL → APPROVED → RUNNING → ...  (human approval gate)
PENDING → BLOCKED_CLARIFICATION → PENDING → RUNNING     (ambiguity gate, re-planned)
COMPLETED → ROLLED_BACK                                 (post-approval failure)
```

The workflow as a whole carries its own `WorkflowStatus`
(`RUNNING`, `AWAITING_HUMAN`, `COMPLETED`, `SAFE_STOPPED`, `FAILED`),
computed from the aggregate of node states on every `advance()` call.

## Entry/Exit Gates
- **Entry gate**: a node only becomes eligible to run when every node in
  its `dependsOn` set has reached `COMPLETED` or `SKIPPED`
  (`WorkflowEngine.depsSatisfied`).
- **Exit gate — human approval**: `release` is marked
  `requiresApproval=true` in `GraphFactory`. The engine transitions it to
  `AWAITING_APPROVAL` and **does not execute the task** until a human
  calls `POST /workflows/{id}/nodes/release/approve`. This is enforced in
  code (`advance()` branches on `isRequiresApproval()` before ever
  submitting the task to the executor), not merely by convention.
- **Exit gate — ambiguity/clarification**: `requirements` can return an
  `AMBIGUOUS` outcome, which the engine turns into
  `BLOCKED_CLARIFICATION` rather than guessing an interpretation. The
  workflow halts (`AWAITING_HUMAN`) until
  `POST /workflows/{id}/nodes/requirements/clarify` supplies a
  clarification, which re-queues the node as `PENDING` and lets it
  re-run with the clarified text now present in the shared context.

## Cross-Stage Context and Decision Lineage
`WorkflowContext` is the shared state object every task reads and writes.
It holds two distinct things:
- **`data`** — a flat key/value working set (e.g. `normalizedRequirement`,
  `designDecision`, `generatedFiles`) that later stages consume directly
  instead of re-deriving.
- **`decisions`** — an ordered, append-only `List<DecisionRecord>`
  capturing *why*, not just *what*: each entry has a stage, a decision
  string, a rationale string, and who made it (`AGENT` vs `HUMAN`). The
  `DocumentationTask` renders this list verbatim into the generated
  change-summary markdown, so the audit trail and the shipped
  documentation are provably the same source of truth, not two
  independently-written artifacts that could drift apart.

## Bounded Retries, Fallback, Rollback, Safe-Stop
- **Bounded retries**: each `NodeDefinition` carries a `maxRetries` value.
  `WorkflowEngine.runNode` loops up to `maxRetries + 1` attempts with a
  configurable backoff (`orchestrator.retry-backoff-ms`), recording a
  `RETRYING` transition and an audit event per attempt.
- **Fallback**: `TestingTask` attempts a real `mvn -q -B test` subprocess
  against the target repo. If it fails or times out (as it does in any
  sandbox without Maven Central access), the task falls back to static
  validation — confirming the files this run generated actually exist and
  are non-empty — and returns `FALLBACK_SUCCESS` with the degraded
  guarantee stated explicitly in the result message, not hidden.
- **Rollback**: if `release` (marked `critical=true`) exhausts its
  retries *after* a human has already approved it, the engine calls
  `ReleaseTask.rollback()`, which deletes the release manifest it had
  written, and marks the node `ROLLED_BACK` rather than `FAILED` — a
  distinct terminal state specifically so audit/metrics can distinguish
  "never succeeded" from "succeeded, then had to be undone."
- **Safe-stop**: if a `critical` node exhausts retries and isn't the
  release node (e.g. `implementation` in the greenfield scenario when the
  Phase 1 artifacts are missing), the whole workflow transitions to
  `SAFE_STOPPED` and `advance()` refuses to schedule anything further —
  proven by `WorkflowEngineTest.greenfieldScenario_failsFastOnMissingArtifacts_andSafeStops`.

## Policy Guardrails
`PathGuardrail.enforce()` is a real filesystem check (not a comment): it
canonicalizes both the allowed repo root and the candidate write path and
verifies the candidate is actually a descendant of the root. Every file
write in `ImplementationTask`, `DocumentationTask`, and `ReleaseTask`
routes through this check first, so a future task implementation with a
path-traversal bug (e.g. a generated filename containing `../../`) is
blocked at the guardrail rather than silently escaping the sanctioned
repo directory. `PathGuardrailTest` proves both the allow and block paths.

## Audit-Grade Observability
Every state transition — human or system-caused — appends an immutable
`AuditEvent` (timestamp, node, from-status, to-status, actor, message) to
`WorkflowInstance.auditLog`, exposed via
`GET /workflows/{id}/audit`. Nothing is overwritten or summarized away;
the full transition history for a workflow is reconstructable from this
list alone.

## Reliability Metrics
`GET /workflows/{id}/metrics` computes, from the same node state that
drives execution (not a separately-maintained counter that could drift):
- **success rate**: completed nodes ÷ total nodes
- **retry frequency**: total retry attempts across all nodes
- **rollback frequency**: count of nodes in `ROLLED_BACK` state
- **MTTR proxy**: mean duration of nodes that needed more than one
  attempt (documented as a proxy, not a true incident-recovery MTTR,
  since this prototype doesn't model a running production incident)
- **end-to-end latency**: wall-clock time from workflow creation to
  completion (or "now" if still in flight)

## Dynamic Re-Planning
The ambiguous scenario is the concrete demonstration: `requirements`
returns `AMBIGUOUS`, the workflow halts, and no downstream node has
started yet. When a human calls `/clarify`, the engine re-queues
`requirements` as `PENDING`, it re-executes with the clarified text now
in context, and `advance()` naturally cascades forward exactly as if the
clarified text had been the original input — no separate "replan" code
path is needed because the same `advance()` loop that drives normal
progress also drives resumption after a gate. If a later version of this
prototype needed to invalidate *already-completed* downstream nodes on
upstream change (not just not-yet-started ones), the natural extension is
to reset their `NodeStatus` back to `PENDING` and let `advance()` re-drive
them — the state machine already supports arbitrary re-entry into
`PENDING`, this just isn't exercised by the current scenarios since none
of them change a requirement after implementation has already run.

## Controlled Autonomy — Concretely
Agents (`StageTask` implementations) execute `requirements`, `design`,
`implementation`, `testing`, and `documentation` autonomously. Humans are
required, by code, at exactly two points: resolving an ambiguous
requirement, and approving the release action. This maps directly to
the "Agents execute under defined autonomy boundaries; humans own
oversight, approvals, and final quality" principle in the assignment —
autonomy is the default, and the two human checkpoints are the
exceptions, not the other way around.
