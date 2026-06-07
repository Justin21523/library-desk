package com.justin.libradesk.domain.model;

/**
 * An MFHD-style holdings record between a bib and its items, scoped to a
 * {@link ShelfLocation}. {@code id} is {@code null} until persisted.
 */
public record Holding(Long id, Long bookId, Long locationId, String callNumber,
                      String summary, String note) {
}
