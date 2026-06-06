package com.justin.libradesk.domain.enumtype;

/**
 * A capability a staff member may have, used for role-based access control.
 * Dashboard, Circulation, and Reservations are available to every role, so they
 * have no permission here; only the gated areas do.
 */
public enum Permission {
    CATALOG,
    PATRONS,
    REPORTS,
    FINES,
    SETTINGS,
    USERS,
    AUDIT
}
