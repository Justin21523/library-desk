package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.enumtype.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionPolicyTest {

    @Test
    void adminHasEveryPermission() {
        for (Permission permission : Permission.values()) {
            assertTrue(PermissionPolicy.has(UserRole.ADMIN, permission), permission.name());
        }
    }

    @Test
    void librarianHasCatalogButNotSettingsOrUsers() {
        assertTrue(PermissionPolicy.has(UserRole.LIBRARIAN, Permission.CATALOG));
        assertTrue(PermissionPolicy.has(UserRole.LIBRARIAN, Permission.REPORTS));
        assertFalse(PermissionPolicy.has(UserRole.LIBRARIAN, Permission.SETTINGS));
        assertFalse(PermissionPolicy.has(UserRole.LIBRARIAN, Permission.USERS));
    }

    @Test
    void assistantHasNoGatedPermissions() {
        assertTrue(PermissionPolicy.permissionsFor(UserRole.ASSISTANT).isEmpty());
    }
}
