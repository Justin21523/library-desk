# LibraDesk — Library Information Management System

LibraDesk is a desktop library management system built as a portfolio and
learning project. It demonstrates clean layered architecture, JavaFX UI
development, relational database design, enforceable business rules, and unit
testing in a realistic — but intentionally not over-engineered — codebase.

> **Status:** Phase 3 (reservations + feature UI). On top of Phases 1–2
> (foundation, persistence, check-out/check-in), the reservation hold queue is
> implemented end-to-end — returning a copy promotes the next patron and holds
> the copy for them — and the core feature screens are wired to the services:
> Dashboard, Catalog, Book Copies, Patrons, Circulation, and Reservations.
> Reports, Settings, and CSV import/export are delivered in later phases.

## Tech stack

- **Java 21**, **JavaFX 21** (FXML + CSS)
- **Maven** build
- **PostgreSQL** via **JDBC** with the **Repository Pattern**, pooled by **HikariCP**
- **JUnit 5** + **Mockito** for tests
- **SLF4J + Logback** for logging
- **Jackson** (settings/config) and **Apache Commons CSV** (import/export, later phase)

## Architecture

A strict top-down layered architecture; dependencies only point downward and the
UI never touches the database directly:

```
JavaFX UI (FXML + CSS)
      │
Controller  ── coordinates UI events only; no business logic
      │
Service     ── business rules, workflows, audit (domain.service)
      │
Repository  ── interfaces + JDBC implementations (the only SQL)
      │
Domain model + enums
```

Cross-cutting concerns live in dedicated packages: `infrastructure` (database,
export, logging), `validation`, `config`, and `util`.

Key points:

- **Business logic stays out of controllers.** Controllers call services.
- **The core borrowing rule is a pure function** (`BorrowingPolicy`) so it is
  trivially unit-testable; `CirculationService` orchestrates it with mocked
  repositories in tests.
- **`AppContext`** is the composition root that wires the object graph and holds
  the signed-in user (the session).

### Package layout

```
com.justin.libradesk
├── config            AppConfig, AppContext (composition root)
├── controller        JavaFX controllers + ViewNavigator
├── domain
│   ├── model         entities (Book, BookCopy, Patron, Loan, ...)
│   ├── enumtype      status/role enums
│   └── service       business services + BorrowingPolicy
├── dto               UI-facing data carriers
├── repository        repository interfaces
│   └── jdbc          JDBC implementations
├── validation        field validation + ValidationException
├── infrastructure
│   └── database      DatabaseManager, SchemaInitializer
└── util              PasswordHasher
```

## Prerequisites

- JDK 21
- Maven 3.9+
- A running PostgreSQL 14+ instance

## Database setup

Create the database and a user, then let the app create the tables on first run
(the schema script is idempotent):

```bash
createdb libradesk
psql -c "CREATE USER libradesk WITH PASSWORD 'libradesk';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE libradesk TO libradesk;"
```

Connection settings live in `src/main/resources/application.properties`. Any key
can be overridden by an environment variable in `UPPER_SNAKE_CASE`, e.g.:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/libradesk"
export DB_USER="libradesk"
export DB_PASSWORD="<secret>"
```

## Run

```bash
mvn javafx:run
```

On first launch the app seeds a default administrator account:

| Username | Password |
| -------- | -------- |
| `admin`  | `admin`  |

Change it immediately — it is intended only to bootstrap a fresh database.

## Test

```bash
mvn test                                   # unit tests only (no database/Docker)
mvn -Dtest=BorrowingPolicyTest test        # a single test class
mvn -Dtest=CirculationServiceTest#checkoutRejectsSuspendedPatron test
```

The unit tests (`BorrowingPolicyTest`, `CirculationServiceTest`) cover the
borrowing rule and the check-out/check-in workflows with mocked repositories, so
they need neither a database nor Docker.

### Integration tests

The JDBC repositories are verified against a real PostgreSQL started by
[Testcontainers](https://java.testcontainers.org/). They run only under the
`it` profile and **require Docker**:

```bash
mvn verify -Pit                            # unit tests + repository integration tests
```

> Note: the integration base class pins the Docker client API version
> (`api.version=1.41`) because the bundled docker-java client otherwise
> negotiates an API version that newer daemons (minimum 1.40) reject.

## License

Provided as-is for learning and portfolio purposes.
