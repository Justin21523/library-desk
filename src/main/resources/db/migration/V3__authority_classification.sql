-- Phase 12: call numbers (classification) and authority see-from variants.

ALTER TABLE books ADD COLUMN call_number           TEXT;   -- e.g. "005.133 B652e" (DDC) or "QA76.73.J38" (LCC)
ALTER TABLE books ADD COLUMN classification_scheme TEXT;   -- DDC | LCC

-- See-from variant headings pointing at an authorized author (authors.name).
CREATE TABLE author_variants (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    author_id    BIGINT NOT NULL,
    variant_form TEXT   NOT NULL,
    UNIQUE (author_id, variant_form),
    FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
);

-- See-from variant headings pointing at an authorized subject (subjects.term).
CREATE TABLE subject_variants (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_id   BIGINT NOT NULL,
    variant_form TEXT   NOT NULL,
    UNIQUE (subject_id, variant_form),
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

CREATE INDEX idx_author_variants_form  ON author_variants  (lower(variant_form));
CREATE INDEX idx_subject_variants_form ON subject_variants (lower(variant_form));
