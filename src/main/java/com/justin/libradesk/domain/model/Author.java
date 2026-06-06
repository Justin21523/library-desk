package com.justin.libradesk.domain.model;

/** A book author. {@code id} is {@code null} until persisted. */
public record Author(Long id, String name) {
}
