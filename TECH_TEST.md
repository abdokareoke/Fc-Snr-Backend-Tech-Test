# Shift Scheduling Engine — Technical Assessment

## Overview

Build a production-ready Shift Scheduling REST API that allows managers to create schedules,
assign employees to shifts, and enforce real-world workforce scheduling constraints.

This is not a CRUD exercise.
We are looking for domain thinking, clean design, and engineering judgment.

---

## Table of Contents

- [The Domain](#the-domain)
- [What We Are Looking For](#what-we-are-looking-for)
- [Requirements](#requirements)
  - [1. Core Entities](#1-core-entities)
  - [2. API Endpoints](#2-api-endpoints)
  - [3. Business Rules](#3-business-rules)
  - [4. Conflict Response Format](#4-conflict-response-format)
- [Technical Requirements](#technical-requirements)
- [Seed Data](#seed-data)
- [What NOT To Do](#what-not-to-do)
- [How to Submit](#how-to-submit)
- [How We Will Evaluate You](#how-we-will-evaluate-you)
- [Debrief](#debrief)
- [Time Expectation](#time-expectation)
- [Questions](#questions)

---

## The Domain

A workforce scheduling system manages:

| Entity     | Description                                        |
|------------|----------------------------------------------------|
| Employee   | Has skills, availability, and contract constraints |
| Shift      | Needs to be covered by qualified employees         |
| Schedule   | Organizes shift assignments over a date range      |
| Assignment | Links an employee to a specific shift              |

---

## What We Are Looking For

- How you model a real business domain in code
- How you implement and isolate business rules
- How you think about data and query efficiency
- How you handle edge cases and failure scenarios
- How you communicate your decisions

---

## Requirements

### 1. Core Entities

#### Employee

| Field               | Description                                    |
|---------------------|------------------------------------------------|
| availabilityWindows | Mon-Fri 06:00-22:00 (ISO-8601 time strings or custom format) |
| skills              | List of strings (e.g. FORKLIFT_CERTIFIED, FIRST_AID, SUPERVISOR) |
| maxWeeklyHours      | e.g. 40 (int or double)      |
| contractType        | FULL_TIME or PART_TIME (Enum) |
| email               | Unique email address (String) |
| name                | Full name (String) |
| id                  | Unique identifier (String) |

#### Shift

| Field             | Description                                 |
|-------------------|---------------------------------------------|
| requiredSkills    | List of skills that assigned employees must possess |
| requiredHeadcount | Minimum number of employees needed (Int) |
| location          | Physical location of the shift (String) |
| endDatetime       | Shift end date and time (ISO-8601)          |
| startDatetime     | Shift start date and time (ISO-8601)        |
| id                | Unique identifier (String) |

#### Schedule

| Field     | Description                               |
|-----------|-------------------------------------------|
| name      | Schedule name                             |
| startDate | Schedule start date (ISO-8601)            |
| endDate   | Schedule end date (ISO-8601)              |
| shifts    | Collection of shifts within this schedule |
| id        | Unique identifier (String) |

#### Assignment

| Field      | Description           |
|------------|-----------------------|
| employeeId | The assigned employee |
| shiftId    | The target shift      |
| scheduleId | The parent schedule   |
| id         | Unique identifier (optional, if using composite keys) |

---

### 2. API Endpoints

Focus your energy here.
These are the only endpoints required.

#### Employees

```http
POST   /employees                  ->  Create employee (returns created employee with ID)
GET    /employees/{id}             ->  Get employee
GET    /employees/{id}/schedule    ->  Get assigned shifts for employee
Query param: ?week=2026-W14
```

#### Shifts

```http
POST   /shifts                     ->  Create shift (returns created shift with ID)
GET    /shifts/{id}                ->  Get shift
GET    /shifts?scheduleId={id}     ->  List shifts for a schedule
```

#### Schedules

```http
POST   /schedules                  ->  Create schedule (returns created schedule with ID)
GET    /schedules/{id}             ->  Get schedule with shifts and assignments
POST   /schedules/{id}/assign      ->  Manually assign employee to shift (201 Created or 400 Bad Request)
DELETE /schedules/{id}/assign      ->  Remove an assignment (requires `employeeId` and `shiftId` in body or query)
GET    /schedules/{id}/conflicts   ->  Get all rule violations in schedule
POST   /schedules/{id}/auto-assign ->  Automatically assign employees to shifts (returns updated schedule)
```

---

### 3. Business Rules

Your system must detect and enforce the following rules.
Each violation must be returned with a clear rule name and explanation.

This is the most important part of the assessment.
We are looking at how you structure, isolate, and test these rules
as much as whether they work correctly.

---

#### RULE-01 - No Overlapping Shifts

An employee cannot be assigned to two shifts whose times overlap.

---

#### RULE-02 - Minimum Rest Period

An employee must have at least 11 hours of rest between the end of one shift
and the start of the next.

---

#### RULE-03 - Weekly Hours Limit

An employee cannot be scheduled beyond their maximum weekly hours
within any 7-day rolling window (any continuous 168-hour period).

---

#### RULE-04 - Skill Match

An employee assigned to a shift must possess **all** skills required by that shift.
Assignments to shifts with no required skills are always valid for any employee.

---

#### RULE-05 - Minimum Headcount

A shift must have at least the required number of employees assigned
to be considered fully covered.

---

#### RULE-06 - Availability Matching

An employee can only be assigned to a shift if the shift's time window falls 
entirely within the employee's availability windows for that day.

---

### 4. Conflict Response Format

GET /schedules/{id}/conflicts must return:

```json
{
  "scheduleId": "sch-001",
  "evaluatedAt": "2026-03-30T09:00:00Z",
  "totalViolations": 3,
  "violations": [
    {
      "rule": "OVERLAP",
      "severity": "ERROR",
      "message": "Alice Johnson is assigned to overlapping shifts on 2026-03-30",
      "affectedEmployeeId": "emp-12",
      "affectedShiftIds": ["shf-44", "shf-45"]
    },
    {
      "rule": "REST_VIOLATION",
      "severity": "ERROR",
      "message": "Bob Smith has less than 11 hours rest before shift shf-67",
      "affectedEmployeeId": "emp-07",
      "affectedShiftIds": ["shf-66", "shf-67"]
    },
    {
      "rule": "UNDERSTAFFED",
      "severity": "WARNING",
      "message": "Shift shf-88 requires 3 employees but only 2 are assigned",
      "affectedShiftIds": ["shf-88"]
    }
  ]
}
```

**Severity levels:**
- **ERROR**: A hard rule violation that must be resolved (e.g. overlap, rest violation, skill mismatch, availability violation)
- **WARNING**: A coverage concern that should be reviewed (e.g. understaffed)

**Violation Fields:**
- `affectedEmployeeId`: Required for RULE-01, 02, 03, 04, 06.
- `affectedShiftIds`: Required for all rules. 
  - For RULE-01, 02: include both conflicting shifts.
  - For RULE-05: include the understaffed shift.

---

### 5. Auto-Assign Feature

Implement an endpoint that automatically assigns available and qualified employees to unassigned shifts within a schedule.

- The algorithm must respect all **6 business rules**.
- It should prioritize filling shifts that are currently understaffed.
- Assignments should be saved to the in-memory storage.

---

## Technical Requirements

| Requirement | Detail          |
|-------------|-----------------|
| Language    | Java 21+        |
| Framework   | Spring Boot 4+  |
| Storage     | In-memory only  |
| Build Tool  | Maven or Gradle |

### Storage Notes
There is no database requirement for this assessment.
Store all data in-memory using appropriate Java data structures.

- Data does not need to survive application restarts.
- We are not evaluating database knowledge here.
- We are evaluating how you model and manage data in code.
- How you structure your in-memory storage is part of the assessment.
- We will discuss persistence choices in the debrief.

---

## Expected Code Quality

- Clean separation between controller, service, and domain layers
- Business rules implemented as named, isolated, testable components
- No business logic inside controllers
- Meaningful error responses - no raw stack traces exposed
- **Unit tests for all 6 business rules** (Independent and comprehensive coverage)

---

## Seed Data

The application must load the following data automatically on startup.
This allows us to test your API immediately without manual setup.

### Employees

| ID | Name | Contract | Max Hours | Skills |
|----|------|----------|-----------|--------|
| emp-01 | Alice Johnson | FULL_TIME | 40h | SUPERVISOR, FIRST_AID |
| emp-02 | Bob Smith | FULL_TIME | 40h | FORKLIFT_CERTIFIED |
| emp-03 | Carol White | PART_TIME | 20h | FIRST_AID |
| emp-04 | David Brown | FULL_TIME | 40h | FORKLIFT_CERTIFIED, SUPERVISOR |
| emp-05 | Eva Martinez | PART_TIME | 20h | FIRST_AID, FORKLIFT_CERTIFIED |

### Shifts - Week of 2026-03-30 (Monday start)

| ID | Start | End | Location | Headcount | Required Skills |
|----|-------|-----|----------|-----------|-----------------|
| shf-01 | Mon 06:00 | Mon 14:00 | Warehouse A | 2 | FORKLIFT_CERTIFIED |
| shf-02 | Mon 14:00 | Mon 22:00 | Warehouse A | 2 | FORKLIFT_CERTIFIED |
| shf-03 | Mon 08:00 | Mon 16:00 | Office | 1 | SUPERVISOR |
| shf-04 | Tue 06:00 | Tue 14:00 | Warehouse A | 1 | FORKLIFT_CERTIFIED |
| shf-05 | Tue 22:00 | Wed 06:00 | Warehouse B | 2 | FIRST_AID |

---

## Conflict Scenarios to Verify

Use these to verify your business rules are working correctly.

### Scenario A - OVERLAP

```text
Assign emp-01 to shf-01  (Mon 06:00 - 14:00)
Assign emp-01 to shf-03  (Mon 08:00 - 16:00)
```

**Expected** -> `OVERLAP` violation for `emp-01`

### Scenario B - REST_VIOLATION

```text
Assign emp-02 to shf-02  (Mon 14:00 - 22:00)
Assign emp-02 to shf-04  (Tue 06:00 - 14:00)
```

**Gap** = 8 hours -> below the required 11 hours
**Expected** -> `REST_VIOLATION` for `emp-02`

### Scenario C - SKILL_MISMATCH

```text
Assign emp-03 to shf-01  (requires FORKLIFT_CERTIFIED)
emp-03 skills: FIRST_AID only
```

**Expected** -> `SKILL_MISMATCH` for `emp-03`

### Scenario D - UNDERSTAFFED

```text
Assign only emp-02 to shf-01  (requires headcount of 2)
```

**Expected** -> `UNDERSTAFFED` for `shf-01`

### Scenario E - AVAILABILITY_VIOLATION

```text
emp-01 availability: Mon-Fri 06:00-22:00
Assign emp-01 to a hypothetical shift on Sat 08:00-16:00
```

**Expected** -> `AVAILABILITY_VIOLATION` for `emp-01`

---

## What NOT To Do

Spending time on these will not improve your evaluation.

- Do not set up a database of any kind
- Do not implement authentication or user management
- Do not build a frontend
- Do not set up Docker
- Do not over-engineer - a working focused solution beats an incomplete ambitious one
- Do not wait on assumptions - state them in `SOLUTION.md` and move forward

---

## How to Submit

- Push your solution to a public GitHub repository
- Ensure the application starts with a single command
- Fill in `SOLUTION.md` with your answers and decisions
- Send us the repository link and add the provided GitHub usernames as collaborators for code review

---

## Final Interview - Debrief & Meet the Team

After reviewing your submission, and upon success we will invite you to a
3rd and final interview.

We will ask you to walk us through your key decisions and
discuss how you would extend the system.

**Topics we will cover:**

- Walk us through your auto-assign implementation and trade-offs
- Walk us through your conflict detection approach
- How would you replace in-memory storage with a real database?
- What schema would you design and why?
- How would your solution perform with 500 employees and 10,000 shifts?
- What would you change if this were a production system?

There is nothing to prepare.
We just want to talk through your thinking.

---

## Time Expectation

We estimate this will take 4 hours.

You are free and encouraged to use AI tools.

---

## Questions

If anything is genuinely ambiguous, email me at nicholas.wan@flightcentre.com

We prefer that you state your assumptions in `SOLUTION.md`
rather than wait for answers.

Good luck.
