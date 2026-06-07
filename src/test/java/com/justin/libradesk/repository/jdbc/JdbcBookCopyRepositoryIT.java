package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Branch;
import com.justin.libradesk.domain.model.ShelfLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void locationIdRoundTripsAndDefaultsToNull() {
        BookCopy unassigned = copyRepository.save(newCopy("BC-NL", CopyStatus.AVAILABLE));
        assertNull(copyRepository.findById(unassigned.getId()).orElseThrow().getLocationId());

        Long branchId = new JdbcBranchRepository(databaseManager)
                .save(new Branch(null, "MAIN", "Main")).id();
        Long locationId = new JdbcLocationRepository(databaseManager)
                .save(new ShelfLocation(null, branchId, "Stacks")).id();

        BookCopy copy = newCopy("BC-LOC", CopyStatus.AVAILABLE);
        copy.setLocationId(locationId);
        BookCopy saved = copyRepository.save(copy);

        assertEquals(locationId, copyRepository.findById(saved.getId()).orElseThrow().getLocationId());
    }
}
