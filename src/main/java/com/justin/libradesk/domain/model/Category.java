package com.justin.libradesk.domain.model;

/** A catalog category. {@code id} is {@code null} until persisted. */
public record Category(Long id, String name) {
}
