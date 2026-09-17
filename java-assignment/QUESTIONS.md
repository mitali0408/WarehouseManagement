# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I'd refactor this — and while implementing this assignment I actually hit the
inconsistency firsthand, which made the case for me concretely rather than abstractly.

The codebase currently has three different data-access styles for three entities of
similar complexity:
  - Store: Panache active record (Store extends PanacheEntity, static Store.findById(...))
  - Product: Panache repository (a separate ProductRepository injected as a bean)
  - Warehouse: full hexagonal ports/adapters (a WarehouseStore interface, a
    WarehouseRepository adapter implementing both it and PanacheRepository, and a domain
    model — Warehouse — completely decoupled from the JPA entity, DbWarehouse)

When I built the bonus fulfillment feature, I initially assumed Product followed the same
active-record pattern as Store (Product.findById(...)) — it doesn't compile, because
Product only exposes a repository. That's a small thing, but it's exactly the kind of
friction an inconsistent codebase creates: every new contributor has to rediscover, per
entity, which access pattern applies, instead of it being a house rule.

I wouldn't force all three into full hexagonal separation, though — that would be
over-engineering for Store and Product, which are simple CRUD entities with no real
business rules to isolate from persistence concerns. I'd standardize on the *repository*
style (Product's approach) as the baseline for everything, and reserve the fuller
ports/domain-model split (Warehouse's approach) specifically for entities with actual
business logic worth testing in isolation.

The concrete argument for repository-over-active-record isn't stylistic — it's testability.
Because Warehouse's use cases depend on an injected WarehouseStore interface, I could
write CreateWarehouseUseCaseTest and ReplaceWarehouseUseCaseTest as plain JUnit+Mockito
tests: no Quarkus boot, no database, no Docker, running in milliseconds, each covering
5-7 distinct validation branches. Store and Product, by contrast, can only be tested
through @QuarkusTest — which, as this project's own history shows, is meaningfully more
fragile (we lost real time to Testcontainers/Docker API-version mismatches and a separate
JaCoCo-instrumentation gap that only affects @QuarkusTest-executed code). Every entity
that could be tested at the fast, environment-independent tier instead of the slow,
infrastructure-dependent one is a real, measurable win — not just a theoretical one.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
I ran into the practical cost of the generated approach directly while implementing
Warehouse's create endpoint. warehouse-openapi.yaml documents POST /warehouse as
returning 201, but the OpenAPI-generated interface fixes the method's return type to a
plain Warehouse bean, not jakarta.ws.rs.core.Response. That meant I couldn't just build a
custom Response with the right status — I had to fall back to injecting the underlying
Vert.x HttpServerResponse directly and setting the status code by hand, after two other
approaches (a RESTEasy Reactive @ResponseStatus annotation, then a @ServerResponseFilter)
silently didn't take effect. There was also real setup friction: the generated sources
land in target/generated-sources at build time, not in src/, so IntelliJ needed that
folder explicitly marked as a source root before anything would compile — an extra step
that isn't obvious the first time you open the project.

Pros of the generated approach (Warehouse): the YAML is the single source of truth, and
drift between documented contract and actual implementation is structurally
impossible — if the implementing class doesn't match the generated interface, the build
fails. It also gets you request/response bean classes, and would generate a working
Swagger/OpenAPI UI for consumers of the API, for free.

Cons: the extra build step is a real onboarding and debugging cost (nothing to grep for
in src/ when you're trying to understand what the generated interface actually declares),
and — as I found firsthand — the generated method signatures constrain how much control
you have over transport-level details like status codes, forcing workarounds for
anything the generator didn't anticipate.

Pros of the hand-coded approach (Product, Store): full, direct control over JAX-RS
behavior with no generator to fight; faster to read and modify since everything lives in
one file in src/; no separate toolchain step to understand or break.

Cons: nothing enforces that the code matches any documented contract, if one exists
elsewhere — the two can drift silently over time with no build-time signal.

My choice, for this specific codebase: I'd standardize on hand-coded across all three
entities, for the same consistency reasons as question 1 — Product and Store already
establish that precedent, and the generated approach's main selling point (contract
enforcement) matters most when the API has external consumers who need a reliable
machine-readable contract, which doesn't appear to be the case for this internal
monolith. If contract drift were a real concern, I'd get the enforcement benefit without
the runtime friction by using the OpenAPI spec purely as a CI-time validation step
(diffing the hand-written endpoints against a maintained spec) rather than for server-side
code generation.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
This is basically what the test suite in this project already does, and the shape of it
was driven directly by cost, not just theory.

I split tests into three tiers, weighted heavily toward the cheapest one:

1. Fast, environment-independent unit tests (plain JUnit + Mockito, no Quarkus boot, no
   database) for anything with real business logic — the Warehouse use cases
   (CreateWarehouseUseCase, ReplaceWarehouseUseCase, ArchiveWarehouseUseCase) and the
   Task 2 fix (StoreLegacySyncListener). This is where I put the bulk of edge-case
   coverage: every validation branch, every exception path, tested in isolation in
   milliseconds. This tier is also where I'd want new business-rule coverage to land by
   default going forward, since it's the cheapest to write, run, and maintain.

2. In-process REST-level tests (@QuarkusTest + RestAssured) for the handful of concerns
   that only exist once everything is wired together: HTTP status codes, JSON
   (de)serialization, routing, and exception-to-status-code mapping. I didn't re-test
   every business rule at this layer — that's what tier 1 is for — I focused on
   representative happy paths plus the specific error paths that prove the wiring (not
   the logic) is correct. This tier is where real problems actually surfaced during this
   assignment: I found that FulfillmentResource, the hand-written REST resource for the
   bonus feature, had zero coverage — the use-case tests called the use case directly via
   CDI and never touched the REST layer at all — and while writing a test for it, I
   caught a genuine bug (ProductNotFoundException/StoreNotFoundException weren't wired to
   any exception mapper, and would have silently returned 500 instead of 404). That's a
   concrete argument for why tier 2 matters even when tier 1 coverage is strong: they
   catch different classes of bugs.

3. A minimal packaged-artifact smoke test (@QuarkusIntegrationTest, WarehouseEndpointIT)
   for genuine deployment confidence — does the actual built jar boot and serve traffic.
   Kept intentionally small, since it's the slowest and least isolated tier: any failure
   here could stem from packaging, configuration, or environment, not just code.

The prioritization logic: push as much coverage as possible into tier 1, use tier 2 only
for what tier 1 structurally can't verify, and keep tier 3 to a handful of true smoke
tests. This isn't just a theoretical preference — I have first-hand cost data for it from
this exact assignment. Getting @QuarkusTest working reliably cost real time: Docker/
Testcontainers version negotiation failures against a modern Docker Engine, a Colima
socket-path mismatch, and then — after all of that was resolved — discovering that plain
JaCoCo doesn't properly instrument code only exercised through @QuarkusTest at all,
requiring the separate quarkus-jacoco extension to get an honest coverage number. None of
that friction touches tier 1 tests. Every business rule I could push down to that tier is
one less thing exposed to that whole class of environmental fragility.

On keeping coverage effective over time: a percentage threshold (I set JaCoCo's gate at
80% instruction coverage, excluding OpenAPI-generated code) is a necessary floor, not a
sufficient signal — it happily accepts code that's merely executed as a side effect of
another test, with no real assertions on its own behavior. That's exactly what happened
with FulfillmentResource before I caught it. Two things I'd do to guard against that
going forward: gate the check in CI (not just locally, so it can't be silently skipped),
and treat a sudden coverage drop on a PR as a stronger signal than the absolute number —
a drop means something got added without matching tests, which is the actual failure mode
worth catching early.
```