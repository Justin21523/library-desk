-- LibraDesk schema (PostgreSQL). Idempotent: safe to run on every startup.
-- All identifiers are lower_snake_case. Timestamps use native TIMESTAMP.
-- Requires the target database to already exist (e.g. createdb libradesk).

-- Staff accounts. Role drives permissions (see UserRole enum).
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      TEXT      NOT NULL UNIQUE,
    password_hash TEXT      NOT NULL,
    full_name     TEXT      NOT NULL,
    role          TEXT      NOT NULL,            -- ADMIN | LIBRARIAN | ASSISTANT
    active        BOOLEAN   NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL
);

-- Upgrade existing databases created before this column was added.
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS publishers (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS authors (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

-- Catalog: a bibliographic record. Physical items live in book_copies.
CREATE TABLE IF NOT EXISTS books (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    isbn           TEXT      UNIQUE,
    title          TEXT      NOT NULL,
    publisher_id   BIGINT,
    category_id    BIGINT,
    published_year INTEGER,
    created_at     TIMESTAMP NOT NULL,
    FOREIGN KEY (publisher_id) REFERENCES publishers(id),
    FOREIGN KEY (category_id)  REFERENCES categories(id)
);

-- Many-to-many between books and authors.
CREATE TABLE IF NOT EXISTS book_authors (
    book_id   BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id)   REFERENCES books(id)   ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
);

-- A physical, loanable item belonging to a book.
CREATE TABLE IF NOT EXISTS book_copies (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id        BIGINT    NOT NULL,
    barcode        TEXT      NOT NULL UNIQUE,
    status         TEXT      NOT NULL,          -- AVAILABLE | ON_LOAN | RESERVED | LOST | DAMAGED
    shelf_location TEXT,
    created_at     TIMESTAMP NOT NULL,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- Library members.
CREATE TABLE IF NOT EXISTS patrons (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    membership_no TEXT      NOT NULL UNIQUE,
    full_name     TEXT      NOT NULL,
    email         TEXT,
    phone         TEXT,
    patron_type   TEXT      NOT NULL,           -- STUDENT | STAFF | PUBLIC
    status        TEXT      NOT NULL,           -- ACTIVE | SUSPENDED | EXPIRED
    created_at    TIMESTAMP NOT NULL
);

-- A borrowing of one copy by one patron.
CREATE TABLE IF NOT EXISTS loans (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    copy_id     BIGINT    NOT NULL,
    patron_id   BIGINT    NOT NULL,
    loaned_at   TIMESTAMP NOT NULL,
    due_at      TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    status      TEXT      NOT NULL,             -- ACTIVE | RETURNED | OVERDUE
    FOREIGN KEY (copy_id)   REFERENCES book_copies(id),
    FOREIGN KEY (patron_id) REFERENCES patrons(id)
);

-- Queue of holds against a book whose copies are unavailable.
CREATE TABLE IF NOT EXISTS reservations (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id        BIGINT    NOT NULL,
    patron_id      BIGINT    NOT NULL,
    reserved_at    TIMESTAMP NOT NULL,
    queue_position INTEGER   NOT NULL,
    status         TEXT      NOT NULL,          -- PENDING | READY | FULFILLED | CANCELLED | EXPIRED
    FOREIGN KEY (book_id)   REFERENCES books(id),
    FOREIGN KEY (patron_id) REFERENCES patrons(id)
);

-- Append-only record of important operations.
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor       TEXT      NOT NULL,             -- username performing the action
    action      TEXT      NOT NULL,             -- e.g. LOAN_CREATED, COPY_RETURNED
    entity_type TEXT,
    entity_id   BIGINT,
    detail      TEXT,
    created_at  TIMESTAMP NOT NULL
);

-- Key/value application settings (e.g. loan period, borrowing limits).
CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_loans_patron       ON loans(patron_id);
CREATE INDEX IF NOT EXISTS idx_loans_status       ON loans(status);
CREATE INDEX IF NOT EXISTS idx_copies_book        ON book_copies(book_id);
CREATE INDEX IF NOT EXISTS idx_reservations_book  ON reservations(book_id);
