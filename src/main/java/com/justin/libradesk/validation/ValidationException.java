package com.justin.libradesk.validation;

/**
 * Thrown when a domain operation is rejected by a business/validation rule
 * (e.g. a suspended patron tries to borrow). Services throw it; controllers
 * catch it and surface the message to the user.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
