package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.domain.service.PermissionPolicy;
import com.justin.libradesk.validation.ValidationException;

/**
 * UI-side access checks for the signed-in user. Used both to hide sidebar entries
 * and to guard privileged controller actions (defence in depth alongside the
 * hidden buttons).
 */
public final class AccessControl {

    private AccessControl() {
    }

    public static boolean can(Permission permission) {
        User user = AppContext.get().getCurrentUser();
        return user != null && PermissionPolicy.has(user.getRole(), permission);
    }

    public static void require(Permission permission) {
        if (!can(permission)) {
            throw new ValidationException("You do not have permission to perform this action.");
        }
    }
}
