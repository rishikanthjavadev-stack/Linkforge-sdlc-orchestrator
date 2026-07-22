# Requirement Normalization — Ambiguous Scenario

## Raw Requirement (as given)
"Make the URL shortener more reliable."

This is the deliberately underspecified input used to demonstrate the
orchestrator's ability to detect ambiguity and stop for clarification rather
than silently guessing at scope.

## Why This Is Ambiguous
"Reliable" is a quality attribute with no agreed single meaning. At least
four materially different engineering problems hide behind this one word:

| Interpretation | What it would actually require | Effort/risk profile |
|---|---|---|
| Reliable against traffic spikes / abuse | Rate limiting, request throttling per IP/client | Medium — new middleware, new failure modes (false-positive blocking) |
| Reliable against data loss | DB replication, backups, transactional integrity review | High — infra-level change, out of a single service's control |
| Reliable against bad input | Stronger validation, defensive null/edge-case handling | Low — contained to existing code |
| Reliable against downstream failure | Circuit breakers, retries, fallback redirect pages | Medium — new resilience library, new configuration surface |
| Reliable as in "high availability" | Multi-instance deployment, load balancing, health checks | High — deployment/infra change, not just app code |

Proceeding on any single interpretation without confirming would mean the
agent silently narrows scope on the human's behalf — precisely the failure
mode the orchestrator's human-approval-checkpoint requirement exists to
prevent.

## What the Orchestrator Should Do (Behavior, Not Guess)
1. **Requirements-stage gate fails validation** — the stage output is marked
   `BLOCKED: ambiguous` rather than allowed to pass through as if it were a
   normal requirement.
2. **Surface the ambiguity explicitly** to the human, using the table above
   (or an equivalent generated at run time) as the clarification prompt.
3. **Do not auto-select a "most likely" interpretation and proceed** — even
   though "rate limiting" is arguably the most common real-world reading,
   choosing it silently would be scope invention, not requirement
   understanding.
4. **Resume only after human input** — once a human picks (or narrows) an
   interpretation, the requirements stage re-runs with that constraint
   attached, and the graph proceeds normally from there (this is the
   re-planning behavior described in the workflow orchestration requirement).

## Simulated Resolution (for demo purposes)
For the purposes of this prototype's demo run, we simulate the human
selecting: **"reliable against traffic spikes / abuse."** This becomes the
brownfield scenario's actual requirement (see
`03-brownfield-requirement-normalization.md`), which lets one clarified
ambiguous requirement flow directly into a concrete, buildable brownfield
change — showing continuity between the two scenarios rather than treating
them as disconnected demos.

## Normalized Engineering Problem Statement (post-clarification)
Add rate limiting to the existing `POST /api/shorten` and `GET /{code}`
endpoints to protect against abusive or excessive request volume from a
single client, without degrading the experience for normal traffic levels.
