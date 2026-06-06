package com.justin.libradesk.dto;

/** A label/count pair for bar-chart style report aggregates. */
public record NamedCount(String name, long count) {
}
