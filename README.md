# Clinic Booking System

Final project for **Software Testing and Validation**, Addis Ababa University,
School of Information Technology and Engineering. Instructor: Abel Tadesse.

> The application itself carries no marks in this course - it exists only as a
> vehicle for the testing effort. See `docs/` for the test plan, test design
> document, defect log and metrics, and test summary report.

## Group members

Solo submission (group size 1).

| Name | Student ID |
|------|------------|
| Biruk Mulatu | _fill in before submission_ |

## Stack

Java 17, Spring Boot 3.2.5, Thymeleaf, Spring Data JPA, H2 (in-memory),
JUnit 5, Mockito, AssertJ, Selenium 4.20, JaCoCo, Maven.

## Running the application

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080>. Three demo patients are seeded on startup
(`DataSeeder`, disabled under the `test` profile so it never touches the
databases the automated tests use):

| Username | Password | Notes |
|----------|----------|-------|
| `abebe`  | `secret` | Adult, no outstanding balance |
| `selam`  | `secret` | Child |
| `almaz`  | `secret` | Senior, has an outstanding balance (booking is rejected) |

## Running the tests

```bash
mvn clean test      # unit tests only (fast, no browser needed)
mvn clean verify     # everything: unit + integration + Selenium system tests,
                      # plus the 80% branch-coverage gate on et.aau.clinic.core
```

`mvn clean verify` launches a real headless Chrome for the Selenium system
tests (Chrome must be installed locally; Selenium Manager finds it
automatically - no separate driver download needed).

Coverage report: `target/site/jacoco/index.html` after `mvn clean verify`.

### Test layout

```
src/test/java/et/aau/clinic/
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
`mvn clean verify`, then publish the JUnit results and archive the JaCoCo
report.

## Defect log

Real defects found during development are logged in `docs/defect-log.md`.

## Project deliverables (PDF)

The four documents required alongside this repository are in `docs/deliverables/`:

- `01-test-plan.pdf` — Part A
- `02-test-design.pdf` — Part B (equivalence partitioning, boundary value analysis, decision
  table, state transition testing, with every derived case mapped to its test method)
- `03-defect-log-and-metrics.pdf` — Parts F and G
- `04-test-summary-and-reflection.pdf` — Parts H and I

Each `.pdf` is generated from the same-named `.html` file in that directory (via `wkhtmltopdf`);
edit the HTML and re-render if the content changes.
