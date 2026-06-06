package com.justin.libradesk.infrastructure;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Patron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Inserts a small set of sample data so evaluators see a populated application.
 * Dev-only: enabled by {@code demo.seed=true} and only runs when the catalog is
 * empty, so it never duplicates or touches real data.
 */
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String ACTOR = "system";

    private final AppContext context;

    public DemoDataSeeder(AppContext context) {
        this.context = context;
    }

    public void seedIfEmpty() {
        if (!context.catalogService().listBooks().isEmpty()) {
            return;
        }
        log.warn("Seeding demo data (demo.seed=true and catalog is empty)");

        addBookWithCopies("978-0001", "Clean Code", 2008, "CC-1", "CC-2");
        addBookWithCopies("978-0002", "The Pragmatic Programmer", 1999, "PP-1");
        addBookWithCopies("978-0003", "Effective Java", 2018, "EJ-1", "EJ-2", "EJ-3");

        register("M001", "Alice Reader", PatronType.STUDENT);
        register("M002", "Bob Member", PatronType.PUBLIC);
        register("M003", "Carol Staff", PatronType.STAFF);
    }

    private void addBookWithCopies(String isbn, String title, int year, String... barcodes) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setPublishedYear(year);
        Book saved = context.catalogService().addBook(book, ACTOR);
        for (String barcode : barcodes) {
            BookCopy copy = new BookCopy();
            copy.setBookId(saved.getId());
            copy.setBarcode(barcode);
            context.catalogService().addCopy(copy, ACTOR);
        }
    }

    private void register(String membershipNo, String fullName, PatronType type) {
        Patron patron = new Patron(null, membershipNo, fullName, null, null, type,
                PatronStatus.ACTIVE, LocalDateTime.now());
        context.patronService().register(patron, ACTOR);
    }
}
