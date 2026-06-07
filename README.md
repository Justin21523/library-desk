# LibraDesk — Library Information Management System

LibraDesk is a desktop library management system built as a portfolio and
learning project. It demonstrates clean layered architecture, JavaFX UI
development, relational database design, enforceable business rules, and unit
testing in a realistic — but intentionally not over-engineered — codebase.

> **Status:** Phase 13 (cataloging — OPAC) — the cataloging track (10–13) is
> complete. A reader-facing **Catalog Search** screen offers keyword search across
> title/author/subject/ISBN, **facet narrowing** (author, subject, year, language,
> material type), and a full record detail. Earlier cataloging phases added MARC21
> fields + import/export, Library of Congress copy cataloging, authority see-from
> variants, DDC/LCC call numbers, and spine labels. A standalone public OPAC and
> **BIBFRAME** remain noted future directions.

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

Create the database and a user; the app applies the schema on first run via
Flyway migrations (`src/main/resources/db/migration`):

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

This account is flagged to require a password change on first login, so you will
be prompted to set a new password before reaching the main screen.

To populate a fresh database with sample data for evaluation, run with
`DEMO_SEED=true` (it only seeds when the catalog is empty):

```bash
DEMO_SEED=true mvn javafx:run
```

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

## Packaging

Build a self-contained native application image (a slim jlink runtime + the app)
with:

```bash
./scripts/package.sh
# launch: target/jpackage/image/LibraDesk/bin/LibraDesk
```

The script builds the jar, collects runtime dependencies, creates a trimmed JDK
runtime with `jlink`, and assembles the image with `jpackage` (`--type
app-image`). JavaFX and the other libraries run from the classpath, so the
packaged app uses `com.justin.libradesk.Launcher` as its entry point (a launcher
that does not extend `Application`). Requires JDK 21 with `jlink`/`jpackage` on
the `PATH`. When `dpkg-deb` and `fakeroot` are present, the script also builds a
`.deb` installer into `target/jpackage/installer/`.

## Continuous integration

`.github/workflows/ci.yml` runs `mvn verify -Pit` on every push/PR
(ubuntu-latest, JDK 21, Maven cache). The runner's preinstalled Docker lets the
Testcontainers integration tests run unchanged.

> The jlink module list in the script covers the app's needs (JavaFX, JDBC,
> logging, BCrypt). If you enable TLS to PostgreSQL or other JDK features, run
> `jdeps` against the dependencies and extend `--add-modules` accordingly.

## License

Provided as-is for learning and portfolio purposes.
