package com.justin.libradesk.domain.model;

/**
 * A FRBR work that clusters bibliographic manifestations (editions/translations)
 * sharing a normalized {@code workKey}. {@code id} is {@code null} until persisted.
 */
public record Work(Long id, String workKey, String title, String author) {
}
