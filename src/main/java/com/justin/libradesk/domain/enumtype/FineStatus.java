package com.justin.libradesk.domain.enumtype;

/**
 * Status of a fine. UNPAID fines count toward a patron's outstanding balance;
 * PAID and WAIVED are settled.
 */
public enum FineStatus {
    UNPAID,
    PAID,
    WAIVED
}
