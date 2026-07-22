# Requirement Normalization — Greenfield Scenario

## Raw Requirement (as given)
"Build a URL shortener service from scratch with core APIs, analytics, and reliability features."

## Interpreted Intent
The request is a clean-slate system build. Nothing exists yet, so there is no
backward-compatibility constraint, no data migration, and no legacy contract
to preserve. The phrase "core APIs, analytics, and reliability" implies three
non-negotiable capability groups, but does not specify implementation detail —
that ambiguity is intentional and is normalized below rather than treated as
missing information.

## Ambiguities Identified and Resolved
| # | Ambiguity | Resolution (assumption made) | Rationale |
|---|-----------|-------------------------------|-----------|
| 1 | What counts as "core APIs"? | Shorten, redirect, and fetch-analytics-by-code. Update/delete are out of scope for Phase 1. | Matches the minimum viable contract of every public URL shortener (bit.ly, tinyurl). |
| 2 | What "analytics" granularity is expected? | Aggregate click count + last-accessed timestamp per code. Per-click event logs (referrer, geo, device) are explicitly deferred. | Per-event analytics is a materially larger scope (needs a separate table, retention policy, and query layer) — flagged as a limitation, not silently built. |
| 3 | What does "reliability" mean here? | For Phase 1: input validation, expiry handling, collision-free code generation, clean error responses. Rate limiting, caching, and horizontal scaling are deferred to the brownfield scenario. | Keeps Phase 1 scoped to a working core; reliability hardening is explicitly the brownfield exercise later, so building it twice would be wasted effort. |
| 4 | Should short codes expire by default? | Yes — default 365-day expiry, configurable per request. | Unbounded-lifetime links are a common real-world abuse/security vector; defaulting to *some* expiry is a defensible, low-risk choice while remaining overridable. |
| 5 | Is authentication/ownership of links in scope? | No — out of scope for this prototype. | Not mentioned in the requirement; adding it would be scope invention, not interpretation. |

## Normalized Engineering Problem Statement
Build a stateless REST service exposing:
1. `POST /api/shorten` — accepts a long URL (+ optional expiry), returns a
   unique short code and shortened URL.
2. `GET /{code}` — redirects (HTTP 302) to the original URL if active and
   unexpired; increments a click counter as a side effect.
3. `GET /api/analytics/{code}` — returns click count, timestamps, and
   active/expired status for a given code.

Non-functional baseline: input validation on URL format, deterministic
collision-free code generation, graceful 404/410 handling for unknown or
expired codes, and a schema that supports future horizontal scaling
(surrogate key + indexed unique code column) without redesign.

## Explicitly Out of Scope (Phase 1)
- Authentication / per-user link ownership
- Rate limiting / abuse prevention (→ brownfield scenario)
- Per-click event analytics (referrer, device, geo)
- Custom/vanity short codes
- Bulk shorten API
