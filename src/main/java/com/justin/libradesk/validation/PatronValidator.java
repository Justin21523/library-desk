package com.justin.libradesk.validation;

import com.justin.libradesk.domain.model.Patron;

/**
 * Field-level validation for patron data, kept separate from the borrowing
 * business rules (which live in the domain service layer).
 */
public final class PatronValidator {

    private PatronValidator() {
    }

    /**
     * @throws ValidationException if any required field is missing/invalid
     */
    public static void validate(Patron patron) {
        if (patron == null) {
            throw new ValidationException("Patron must not be null");
        }
        if (isBlank(patron.getMembershipNo())) {
            throw new ValidationException("Membership number is required");
        }
        if (isBlank(patron.getFullName())) {
            throw new ValidationException("Full name is required");
        }
        if (patron.getPatronType() == null) {
            throw new ValidationException("Patron type is required");
        }
        if (patron.getStatus() == null) {
            throw new ValidationException("Patron status is required");
        }
        String email = patron.getEmail();
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw new ValidationException("Email is not valid: " + email);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
