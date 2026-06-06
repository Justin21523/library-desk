package com.justin.libradesk.repository;

/**
 * Unchecked wrapper around persistence failures. Repositories translate
 * {@link java.sql.SQLException} into this so upper layers are not coupled to JDBC.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
