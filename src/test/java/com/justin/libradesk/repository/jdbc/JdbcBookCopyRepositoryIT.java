package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcBookCopyRepositoryIT extends AbstractRepositoryIT {

    private JdbcBookRepository bookRepository;
    private JdbcBookCopyRepository copyRepository;
    private Long bookId;

    @BeforeEach
    void setUp() {
        bookRepository = new JdbcBookRepository(databaseManager);
        copyRepository = new JdbcBookCopyRepository(databaseManager);
        bookId = bookRepository.save(new Book(null, "isbn", "Host Book", null, null, null, FIXED)).getId();
    }

    private BookCopy newCopy(String barcode, CopyStatus status) {
        return new BookCopy(null, bookId, barcode, status, "A-1", FIXED);
    }

    @Test
    void savesAndReadsBackByIdAndBarcode() {
        BookCopy saved = copyRepository.save(newCopy("BC-1", CopyStatus.AVAILABLE));

        assertTrue(saved.getId() != null);
        assertEquals("BC-1", copyRepository.findById(saved.getId()).orElseThrow().getBarcode());
        assertEquals(CopyStatus.AVAILABLE,
                copyRepository.findByBarcode("BC-1").orElseThrow().getStatus());
    }

    @Test
    void findAvailableByBookIdExcludesNonAvailableCopies() {
        copyRepository.save(newCopy("BC-A", CopyStatus.AVAILABLE));
        copyRepository.save(newCopy("BC-B", CopyStatus.AVAILABLE));
        copyRepository.save(newCopy("BC-C", CopyStatus.ON_LOAN));

        assertEquals(3, copyRepository.findByBookId(bookId).size());
        assertEquals(2, copyRepository.findAvailableByBookId(bookId).size());
    }

    @Test
    void updatePersistsStatusChange() {
        BookCopy saved = copyRepository.save(newCopy("BC-2", CopyStatus.AVAILABLE));

        saved.setStatus(CopyStatus.ON_LOAN);
        copyRepository.save(saved);

        assertEquals(CopyStatus.ON_LOAN,
                copyRepository.findById(saved.getId()).orElseThrow().getStatus());
    }
}
