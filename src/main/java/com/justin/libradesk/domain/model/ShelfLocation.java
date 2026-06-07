package com.justin.libradesk.domain.model;

/**
 * A named shelving location within a {@link Branch} (e.g. "Main Stacks").
 * {@code id} is {@code null} until persisted.
 */
public record ShelfLocation(Long id, Long branchId, String name) {
}
