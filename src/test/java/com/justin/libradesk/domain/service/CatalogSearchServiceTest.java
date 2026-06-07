package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Subject;
import com.justin.libradesk.dto.CatalogSearchResult;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.PublisherRepository;
import com.justin.libradesk.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSearchServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private PublisherRepository publisherRepository;

    private CatalogSearchService service;

    @BeforeEach
    void setUp() {
        service = new CatalogSearchService(bookRepository, authorRepository, subjectRepository,
                publisherRepository);

        when(authorRepository.findAll()).thenReturn(List.of(
                new Author(1L, "Bloch, Joshua"), new Author(2L, "Gamma, Erich")));
        when(subjectRepository.findAll()).thenReturn(List.of(
                new Subject(10L, "Java"), new Subject(11L, "Patterns")));
        when(publisherRepository.findAll()).thenReturn(List.of());

        Book java = book(100L, "Effective Java", 2018, 1L, 10L);
        Book patterns = book(200L, "Design Patterns", 1994, 2L, 11L);
        Book unrelated = book(300L, "Cooking 101", 2010, null, null);
        when(bookRepository.findAll()).thenReturn(List.of(java, patterns, unrelated));
    }

    private Book book(long id, String title, int year, Long authorId, Long subjectId) {
        Book book = new Book(id, "isbn" + id, title, null, null, year, LocalDateTime.now());
        if (authorId != null) {
            book.getAuthorIds().add(authorId);
        }
        if (subjectId != null) {
            book.getSubjectIds().add(subjectId);
        }
        return book;
    }

    @Test
    void keywordMatchesTitle() {
        CatalogSearchResult result = service.search("java");

        assertEquals(1, result.records().size());
        assertEquals("Effective Java", result.records().get(0).book().getTitle());
    }

    @Test
    void keywordMatchesAuthorName() {
        CatalogSearchResult result = service.search("gamma");

        assertEquals(1, result.records().size());
        assertEquals("Design Patterns", result.records().get(0).book().getTitle());
    }

    @Test
    void blankKeywordReturnsEverythingWithFacetCounts() {
        CatalogSearchResult result = service.search("  ");

        assertEquals(3, result.records().size());
        assertEquals(1L, result.authorFacet().get("Bloch, Joshua"));
        assertEquals(1L, result.yearFacet().get("2018"));
        assertEquals(2, result.subjectFacet().size()); // Java, Patterns (Cooking has none)
    }
}
