-- Phase 17: bibliographic structure.
-- FRBR work grouping, MFHD-style holdings, serials, and 856 e-resource links.

-- A work clusters editions/manifestations (FRBR Group 1, pragmatic).
CREATE TABLE IF NOT EXISTS works (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_key TEXT NOT NULL UNIQUE,        -- normalized title + first author
    title    TEXT NOT NULL,
    author   TEXT
);
ALTER TABLE books ADD COLUMN IF NOT EXISTS work_id BIGINT REFERENCES works(id);

-- A holdings (MFHD) record sits between a bib and its items, per location.
CREATE TABLE IF NOT EXISTS holdings (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id     BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    location_id BIGINT REFERENCES locations(id),
    call_number TEXT,
    summary     TEXT,                      -- holdings statement, e.g. "v.1-10 (2010-2020)"
    note        TEXT
);
ALTER TABLE book_copies ADD COLUMN IF NOT EXISTS holding_id BIGINT REFERENCES holdings(id);

-- Serials: a subscription and its predicted/received issues.
CREATE TABLE IF NOT EXISTS subscriptions (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id       BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    label         TEXT NOT NULL,
    frequency     TEXT NOT NULL,           -- WEEKLY | MONTHLY | QUARTERLY | ANNUAL
    status        TEXT NOT NULL,           -- ACTIVE | CANCELLED
    start_date    DATE NOT NULL,
    next_expected DATE
);

CREATE TABLE IF NOT EXISTS serial_issues (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    enumeration     TEXT NOT NULL,         -- e.g. "Vol 12, No 3"
    expected_date   DATE,
    received_date   DATE,
    status          TEXT NOT NULL          -- EXPECTED | RECEIVED | LATE | CLAIMED
);

-- 856 e-resource links with last link-check result.
CREATE TABLE IF NOT EXISTS e_links (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id      BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    url          TEXT NOT NULL,
    label        TEXT,
    last_status  INTEGER,                  -- last HTTP status (NULL = unchecked)
    last_checked TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_holdings_book ON holdings(book_id);
CREATE INDEX IF NOT EXISTS idx_issues_subscription ON serial_issues(subscription_id);
CREATE INDEX IF NOT EXISTS idx_elinks_book ON e_links(book_id);
