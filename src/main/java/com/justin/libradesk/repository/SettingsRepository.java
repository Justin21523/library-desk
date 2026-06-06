package com.justin.libradesk.repository;

import java.util.Map;
import java.util.Optional;

/**
 * Key/value store for runtime-editable application settings (the {@code settings}
 * table). Separate from {@link Repository} because settings are keyed by a String
 * and have no entity identity.
 */
public interface SettingsRepository {

    Optional<String> find(String key);

    /** Inserts or updates the value for a key. */
    void put(String key, String value);

    Map<String, String> findAll();
}
