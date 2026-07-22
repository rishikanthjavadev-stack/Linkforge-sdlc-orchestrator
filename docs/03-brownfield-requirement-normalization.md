# Requirement Normalization — Brownfield Scenario

## Raw Requirement (as given, post-clarification)
"Add rate limiting to the existing URL shortener to protect against abusive
or excessive request volume from a single client." (Resolved from the
ambiguous scenario — see `02-ambiguous-requirement-normalization.md`.)

## Interpreted Intent
This is an additive, non-breaking enhancement to an already-running system.
Unlike the greenfield case, every design choice here must be evaluated
against the existing codebase: what it touches, what it risks breaking, and
what can be added without a redesign.

## Codebase Reasoning — Impacted Modules

| Layer | Existing component | Impact |
|---|---|---|
| Controller | `UrlShortenerController` (`POST /api/shorten`) | New pre-processing step needed before the method body executes — a filter/interceptor, not a code change inside the method itself, to keep rate limiting decoupled from business logic. |
| Controller | `RedirectController` (`GET /{code}`) | Same as above — redirects are the highest-traffic endpoint and the most likely abuse target (link enumeration, scraping). |
| Config | `application.yml` | New `app.rate-limit.*` properties needed (requests-per-window, window-size, scope: per-IP vs per-client-id). |
| New component | None existing — requires a new `RateLimitFilter` (Servlet `Filter` or Spring `HandlerInterceptor`) | Net-new addition; does not modify existing service/repository logic, which limits blast radius. |
| Cross-cutting | `GlobalExceptionHandler` | Needs a new exception type (`RateLimitExceededException`) and a `429 Too Many Requests` mapping, following the existing handler pattern already established for `ShortUrlNotFoundException` / `ShortUrlExpiredException`. |

**Not impacted:** `ShortUrl` entity/schema, `ShortUrlRepository`, `Base62Encoder`,
`ShortUrlService` business logic. This is deliberate — rate limiting is a
cross-cutting infrastructure concern and should not leak into domain logic.
Confirming this boundary *is* the architectural understanding this section
is meant to demonstrate.

## Design Options Considered
| Option | Trade-off |
|---|---|
| In-memory token bucket per IP (e.g. Bucket4j, or hand-rolled `ConcurrentHashMap`) | Simple, zero new infra. Fails to hold limits across multiple instances (breaks under horizontal scaling) — acceptable for a single-instance prototype, flagged as a scaling limitation. |
| Redis-backed distributed rate limiter | Correct under horizontal scaling. Adds an infrastructure dependency not justified for a prototype. |
| API gateway-level rate limiting (e.g. Spring Cloud Gateway, Nginx) | Correct separation of concerns for production, but moves the control outside the service being built — inappropriate for a prototype meant to demonstrate application-level engineering judgment. |

**Decision:** in-memory per-IP token bucket, scoped to the two public
endpoints, with the distributed-limits gap explicitly named as a known
limitation (see final engineering summary).

## Risk of This Change
- **False positives**: legitimate users behind a shared corporate NAT/IP
  could be throttled together. Mitigated by a generous default threshold
  and configurable override, not solved outright.
- **No persistence across restarts**: in-memory counters reset on deploy.
  Acceptable for a prototype; called out as a limitation.
- **Does not protect against distributed/botnet-style abuse** — only
  single-source abuse. Named explicitly rather than implied as "solved."

## Normalized Engineering Problem Statement
Introduce a stateless, per-IP token-bucket rate limiter as a servlet filter
in front of `/api/shorten` and `/{code}`, returning `429 Too Many Requests`
with a `Retry-After` header when exceeded, without modifying existing
service or persistence logic.
