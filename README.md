# Clinic Booking System

Final project for **Software Testing and Validation**, Addis Ababa University,
School of Information Technology and Engineering. Instructor: Abel Tadesse.

> The application itself carries no marks in this course - it exists only as a
> vehicle for the testing effort. See `docs/` for the test plan, test design
> document, defect log and metrics, and test summary report.

## Repository layout

```
clinic-booking/
├── backend/    Spring Boot app - the graded application: Thymeleaf pages, the
│               core business rules, and every unit/integration/system test.
│               Untouched by the frontend/ addition below - still the thing
│               Selenium drives and JaCoCo measures.
├── frontend/   A React (Vite) UI that talks to a small JSON API added under
│               backend/.../web/api. Purely a decorated alternative interface -
│               not part of the graded test suite, and has no tests of its own.
├── docs/       Test plan, test design doc, defect log, metrics, deliverable PDFs.
├── docker/     Jenkins-in-Docker setup.
└── Jenkinsfile, .github/workflows/ci.yml
```

Two ways to use the app: the original server-rendered pages at
`backend`'s `:8080` (what the automated test suite exercises), or the
decorated React UI in `frontend/` talking to the same backend over `/api/*`.
Both hit the same `AppointmentService` and the same H2 database - a booking
made in one is visible in the other.

## Group members

Solo submission (group size 1).

| Name | Student ID |
|------|------------|
| Biruk Mulatu | _fill in before submission_ |

## Stack

Java 17, Spring Boot 3.2.5, Thymeleaf, Spring Data JPA, H2 (in-memory),
JUnit 5, Mockito, AssertJ, Selenium 4.20, JaCoCo, Maven.

## Running the backend

```bash
cd backend
mvn spring-boot:run
```

Then open <http://localhost:8080> for the original server-rendered pages
(this is also what the React frontend below calls). Three demo patients are
seeded on startup (`DataSeeder`, disabled under the `test` profile so it
never touches the databases the automated tests use):

| Username | Password | Notes |
|----------|----------|-------|
| `abebe`  | `secret` | Adult, no outstanding balance |
| `selam`  | `secret` | Child |
| `almaz`  | `secret` | Senior, has an outstanding balance (booking is rejected) |

## Running the React frontend

```bash
cd frontend
npm install    # first time only
npm run dev
```

Open <http://localhost:5173>. The backend must already be running on
`:8080` - the frontend calls it directly at `http://localhost:5173/api/*`
proxied by nothing; CORS is opened for `localhost:5173` specifically in
`WebConfig`. Same demo accounts as above, plus a one-click "fill demo
account" shortcut on the login page.

## Running the tests

```bash
cd backend
mvn clean test      # unit tests only (fast, no browser needed)
mvn clean verify     # everything: unit + integration + Selenium system tests,
                      # plus the 80% branch-coverage gate on et.aau.clinic.core
```

`mvn clean verify` launches a real headless Chrome for the Selenium system
tests (Chrome must be installed locally; Selenium Manager finds it
automatically - no separate driver download needed). These all drive the
original Thymeleaf pages in `backend/` - the React frontend has no
automated tests of its own; it is a decorated UI addition, not part of the
graded suite.

Coverage report: `backend/target/site/jacoco/index.html` after `mvn clean verify`.

### Test layout

```
backend/src/test/java/et/aau/clinic/
├── unit/            fast, no Spring container - core/ business rules + AppointmentService with mocks
├── integration/      *IT - service+repository+H2, and controller tests via MockMvc
└── system/            *IT - Selenium, Page Object pattern (system/pages/)
```

## Continuous integration

### GitHub Actions

`.github/workflows/ci.yml` runs on every push and pull request: builds,
runs the full test suite, checks the coverage gate, and uploads the JaCoCo
report and JUnit/Failsafe XML reports as build artifacts. A coverage summary
table is also written to the workflow's job summary.

### Jenkins

A self-contained Jenkins with Maven and Chrome baked in (`docker/jenkins/`):

```bash
cd docker/jenkins
docker compose up --build
```

Then open <http://localhost:8080>, create a Pipeline job pointing at this
repository, and it will run the `Jenkinsfile` at the repo root - checkout,
`cd backend && mvn clean verify`, then publish the JUnit results and archive
the JaCoCo report. (Both the Jenkinsfile and `ci.yml` run the Maven build
from `backend/` - `frontend/` has no build step in either pipeline.)

## Defect log

Real defects found during development are logged in `docs/defect-log.md`
(all three predate the `frontend/` addition). The JSON API added under
`backend/.../web/api` for the React frontend is additive and untested - it
delegates to the same `AppointmentService` the graded suite already covers,
but the controllers and DTOs themselves have no dedicated tests.

## Project deliverables (PDF)

The four documents required alongside this repository are in `docs/deliverables/`:

- `01-test-plan.pdf` — Part A
- `02-test-design.pdf` — Part B (equivalence partitioning, boundary value analysis, decision
  table, state transition testing, with every derived case mapped to its test method)
- `03-defect-log-and-metrics.pdf` — Parts F and G
- `04-test-summary-and-reflection.pdf` — Parts H and I

Each `.pdf` is generated from the same-named `.html` file in that directory (via `wkhtmltopdf`);
edit the HTML and re-render if the content changes.
