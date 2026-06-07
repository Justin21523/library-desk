package com.justin.libradesk.domain.model;

import java.time.LocalDateTime;

/**
 * An 856 e-resource link for a bib, with the result of the last link check.
 * {@code id} is {@code null} until persisted; {@code lastStatus}/{@code lastChecked}
 * are {@code null} until the link has been checked.
 */
public record ELink(Long id, Long bookId, String url, String label,
                    Integer lastStatus, LocalDateTime lastChecked) {
}
