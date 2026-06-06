package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;

import java.util.List;

/**
 * Manages the catalog (books and their physical copies).
 *
 * <p>Phase 1 skeleton: lookups delegate to the repositories (currently returning
 * empty results until {@code JdbcBookRepository}/{@code JdbcBookCopyRepository}
 * are implemented in Phase 2). Mutating operations are added in Phase 2 once the
 * repositories support writes.
 */
public class CatalogService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final AuditLogService auditLogService;

    public CatalogService(BookRepository bookRepository,
                          BookCopyRepository bookCopyRepository,
                          AuditLogService auditLogService) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.auditLogService = auditLogService;
    }

    public List<Book> listBooks() {
        return bookRepository.findAll();
    }

    public List<Book> searchByTitle(String fragment) {
        return bookRepository.searchByTitle(fragment);
    }

    // TODO(phase2): addBook, updateBook, addCopy, retireCopy with audit + validation.
}
