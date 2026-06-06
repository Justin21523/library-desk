package com.justin.libradesk.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract implemented by every JDBC repository.
 *
 * @param <T>  the entity type
 * @param <ID> the identifier type
 */
public interface Repository<T, ID> {

    /** Inserts a new entity or updates an existing one; returns the persisted entity (with id). */
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void deleteById(ID id);
}
