-- Phase 16: collection policy + circulation upgrade.
-- Branches/shelving locations, a circulation policy matrix, a library calendar,
-- renewal tracking, and billing (fee types + partial payments).

-- Branches and named shelving locations.
CREATE TABLE IF NOT EXISTS branches (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS locations (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    name      TEXT NOT NULL,
    UNIQUE (branch_id, name)
);

ALTER TABLE book_copies ADD COLUMN IF NOT EXISTS location_id BIGINT REFERENCES locations(id);

-- Circulation policy matrix. A row with NULL material_type is the default for
-- the patron type; an exact (patron_type, material_type) row overrides it.
CREATE TABLE IF NOT EXISTS circ_policies (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patron_type   TEXT NOT NULL,            -- STUDENT | STAFF | PUBLIC
    material_type TEXT,                      -- BOOK | SERIAL | EBOOK | AUDIOVISUAL | NULL (=default)
    loan_days     INTEGER NOT NULL,
    max_loans     INTEGER NOT NULL,
    renewal_limit INTEGER NOT NULL,
    max_holds     INTEGER NOT NULL,
    fine_per_day  NUMERIC(10,2) NOT NULL,
    fine_cap      NUMERIC(10,2) NOT NULL DEFAULT 0,   -- 0 = no cap
    grace_days    INTEGER NOT NULL DEFAULT 0
);
-- COALESCE keeps a single default row per patron type (NULLs are otherwise distinct).
CREATE UNIQUE INDEX IF NOT EXISTS ux_circ_policy
    ON circ_policies (patron_type, COALESCE(material_type, ''));

-- Library calendar: dates the library is closed (due dates roll off these, and
-- overdue fines do not accrue on them).
CREATE TABLE IF NOT EXISTS calendar_days (
    closed_date DATE PRIMARY KEY,
    note        TEXT
);

-- Renewal tracking on loans.
ALTER TABLE loans ADD COLUMN IF NOT EXISTS renewal_count INTEGER NOT NULL DEFAULT 0;

-- Billing extensions on fines.
ALTER TABLE fines ADD COLUMN IF NOT EXISTS fee_type    TEXT          NOT NULL DEFAULT 'OVERDUE';
ALTER TABLE fines ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE fines ADD COLUMN IF NOT EXISTS note        TEXT;

-- Payments recorded against a fine (supports partial payment and waive reasons).
CREATE TABLE IF NOT EXISTS payments (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fine_id BIGINT        NOT NULL REFERENCES fines(id) ON DELETE CASCADE,
    amount  NUMERIC(10,2) NOT NULL,
    method  TEXT,                            -- CASH | CARD | WAIVE
    note    TEXT,
    paid_at TIMESTAMP     NOT NULL,
    actor   TEXT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_payments_fine ON payments(fine_id);
CREATE INDEX IF NOT EXISTS idx_copies_location ON book_copies(location_id);
