-- Phase 14b: bibliographic record status (IN_PROCESS | COMPLETE | SUPPRESSED).
-- SUPPRESSED records are hidden from the OPAC search.
ALTER TABLE books ADD COLUMN record_status TEXT NOT NULL DEFAULT 'COMPLETE';
