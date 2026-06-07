package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.dto.BatchImportResult;
import com.justin.libradesk.infrastructure.marc.MarcData;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.CategoryRepository;
import com.justin.libradesk.repository.PublisherRepository;
import com.justin.libradesk.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private AuthorityService authorityService;
    @Mock private AuditLogService auditLogService;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 1, 1, 9, 0).atZone(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC"));
        catalogService = new CatalogService(bookRepository, bookCopyRepository, authorRepository,
                publisherRepository, categoryRepository, subjectRepository, authorityService,
                auditLogService, clock);
    }

    private MarcData marcData(String title) {
        return marcData(title, null);
    }

    private MarcData marcData(String title, String isbn) {
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setMarcXml("<record/>");
        return new MarcData(book, List.of(), List.of(), null);
    }

    @Test
    void saveFromMarcInsertsWhenNoExistingId() {
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

        catalogService.saveFromMarc(null, marcData("New Title"), "admin");

        verify(bookRepository).save(any(Book.class));
        verify(auditLogService).record(eq("admin"), eq("BOOK_ADDED"), eq("Book"), any(), eq("New Title"));
    }

    @Test
    void saveFromMarcUpdatesExistingBookInPlace() {
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

        catalogService.saveFromMarc(7L, marcData("Edited Title"), "admin");

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getId());
        assertEquals("<record/>", captor.getValue().getMarcXml());
        verify(auditLogService).record("admin", "BOOK_UPDATED", "Book", 7L, "Edited Title");
    }

    @Test
    void importBatchSkipsDuplicatesByIsbn() {
        Book existing = new Book(1L, "9780134685991", "Existing", null, null, 2018, LocalDateTime.now());
        when(bookRepository.findAll()).thenReturn(List.of(existing));
        when(bookRepository.findByIsbn("222")).thenReturn(java.util.Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

        BatchImportResult result = catalogService.importBatch(List.of(
                marcData("Dup", "978-0-13-468599-1"),   // same ISBN as existing -> duplicate
                marcData("Fresh", "222")), "admin");      // new -> imported

        assertEquals(1, result.imported());
        assertEquals(1, result.duplicates().size());
        assertEquals(0, result.errors().size());
    }
}
