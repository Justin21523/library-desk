# LLM_PROVIDER.md

This file provides guidance to LLMProvider Tooling (llm_provider.ai/code) when working with code in this repository.

## Commands

```bash
mvn javafx:run                                 # run the JavaFX desktop app
mvn test                                       # run all tests
mvn -Dtest=BorrowingPolicyTest test            # run one test class
mvn -Dtest=CirculationServiceTest#checkoutRejectsSuspendedPatron test   # run one test method
mvn compile                                    # compile only
mvn clean package                              # build a jar
```

Phase 1 tests (`domain.service`) do **not** require a database. Running the app
does: a PostgreSQL instance must be reachable per `application.properties`, and
the target database must already exist (`createdb libradesk`). The schema is
applied automatically at startup from `src/main/resources/db/schema.sql`.

## Architecture

Strict top-down layered architecture; dependencies point downward only and the
UI never reaches the database directly:

`JavaFX (FXML/CSS) → Controller → Service (domain.service) → Repository (interface + jdbc) → Domain model/enum`

Read these to understand the big picture quickly:

- **`config/AppContext`** — the composition root. It constructs the
  `DatabaseManager`, all JDBC repositories, and all services, and exposes them
  via a single static `AppContext.get()`. It also stores the signed-in `User`
  (the session). JavaFX instantiates FXML controllers with a no-arg constructor,
  so controllers pull their dependencies from `AppContext.get()` rather than via
  constructor injection. Services themselves use plain constructor injection and
  are wired only here.
- **`LibraDeskApplication`** — startup sequence: load config → open DB → apply
  schema → seed default `admin`/`admin` on an empty `users` table → initialise
  `AppContext` → show login. Wrap startup failures in a JavaFX `Alert`.
- **`controller/ViewNavigator`** — owns the primary `Stage` and swaps the scene
  root between top-level views (`showLogin()` / `showMain()`). Navigation goes
  through this, not between controllers.

### Conventions that span multiple files

- **Controllers contain no business logic.** They read inputs, call a service,
  and update the view. Rules live in `domain.service`.
- **Business rules are extracted as pure functions where possible.**
  `BorrowingPolicy` (suspended patron / borrowing limit) is dependency-free and
  unit-tested directly; `CirculationService` gathers inputs (repos + config +
  clock), delegates the decision to it, then applies side effects and audit.
- **Time is injected via `java.time.Clock`** into services so workflows are
  deterministically testable (see `CirculationServiceTest` with `Clock.fixed`).
- **Repositories are the only place with SQL.** Each entity has an interface in
  `repository/` and a `Jdbc*` implementation in `repository/jdbc/`. They convert
  `SQLException` into the unchecked `DataAccessException` so upper layers stay
  JDBC-free. JDBC `Statement.RETURN_GENERATED_KEYS` with `getLong("id")` is the
  pattern for inserts against `GENERATED ALWAYS AS IDENTITY` columns.
- **Validation/rule failures throw `ValidationException`** (unchecked); services
  throw it and controllers surface its message to the user.
- **Auditing goes through `AuditLogService.record(...)`**, never the audit
  repository directly, so the "what is auditable" policy stays in one place.
- **Config access is `AppConfig`**, which lets any property be overridden by an
  `UPPER_SNAKE_CASE` environment variable (e.g. `db.password` → `DB_PASSWORD`).
  Keep secrets in the environment, not in `application.properties`.

### Phase status (important when reading skeletons)

This is Phase 1. **Fully implemented:** `JdbcUserRepository`,
`JdbcPatronRepository`, `JdbcAuditLogRepository`, `AuthService`, `PatronService`,
`CirculationService.checkout`, `BorrowingPolicy`. **Intentional skeletons**
(marked `TODO(phase2)`): `JdbcBookRepository`, `JdbcBookCopyRepository`,
`JdbcLoanRepository`, `JdbcReservationRepository` (reads return empty, writes
throw `UnsupportedOperationException`), plus the return/overdue/reservation
workflows and the feature views behind the main-layout buttons. When extending,
follow the implemented repositories as the reference pattern.

## Language rules

All code, comments, identifiers, documentation, and commit messages are written
in **English**. (Project discussion with the user may be in Traditional Chinese,
but nothing in the repository should be.)
