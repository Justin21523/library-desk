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
by **Flyway** (`infrastructure/database/FlywayMigrator`) from
`src/main/resources/db/migration` (the ITs run the same migrator). CI is
`.github/workflows/ci.yml` (`mvn verify -Pit` on ubuntu).

Two non-obvious test details: the integration base class
(`repository/jdbc/AbstractRepositoryIT`) pins `api.version=1.41` because the
bundled docker-java negotiates an API version newer daemons reject, and it uses
second-aligned `LocalDateTime`s since PostgreSQL `TIMESTAMP` is microsecond
precision.

GUI/network coverage: `ui/FxmlSmokeIT` (under `-Pit`) starts the JavaFX toolkit and
`FXMLLoader.load`s every view against a wired `AppContext` to catch fx:id/handler
mismatches — it self-skips when `DISPLAY` is unset, so headless CI is unaffected.
`infrastructure/marc/LocSruLiveTest` hits the real LoC SRU service and is disabled
unless `LOC_LIVE=true` (the offline `LocSruClientTest` fixture is the everyday cover).
Both have been run green here (the app was also booted against a real Postgres).

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
  `jlink` runtime, plus a `.deb` installer when `dpkg-deb`/`fakeroot` are present.
  The app runs JavaFX from the classpath, so the packaged entry point is
  `com.justin.libradesk.Launcher` (not `LibraDeskApplication`, which extends
  `Application` and can't be launched from the classpath). `mvn javafx:run` still
  uses `LibraDeskApplication`.
- **Schema is Flyway-managed.** Add new changes as `db/migration/Vn__*.sql` (plain
  SQL, no `IF NOT EXISTS` — Flyway versions them). `V1__baseline.sql` keeps
  `IF NOT EXISTS` only because it adopts databases created before Flyway;
  `FlywayMigrator` uses `baselineOnMigrate(true)` for that transition.
- **Demo data:** `infrastructure/DemoDataSeeder` runs at startup only when
  `demo.seed=true` and the catalog is empty (dev/demo convenience; never touches
  real data).
- **MARC** lives in `infrastructure/marc`: `MarcService` does file I/O (`.mrc` via
  ISO 2709, `.xml` via MARCXML, both marc4j) and `MarcMapper` converts between a
  marc4j `Record` and `MarcData` (a `Book` plus author/subject/publisher *names*).
  MARC carries names, not our ids, so `CatalogService.importMarc`/`exportMarc`/
  `toMarcData` resolve names↔ids (find-or-create authors/subjects/publishers). The
  original record is stored in `books.marc_xml` for fidelity. `Book` carries richer
  fields (edition/pubPlace/extent/series/language/materialType/controlNumber/
  summary) added as plain fields (not constructor args, to avoid churn) and a
  `subjectIds` list synced like `authorIds` (`JdbcBookRepository.syncLinks`).
  `util/Isbn` validates/normalises ISBN-10/13.
- **Copy cataloging** is `infrastructure/marc/LocSruClient`: it queries the LoC SRU
  service (`loc.sru.url`) with the built-in `HttpClient`, DOM-extracts each
  MARC21-slim record from the `srw:` envelope, and decodes via
  `MarcService.fromXmlString`. HTTP is behind an injectable `HttpFetcher` seam so
  `LocSruClientTest` runs offline against `test/resources/marc/loc-sru-sample.xml`;
  the Catalog "Search LoC" dialog imports the chosen record through
  `CatalogService.importMarc`.
- **Authority control** is lightweight: `authors.name`/`subjects.term` are the
  *authorized* headings; `author_variants`/`subject_variants` hold see-from forms
  (`AuthorityRepository`/`AuthorityService`). `CatalogService.findOrCreateAuthor/
  Subject` checks an exact authorized match, then a variant (`resolveAuthor/Subject`),
  before creating — so importing a variant heading reuses the authorized record.
- **Classification:** `books.call_number` + `classification_scheme` (DDC/LCC), set
  manually or from MARC 082/050; `util/CallNumber.shelfKey` gives a comparable
  shelf-order key (pragmatic, not full LC shelflisting). `PdfService.writeSpineLabels`
  prints labels (call number stacked + barcode) from the Book Copies screen.
- **OPAC search** is `CatalogSearchService.search(keyword)`: in-memory keyword match
  over title/ISBN/author/subject plus facet count maps (author/subject/year/language/
  material), returning `CatalogSearchResult`/`CatalogRecord`. The Catalog Search
  screen (ungated, all roles) populates facet ComboBoxes from the result and narrows
  the loaded set in the controller; facet labels carry a "(count)" suffix stripped on
  use.
- **Full MARC editor** (`MarcEditorController` + `MarcEditorView`, opened modally from
  Catalog): edits the Leader and field lines via `MarcLineCodec` (Record ↔ flat
  `MarcLine` rows, subfields inline as `$aValue$bValue`) with `MarcValidator` checks.
  **The MARC record is the bib source of truth**: `CatalogService.saveFromMarc` stores
  the full `marc_xml` and re-projects the structured columns (so editor changes outside
  the mapped subset are preserved). `MarcService.toXml/parseXml` expose raw Record I/O.

### Phase status (important when reading skeletons)

The full roadmap (Phases 1–9) is complete. Phases 6–9 added: RBAC
(`PermissionPolicy` + `AccessControl`, sidebar gating), Users management, forced
first-login password change; fines (charge on overdue return, block-over-threshold,
Fines screen), loan renewal, READY-reservation expiry via `MaintenanceScheduler`;
audit-log viewer, Reports charts (Bar/Line) and PDF export (OpenPDF); Flyway
migrations, GitHub Actions CI, a `.deb` installer, an About dialog, and a demo-data
seeder.

A **cataloging track** is now in progress on top of that: **Phase 10 (done)** —
richer MARC21 bibliographic fields + MARC import/export (marc4j) + ISBN util;
**Phase 11 (done)** — copy cataloging from the Library of Congress (SRU);
**Phase 12 (done)** — authority see-from variants, DDC/LCC call numbers + shelf-order
key, spine-label PDF; **Phase 13 (done)** — OPAC catalog search with facets. A
professional-ILS track (14–17) is now underway: **Phase 14a (done)** — full
field-level MARC editor with `marc_xml` as source of truth. **Planned:** 14b (batch
import + dedup, record status), 15 (full authority records + global heading change +
id.loc.gov), 16 (circulation policy matrix, calendar, branches, blocks, payments,
email notices), 17 (MFHD/FRBR/serials + REST/OAI-PMH/SRU server + job framework). When extending, follow the
implemented repositories/services and the existing feature controllers as the
reference pattern.

## Language rules

All code, comments, identifiers, documentation, and commit messages are written
in **English**. (Project discussion with the user may be in Traditional Chinese,
but nothing in the repository should be.)
