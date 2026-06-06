# LLM_PROVIDER.md

This file provides guidance to LLMProvider Tooling (llm_provider.ai/code) when working with code in this repository.

## Commands

```bash
mvn javafx:run                                 # run the JavaFX desktop app
mvn test                                       # unit tests only (no DB / no Docker)
mvn verify -Pit                                # + repository integration tests (needs Docker)
mvn -Dtest=BorrowingPolicyTest test            # run one test class
mvn -Dtest=CirculationServiceTest#checkoutRejectsSuspendedPatron test   # run one test method
mvn compile                                    # compile only
mvn clean package                              # build a jar
./scripts/package.sh                           # native app image via jlink + jpackage
```

Note: incremental Maven builds have occasionally dropped a synthetic class
(e.g. an enum-switch `$1`) and caused a spurious `NoClassDefFoundError` in tests;
`mvn clean test` resolves it.

Unit tests (`domain.service`) do **not** require a database. The repository
integration tests (`repository.jdbc.*IT`) run only under the `it` profile and
require Docker — they spin up a real PostgreSQL via Testcontainers. Running the
app needs a reachable PostgreSQL per `application.properties` with the target
database already created (`createdb libradesk`); the schema is applied at startup
from `src/main/resources/db/schema.sql`.

Two non-obvious test details: the integration base class
(`repository/jdbc/AbstractRepositoryIT`) pins `api.version=1.41` because the
bundled docker-java negotiates an API version newer daemons reject, and it uses
second-aligned `LocalDateTime`s since PostgreSQL `TIMESTAMP` is microsecond
precision.

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
  root between the two top-level scenes (`showLogin()` / `showMain()`).
- **`controller/MainLayoutController`** — the shell after login; each sidebar
  button loads a feature view's FXML into the central content `StackPane`. Feature
  controllers (Dashboard/Catalog/Copies/Patrons/Circulation/Reservations) are
  no-arg and pull their service from `AppContext.get()`; the staff username for
  audit comes from `AppContext.get().getCurrentUser().getUsername()`. They surface
  service `ValidationException`s through `controller/Dialogs`. Human identifiers
  (membership number, barcode) are resolved to ids in the controller via service
  lookups before calling the workflow methods.

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
- **Runtime-editable settings go through `SettingsService`, not `AppConfig`.** It
  is the single source of truth for the loan period and borrowing limits: a value
  in the `settings` table (edited on the Settings screen) overrides the
  `application.properties` default. `CirculationService` reads limits/period from
  it. `AppConfig` only supplies the defaults.
- **OVERDUE is still outstanding.** `markOverdueLoans()` flips ACTIVE→OVERDUE, but
  the loan still counts against the patron's limit and can still be returned, so
  `JdbcLoanRepository`'s "active" queries match `status IN ('ACTIVE','OVERDUE')`
  while `findOverdue()` stays ACTIVE-only (idempotent sweep).
- **Background maintenance** runs on a daemon thread via
  `infrastructure/scheduling/MaintenanceScheduler` (interval `overdue.sweep.minutes`),
  started/stopped by `LibraDeskApplication`. Each tick runs `markOverdueLoans()`
  and `ReservationService.expireStaleReady(...)` (READY holds older than
  `reservation.ready.expiry.days` expire and the next patron is promoted — this is
  why reservations carry `ready_at`). The overdue sweep can also be triggered
  manually from Reports.
- **Fines** (`fines` table → `Fine`/`FineService`): an overdue return charges
  `fine.per.day × overdue days` via `CirculationService.returnByCopy`. Checkout is
  blocked when a patron's unpaid total exceeds `fine.block.threshold` (`0` =
  disabled). The Fines screen pays/waives. Money is `BigDecimal`;
  `SettingsService.getBigDecimal` reads the decimal settings (which are configured
  in `application.properties`, not the integer-only Settings grid).
- **Loan renewal** is `CirculationService.renew`: extends the due date one period
  unless the book has a pending reservation (`ReservationService.hasPending`).
- **CSV/PDF export** live in `infrastructure/export/` (`CsvService` via Apache
  Commons CSV, `PdfService` via OpenPDF). Both only map domain objects to a file;
  controllers gather the data and call them. CSV import loops over parsed rows and
  calls the services, catching per-row `ValidationException`s to build a summary.
- **Reports aggregates** (`ReportsService.mostBorrowed/activeLoansByPatronType/
  loansPerDay`) are computed in-memory from the repositories — the same approach as
  `DashboardService` — so there is no bespoke aggregate SQL; they return small DTO
  records (`NamedCount`, `DailyCount`) that the Reports charts bind to.
- **Audit viewer** reads `AuditLogService.recent()` and filters in-memory; it is the
  only reader of the audit trail (everything else only writes via `record(...)`).
- **Passwords** go through `util/PasswordHasher` (BCrypt). It still verifies
  legacy SHA-256 hashes so existing accounts work, and `AuthService` rehashes them
  to BCrypt on the next successful login (`needsRehash`). Account operations
  (change password, create staff, deactivate) live in `UserService`. New staff and
  the seeded admin carry `mustChangePassword`; `LoginController` forces the change
  before reaching the main layout.
- **Access control** is `domain/service/PermissionPolicy` (pure, like
  `BorrowingPolicy`): `UserRole → Set<Permission>`. Two enforcement layers:
  `MainLayoutController` hides sidebar entries via `AccessControl.can(...)`, and
  privileged controller actions call `AccessControl.require(...)` (which reads the
  session user from `AppContext`). Circulation/Reservations/Dashboard are
  ungated; CATALOG/PATRONS/REPORTS/FINES are LIBRARIAN+, SETTINGS/USERS/AUDIT are
  ADMIN-only.
- **Packaging** (`scripts/package.sh`) builds a jpackage `app-image` over a slim
  `jlink` runtime. The app runs JavaFX from the classpath, so the packaged entry
  point is `com.justin.libradesk.Launcher` (not `LibraDeskApplication`, which
  extends `Application` and can't be launched from the classpath). `mvn javafx:run`
  still uses `LibraDeskApplication`.

### Phase status (important when reading skeletons)

Roadmap Phases 6–9 are in progress (see `~/.llm_provider/plans/`). **Done:** Phase 6 —
role-based access control (`PermissionPolicy` + `AccessControl`, sidebar gating),
Users management screen, forced password change on first login; Phase 7 — fines
(charge on overdue return, block-over-threshold, Fines screen), loan renewal, and
READY-reservation expiry via `MaintenanceScheduler`; Phase 8 — audit-log viewer,
Reports charts (Bar/Line) and PDF export (OpenPDF). **Remaining:** Phase 9 —
GitHub Actions CI, jpackage installers, Flyway migrations, demo seed data. When
extending, follow the implemented repositories/services and the existing feature
controllers as the reference pattern.

## Language rules

All code, comments, identifiers, documentation, and commit messages are written
in **English**. (Project discussion with the user may be in Traditional Chinese,
but nothing in the repository should be.)
