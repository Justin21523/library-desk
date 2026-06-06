-- Phase 10: richer bibliographic fields (MARC-derived) and subjects.

ALTER TABLE books ADD COLUMN edition        TEXT;   -- MARC 250
ALTER TABLE books ADD COLUMN pub_place      TEXT;   -- MARC 264 $a
ALTER TABLE books ADD COLUMN extent         TEXT;   -- MARC 300 (physical description)
ALTER TABLE books ADD COLUMN series         TEXT;   -- MARC 490 / 830
ALTER TABLE books ADD COLUMN language       TEXT;   -- MARC 008/35-37 / 041
ALTER TABLE books ADD COLUMN material_type  TEXT;   -- BOOK | EBOOK | SERIAL | AUDIOVISUAL | OTHER
ALTER TABLE books ADD COLUMN control_number TEXT;   -- MARC 001
ALTER TABLE books ADD COLUMN summary        TEXT;   -- MARC 520
ALTER TABLE books ADD COLUMN marc_xml       TEXT;   -- original record, for round-trip fidelity

-- Subject headings (MARC 6xx), repeatable per book.
CREATE TABLE subjects (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    term TEXT NOT NULL UNIQUE
);

CREATE TABLE book_subjects (
    book_id    BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, subject_id),
    FOREIGN KEY (book_id)    REFERENCES books(id)    ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);
