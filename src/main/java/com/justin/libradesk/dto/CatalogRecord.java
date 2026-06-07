package com.justin.libradesk.dto;

import com.justin.libradesk.domain.model.Book;

import java.util.List;

/**
 * A catalog record prepared for display in the OPAC: the {@link Book} with its
 * author/subject names and publisher resolved (ids → names).
 */
public record CatalogRecord(Book book, List<String> authors, List<String> subjects, String publisher) {

    public String authorsJoined() {
        return String.join("; ", authors);
    }
}
