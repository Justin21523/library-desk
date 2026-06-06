package com.justin.libradesk.domain.model;

/** A subject heading (MARC 6xx term). {@code id} is {@code null} until persisted. */
public record Subject(Long id, String term) {
}
