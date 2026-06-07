package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.dto.CatalogRecord;
import com.justin.libradesk.dto.CatalogSearchResult;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.PublisherRepository;
import com.justin.libradesk.repository.SubjectRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only OPAC search: keyword matching across title/ISBN/author/subject plus
 * facet counts. Computed in memory from the repositories (same approach as
 * {@link DashboardService}/{@link ReportsService}); for this project's scale that
 * keeps it simple and avoids a separate search index.
 */
public class CatalogSearchService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final SubjectRepository subjectRepository;
    private final PublisherRepository publisherRepository;

    public CatalogSearchService(BookRepository bookRepository,
                                AuthorRepository authorRepository,
                                SubjectRepository subjectRepository,
                                PublisherRepository publisherRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.subjectRepository = subjectRepository;
        this.publisherRepository = publisherRepository;
    }

    public CatalogSearchResult search(String keyword) {
        Map<Long, String> authorNames = new HashMap<>();
        authorRepository.findAll().forEach(a -> authorNames.put(a.id(), a.name()));
        Map<Long, String> subjectTerms = new HashMap<>();
        subjectRepository.findAll().forEach(s -> subjectTerms.put(s.id(), s.term()));
        Map<Long, String> publisherNames = new HashMap<>();
        publisherRepository.findAll().forEach(p -> publisherNames.put(p.id(), p.name()));

        List<CatalogRecord> all = bookRepository.findAll().stream()
                .map(book -> new CatalogRecord(book,
                        names(book.getAuthorIds(), authorNames),
                        names(book.getSubjectIds(), subjectTerms),
                        book.getPublisherId() == null ? null : publisherNames.get(book.getPublisherId())))
                .toList();

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<CatalogRecord> matched = kw.isEmpty()
                ? all
                : all.stream().filter(record -> matches(record, kw)).toList();

        return new CatalogSearchResult(matched,
                facet(matched, record -> record.authors()),
                facet(matched, record -> record.subjects()),
                facet(matched, record -> singletonOrEmpty(yearOf(record.book()))),
                facet(matched, record -> singletonOrEmpty(record.book().getLanguage())),
                facet(matched, record -> singletonOrEmpty(materialOf(record.book()))));
    }

    private boolean matches(CatalogRecord record, String keyword) {
        Book book = record.book();
        if (contains(book.getTitle(), keyword) || contains(book.getIsbn(), keyword)) {
            return true;
        }
        return record.authors().stream().anyMatch(a -> contains(a, keyword))
                || record.subjects().stream().anyMatch(s -> contains(s, keyword));
    }

    /** Counts each value produced by {@code extractor} across the records (skips blanks). */
    private Map<String, Long> facet(List<CatalogRecord> records,
                                    java.util.function.Function<CatalogRecord, List<String>> extractor) {
        Map<String, Long> counts = new TreeMap<>();
        for (CatalogRecord record : records) {
            for (String value : extractor.apply(record)) {
                if (value != null && !value.isBlank()) {
                    counts.merge(value, 1L, Long::sum);
                }
            }
        }
        return counts;
    }

    private static List<String> names(List<Long> ids, Map<Long, String> lookup) {
        return ids.stream().map(lookup::get).filter(java.util.Objects::nonNull).toList();
    }

    private static List<String> singletonOrEmpty(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }

    private static String yearOf(Book book) {
        return book.getPublishedYear() == null ? null : book.getPublishedYear().toString();
    }

    private static String materialOf(Book book) {
        return book.getMaterialType() == null ? null : book.getMaterialType().name();
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}
