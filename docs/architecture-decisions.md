# Architecture Decisions — Phase 1 Core

## 1. Short code generation: Base62(auto-increment id) vs random hash

**Chosen: Base62 encoding of a DB auto-increment id.**

| Approach | Uniqueness guarantee | Collision handling needed | Predictability risk |
|---|---|---|---|
| Base62(id) — **chosen** | Guaranteed by DB identity column | None | Codes are sequentially guessable/enumerable |
| Random N-char hash | Not guaranteed | Requires collision check + retry loop | Low — not sequential |

Chosen for simplicity and zero collision-handling code in a time-boxed
prototype. Enumerability is accepted as a known risk and named explicitly
(see risk register in the final summary) rather than silently ignored.
A production hardening path (id XOR-masking before encoding, or switching to
a random-hash-with-retry scheme) is noted but not implemented Phase 1.

## 2. Two-phase write (save → encode → save)

Because the short code is derived *from* the database-generated id, the
entity must be persisted once to obtain that id before the code can be
computed and persisted back. This is a direct consequence of decision #1 and
is called out here so it isn't mistaken for an oversight — it's a documented
trade-off of choosing counter-based encoding over pre-computed random codes.

## 3. Database: H2 (file-mode) for Phase 1, Postgres-ready schema

H2 was chosen to eliminate setup friction for a short prototype timeline.
The schema is written using standard JPA annotations with no H2-specific
features, so migrating to Postgres later is a configuration change
(`application.yml` datasource block), not a code or schema change.

## 4. Denormalized click count vs separate click-event table

`clickCount` and `lastAccessedAt` are stored directly on the `short_urls` row
rather than in a separate `click_events` table. This gives O(1) reads for
the analytics endpoint at the cost of losing per-event granularity
(referrer, timestamp-per-click, geo). Per-event analytics is named as an
explicit Phase 1 limitation, not attempted, because it changes the write path
(every redirect becomes an insert, not an update) and adds a query/retention
design surface that wasn't in the Phase 1 time budget.

## 5. Redirect endpoint at root path, not under /api

`GET /{code}` is intentionally placed outside the `/api` prefix used by the
other endpoints, because a short URL must be as short as possible
(`host/{code}`, not `host/api/{code}`) to fulfill the basic purpose of a URL
shortener. This does introduce a routing consideration: any future
`/api/**`-prefixed route is safe, but a single-segment top-level route (e.g.
a future `/health` endpoint) could theoretically collide with a generated
code. Documented here as a constraint for future route additions.

## 6. Defect found during manual verification: NOT NULL constraint on `code`

**Symptom:** `POST /api/shorten` returned `500` with H2 error `NULL not allowed
for column "CODE"`.

**Root cause:** Decision #2 (two-phase write) means the entity is persisted
once *before* the Base62 code is computed from its generated id. The `code`
column was originally declared `nullable = false`, so that first insert
violated its own schema.

**Fix:** relaxed `code` to `nullable = true` at the column level. Uniqueness
is still enforced via the existing unique index; the application layer
(not the schema) is now responsible for guaranteeing every row has a code
by the time it's queryable, since the transient null only exists within a
single `@Transactional` method body and is never visible to another
request.

**Why this wasn't caught in local review:** this is precisely the kind of
defect that only surfaces when the code actually runs against a real
database and constraint enforcement kicks in — it's a good example of why
"looks correct on read-through" isn't equivalent to "verified working," and
is called out explicitly here rather than quietly patched.

## 7. Validation strategy

URL format validation is done via a regex `@Pattern` constraint at the DTO
layer rather than attempting a full RFC 3986 parser. This is a deliberate
scope trade-off: it catches the overwhelming majority of malformed input
with negligible implementation cost, while edge-case exotic URL formats may
slip through. Named as a limitation rather than solved rigorously.
