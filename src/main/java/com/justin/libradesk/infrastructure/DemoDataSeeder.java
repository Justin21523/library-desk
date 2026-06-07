package com.justin.libradesk.infrastructure;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Branch;
import com.justin.libradesk.domain.model.CircPolicy;
import com.justin.libradesk.domain.model.Patron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        seedStructureAndPolicies();

        addBookWithCopies("978-0001", "Clean Code", 2008, "CC-1", "CC-2");
        addBookWithCopies("978-0002", "The Pragmatic Programmer", 1999, "PP-1");
        addBookWithCopies("978-0003", "Effective Java", 2018, "EJ-1", "EJ-2", "EJ-3");

        register("M001", "Alice Reader", "alice@example.com", PatronType.STUDENT);
        register("M002", "Bob Member", "bob@example.com", PatronType.PUBLIC);
        register("M003", "Carol Staff", "carol@example.com", PatronType.STAFF);
    }

    /** Seeds a branch with locations, a default policy per patron type, and a sample closed day. */
    private void seedStructureAndPolicies() {
        Branch main = context.locationService().addBranch("MAIN", "Main Library", ACTOR);
        context.locationService().addLocation(main.id(), "Main Stacks", ACTOR);
        context.locationService().addLocation(main.id(), "Reference", ACTOR);

        // (id, patronType, materialType=null default, loanDays, maxLoans, renewalLimit,
        //  maxHolds, finePerDay, fineCap, graceDays)
        savePolicy(PatronType.STUDENT, 14, 5, 2, 5, "0.50", "20.00", 0);
        savePolicy(PatronType.STAFF, 28, 10, 3, 10, "0.25", "30.00", 2);
        savePolicy(PatronType.PUBLIC, 14, 3, 1, 3, "0.50", "15.00", 0);

        context.calendarService().add(LocalDate.now().plusDays(7), "Staff training day", ACTOR);
    }

    private void savePolicy(PatronType type, int loanDays, int maxLoans, int renewalLimit,
                            int maxHolds, String finePerDay, String fineCap, int graceDays) {
        context.circPolicyService().save(new CircPolicy(null, type, null, loanDays, maxLoans,
                renewalLimit, maxHolds, new BigDecimal(finePerDay), new BigDecimal(fineCap), graceDays), ACTOR);
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

    private void register(String membershipNo, String fullName, String email, PatronType type) {
        Patron patron = new Patron(null, membershipNo, fullName, email, null, type,
                PatronStatus.ACTIVE, LocalDateTime.now());
        context.patronService().register(patron, ACTOR);
    }
}
