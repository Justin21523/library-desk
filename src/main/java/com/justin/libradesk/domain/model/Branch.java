package com.justin.libradesk.domain.model;

/** A physical library branch. {@code id} is {@code null} until persisted. */
public record Branch(Long id, String code, String name) {
}
