package com.justin.libradesk.domain.model;

/** A publisher. {@code id} is {@code null} until persisted. */
public record Publisher(Long id, String name) {
}
