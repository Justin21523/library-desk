package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.enumtype.UserRole;

import java.util.EnumSet;
import java.util.Set;

/**
 * Pure mapping of {@link UserRole} to the {@link Permission}s it grants. Like
 * {@link BorrowingPolicy}, this is dependency-free and unit-tested directly.
 *
 * <p>Roles are cumulative: ASSISTANT handles circulation/reservations (which need
 * no permission), LIBRARIAN adds catalog/patrons/reports/fines, and ADMIN adds
 * settings/users/audit.
 */
public final class PermissionPolicy {

    private static final Set<Permission> LIBRARIAN_EXTRA = EnumSet.of(
            Permission.CATALOG, Permission.PATRONS, Permission.REPORTS, Permission.FINES);

    private PermissionPolicy() {
    }

    public static Set<Permission> permissionsFor(UserRole role) {
        return switch (role) {
            case ASSISTANT -> EnumSet.noneOf(Permission.class);
            case LIBRARIAN -> EnumSet.copyOf(LIBRARIAN_EXTRA);
            case ADMIN -> EnumSet.allOf(Permission.class);
        };
    }

    public static boolean has(UserRole role, Permission permission) {
        return permissionsFor(role).contains(permission);
    }
}
