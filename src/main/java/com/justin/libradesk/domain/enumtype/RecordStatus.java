package com.justin.libradesk.domain.enumtype;

/**
 * Workflow status of a bibliographic record. SUPPRESSED records are kept but
 * hidden from the public catalog (OPAC) search.
 */
public enum RecordStatus {
    IN_PROCESS,
    COMPLETE,
    SUPPRESSED
}
