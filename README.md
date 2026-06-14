# LibraDesk

LibraDesk is a comprehensive, desktop-based Library Information Management System (ILS) built with Java and JavaFX. It is designed to handle everything from basic circulation and cataloging to professional bibliographic management using MARC21, authority control, and interoperability protocols.

## Features

### Core Library Operations
- **Circulation Management**: Checkout, check-in, renewals, and holds.
- **Patron Management**: Detailed patron accounts, membership levels, and automated blocks (for fines or overdues).
- **Fine Management**: Typed charges (Overdue, Lost, Damaged), partial payments, and waive functionality.
- **Reservation System**: Manage item holds with automated expiry for uncollected items.
- **Notices**: Automated due-soon, overdue, and hold-ready notifications via email.

### Advanced Cataloging
- **MARC21 Support**: Full field-level MARC editor with `marc_xml` as the source of truth.
- **Copy Cataloging**: Import records directly from the Library of Congress via SRU.
- **Authority Control**: Manage authors and subjects with see-from variants, rename/merge capabilities, and id.loc.gov lookup.
- **Classification**: Support for DDC and LCC schemes with shelf-order key generation.
- **OPAC Search**: Keyword-based search with faceted navigation (author, subject, year, language, material).
- **Bib Structure**: Support for FRBR works, MFHD holdings, and serials subscriptions with automated issue prediction and claiming.

### Administrative & Technical
- **RBAC (Role-Based Access Control)**: Gated access for Librarians and Admins.
- **Audit Logging**: Comprehensive tracking of all sensitive actions.
- **Reporting**: Visual charts (Bar/Line) and exportable reports in PDF and CSV.
- **Background Jobs**: A generalized job framework for maintenance tasks like overdue sweeps and notice sending.
- **Interoperability**: Built-in server supporting REST (JSON), OAI-PMH, and SRU protocols.
- **i18n**: Multi-language support (English default, Traditional Chinese).

## Tech Stack
- **Java 21**: Leveraging the latest LTS features.
- **JavaFX 21**: Modern desktop UI.
- **PostgreSQL**: Robust relational data storage.
- **Flyway**: Automated database migrations.
- **Maven**: Dependency management and build automation.
- **Testcontainers**: Integration testing with real Docker-based PostgreSQL.
- **Libraries**: HikariCP (pooling), Jackson (JSON), Apache Commons CSV, OpenPDF (PDF export), MARC4J (MARC21 parsing).

## Architecture

The project follows a strict top-down layered architecture:
`JavaFX (FXML/CSS) → Controller → Service (Domain Logic) → Repository (JDBC) → Domain Model`

- **Composition Root**: `AppContext` manages service and repository instantiation.
- **Persistence**: SQL-based repositories with conversion to domain objects.
- **Validation**: Service-layer validation with custom exceptions surfaced to the UI.
- **Time Management**: Injected `java.time.Clock` for deterministic testing.

## Getting Started

### Prerequisites
- JDK 21+
- PostgreSQL 16+
- Maven 3.9+
- Docker (optional, for running integration tests)

### Database Setup
1. Create a PostgreSQL database named `libradesk`:
   ```bash
   createdb libradesk
   ```
2. Update `src/main/resources/application.properties` with your database credentials (or use environment variables like `DB_PASSWORD`).

### Running the Application
```bash
mvn javafx:run
```
Default login: `admin` / `admin` (You will be prompted to change your password on first login).

### Testing
- **Unit Tests**: `mvn test`
- **Integration Tests**: `mvn verify -Pit` (Requires Docker)

## Deployment
Build a native application image using jlink and jpackage:
```bash
./scripts/package.sh
```

## License
[Insert License Here]
