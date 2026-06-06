package com.justin.libradesk.infrastructure.marc;

import com.justin.libradesk.domain.model.Book;

import java.util.List;

/**
 * A MARC record decoded into the parts LibraDesk uses. Author/subject/publisher
 * appear here as names (MARC carries names, not our ids); the catalog service
 * resolves them to/from {@code authors}/{@code subjects}/{@code publishers} when
 * importing or exporting.
 */
public record MarcData(Book book, List<String> authorNames, List<String> subjectTerms,
                       String publisherName) {
}
