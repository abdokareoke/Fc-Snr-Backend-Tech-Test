# Solution

## Architecture Overview

The application follows a clean three-layer architecture:

```
controller/        → Thin REST layer. Receives HTTP, validates input, delegates to services, returns responses.
service/           → Business logic. Orchestrates repositories and the rule engine. No HTTP concerns.
repository/        → Data access abstraction. Interface + in-memory implementation.
rules/             → Isolated business rule engine. Each rule is a named, testable component.
model/             → Domain, Core entities, enums, and value objects.
exception/         → Custom exceptions mapped to HTTP status codes.
dto/               → Request DTOs for API input validation.
seed/              → Startup data loader.
```

Services depend on repository interfaces, not their implementations. Swapping to JPA requires changing the repository layer and adding annotations to models, but no structural changes nor changes to service or controller.

## Business Rules Design

Each of the 6 rules is implemented as a standalone `@Component` implementing the `BusinessRule` interface:

```java
public interface BusinessRule {
    List<Violation> evaluate(RuleContext context);
    String getRuleName();
}
```

**Why this pattern:**
- **Isolated** — each rule has a single responsibility. A bug in skill matching cannot affect rest period checks.
- **Testable** — each rule is unit tested independently with minimal setup.
- **Extensible** — adding a 7th rule means creating one new file. The `RuleEvaluator` discovers it automatically via Spring's dependency injection. No existing code changes.
- **Named** — each rule owns its identifier (`"OVERLAP"`, `"SKILL_MISMATCH"`, etc.) which appears directly in violation responses.

`RuleContext` is a Java `record` that bundles everything a rule needs — the candidate assignment, employee, shift, existing assignments, and pre-computed lookup maps. This avoids passing many parameters and ensures rules are pure functions: given this context, return violations.

Rules run synchronously. The isolated `BusinessRule` interface means switching to async execution — via `CompletableFuture` with a dedicated thread pool — requires changing only `RuleEvaluator`, not any individual rule. I chose synchronous execution because of 2 main reasons: 

1. The rules are in-memory and fast, and premature async adds complexity without measurable benefit at this scale. 
2. Quickest approach was the .parallelStream() which uses the JVM's ForkJoinPool. In a real life production scenario, this will create thread starvation causing requests to queue up. 

## RULE-03 (Weekly Hours) — Detailed Design Notes

The rolling 7-day window is the trickiest rule. Here is the reasoning behind the current implementation and the alternatives considered:

**Current approach:** The window is defined as the 168 hours leading up to and including the candidate shift. All existing shifts that touch this window are counted in full.

**Known limitation — no partial shifts:** If a shift straddles the window boundary (e.g. the window starts at 06:00 and a shift runs 04:00-12:00), the full shift duration is counted rather than just the portion within the window. This is a conservative overcount by design — it may flag a violation where precise prorating would not.

**Known limitation — single-window check:** The current implementation checks one window (168h lookback from the candidate shift). A truly exhaustive rolling window check would place the candidate shift in every possible position within a 7-day window and find the worst case. This would require iterating all possible window placements, which the **sliding window sum algorithm** can solve efficiently.

**Sliding window sum algorithm (considered, not implemented):** I considered a sliding window sum approach — iterating a 14-day range and computing all seven possible 7-day window sums in O(n) using the prefix subtraction technique. I decided against implementing it for two reasons: first, our shift data isn't bucketed into daily slots so converting it would add unnecessary complexity; second, the spec only requires checking whether adding a single candidate shift violates the limit, not finding the worst-case window across a full month. The simpler single-window check is correct for the given requirements, and the architecture makes it trivial to swap in a more sophisticated algorithm later if the requirements change.

## Auto-Assign Implementation

The auto-assign algorithm uses a greedy approach:

1. Sort shifts by how understaffed they are (most understaffed first)
2. For each understaffed shift, iterate all employees
3. For each employee, build a `RuleContext` and evaluate all 6 rules
4. If no ERROR-level violations, save the assignment
5. Continue until the shift reaches its required headcount or no valid candidates remain

**Trade-offs:**
- Greedy is not optimal — it may miss globally better configurations. An optimal solution would require backtracking or constraint satisfaction, which is significantly more complex.
- The algorithm prioritises coverage (filling shifts) over fairness (balancing hours across employees). A production system might additionally sort employees by remaining weekly capacity.
- Time complexity is O(shifts × employees × rules) which is acceptable at the current scale but would need optimisation for large datasets.

## Conflict Detection Approach

`GET /schedules/{id}/conflicts` evaluates every existing assignment in the schedule as if it were a candidate:

1. Load all assignments and shifts for the schedule
2. For each assignment, build a `RuleContext` excluding the assignment itself from `existingAssignments`
3. Run all 6 rules
4. Collect violations, deduplicate, and return

Additionally, completely unstaffed shifts (zero assignments) are flagged as `UNDERSTAFFED` warnings since no assignment evaluation would catch them.

The same rule engine is used for both assignment validation (`POST /assign`) and conflict detection (`GET /conflicts`) — ensuring consistency between what gets blocked and what gets reported.

## Assumptions Made

### Data Modelling

1. **Availability windows**: The spec says "Mon-Fri 06:00-22:00" for all seed employees. I modelled this as `List<AvailabilityWindow>` with per-day entries using `DayOfWeek` + `LocalTime`. This allows different availability per day and supports future extension to multiple windows per day.

2. **Skills as enum**: Skills are modelled as a Java enum for type safety in this assessment. In production, skills would be a database table with management endpoints since the valid skill set is business data that changes without code deployments.

3. **Seed schedule**: A schedule (`sch-01`) is seeded alongside employees and shifts to allow immediate testing of assignment, conflict, and auto-assign endpoints without manual setup.

### Business Rule Interpretations

4. **RULE-03 — No partial shift prorating**: The weekly hours rule counts the full duration of shifts that overlap the 168-hour window boundary rather than prorating to the exact portion within the window. This is conservative — it may flag a violation where precise prorating would not. See "RULE-03 Detailed Design Notes" above for the full rationale.

5. **RULE-03 — Single-window lookback**: The rolling window checks the 168 hours leading up to the candidate shift, not every possible 7-day window. This is a pragmatic simplification — the sliding window sum algorithm was considered but not implemented (see above).

6. **RULE-05 — Severity**: Understaffed is a WARNING, not an ERROR. Assignments are not blocked if the shift remains understaffed — a manager may be building the schedule incrementally.

7. **RULE-06 — Overnight shifts**: Shifts spanning midnight are validated against availability on both days. A shift from Tue 22:00 to Wed 06:00 requires availability on Tuesday evening AND Wednesday morning.

## Path to Production

### Database Schema

```sql
CREATE TABLE employees (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    contract_type VARCHAR(20) NOT NULL,
    max_weekly_hours DECIMAL(4,1) NOT NULL
);

CREATE TABLE employee_skills (
    employee_id VARCHAR(36) REFERENCES employees(id),
    skill       VARCHAR(50) NOT NULL,
    PRIMARY KEY (employee_id, skill)
);

CREATE TABLE availability_windows (
    id          SERIAL PRIMARY KEY,
    employee_id VARCHAR(36) REFERENCES employees(id),
    day_of_week VARCHAR(10) NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL
);

CREATE TABLE shifts (
    id               VARCHAR(36) PRIMARY KEY,
    location         VARCHAR(255) NOT NULL,
    start_datetime   TIMESTAMP NOT NULL,
    end_datetime     TIMESTAMP NOT NULL,
    required_headcount INT NOT NULL
);

CREATE TABLE shift_required_skills (
    shift_id VARCHAR(36) REFERENCES shifts(id),
    skill    VARCHAR(50) NOT NULL,
    PRIMARY KEY (shift_id, skill)
);

CREATE TABLE schedules (
    id         VARCHAR(36) PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date   DATE NOT NULL
);

CREATE TABLE schedule_shifts (
    schedule_id VARCHAR(36) REFERENCES schedules(id),
    shift_id    VARCHAR(36) REFERENCES shifts(id),
    PRIMARY KEY (schedule_id, shift_id)
);

CREATE TABLE assignments (
    id          VARCHAR(36) PRIMARY KEY,
    schedule_id VARCHAR(36) REFERENCES schedules(id),
    shift_id    VARCHAR(36) REFERENCES shifts(id),
    employee_id VARCHAR(36) REFERENCES employees(id),
    UNIQUE (schedule_id, shift_id, employee_id)
);
```

Key indexes:
- `assignments(employee_id)` — for rule evaluation (RULE-01, 02, 03)
- `assignments(schedule_id)` — for conflict detection
- `assignments(shift_id)` — for headcount checks
- `shifts(start_datetime, end_datetime)` — for time-range queries

### Performance at Scale (500 employees, 10,000 shifts)

The current in-memory approach with indexed lookups would actually handle this scale fine — the bottleneck would be the auto-assign algorithm at O(shifts × employees × rules).

For a database-backed system:
- Rule evaluation would batch-fetch assignments per employee rather than querying per rule
- Auto-assign could be parallelised per shift since shifts are independent
- Conflict detection could use SQL queries to pre-filter (e.g. find overlapping shifts via time-range intersection) rather than loading all assignments into memory
- Caching (e.g. employee skills, availability) would reduce repeated lookups

## What I Would Change

With more time or in a production context:

- **Prorated weekly hours**: Calculate the exact overlap between shifts and the 168-hour window instead of counting full durations.
- **Skills as data**: Replace the enum with a managed entity and CRUD endpoints.
- **Audit trail**: Record who assigned whom and when, with the ability to undo.
- **Smarter auto-assign**: Consider employee preferences, fairness in hour distribution, and backtracking for better global coverage.
- **API pagination**: List endpoints would return paginated results for large datasets.
- **OpenAPI/Swagger documentation**: Auto-generated API docs from annotations.


## Testing with Postman

A Postman collection is included at `postman/Shift-Scheduler-API.postman_collection.json`.

Import it into Postman (File → Import) and run requests top-to-bottom. The collection is structured as a narrative that walks through the full API:

1. **Verify Seed Data** — confirm employees, shifts, and schedule are loaded
2. **Valid Assignments** — assign qualified employees to shifts (201 Created)
3. **Scenario A** — OVERLAP violation (Alice on two overlapping Monday shifts)
4. **Scenario B** — REST_VIOLATION (Bob with only 8h rest between shifts)
5. **Scenario C** — SKILL_MISMATCH (Carol without FORKLIFT_CERTIFIED)
6. **Scenario D** — UNDERSTAFFED warnings via the conflicts endpoint
7. **Scenario E** — AVAILABILITY_VIOLATION (Saturday shift for Mon-Fri employee)
8. **Auto-Assign** — system fills remaining shifts + verify conflicts reduced
9. **Cleanup** — demonstrate removing an assignment
10. **Employee Schedule View** — per-employee shift query with week filter

Prerequisites: start the server with `mvn spring-boot:run` before running the collection.
