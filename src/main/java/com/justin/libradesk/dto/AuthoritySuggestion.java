package com.justin.libradesk.dto;

/** An authorized-heading suggestion from an authority service (label + its URI). */
public record AuthoritySuggestion(String label, String uri) {
}
