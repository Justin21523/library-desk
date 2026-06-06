package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Category;
import com.justin.libradesk.domain.model.Publisher;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.CategoryRepository;
import com.justin.libradesk.repository.PublisherRepository;
import com.justin.libradesk.validation.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Manages the catalog: bibliographic records ({@link Book}) and their physical,
 * loanable copies ({@link BookCopy}).
 */
public class CatalogService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public CatalogService(BookRepository bookRepository,
                          BookCopyRepository bookCopyRepository,
                          AuthorRepository authorRepository,
                          PublisherRepository publisherRepository,
                          CategoryRepository categoryRepository,
                          AuditLogService auditLogService,
                          Clock clock) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    // --- Books ---

    /**
     * Adds a new bibliographic record.
     *
     * @throws ValidationException if the title is blank or the ISBN is already used
     */
    public Book addBook(Book book, String actor) {
        if (isBlank(book.getTitle())) {
            throw new ValidationException("Title is required");
        }
        if (!isBlank(book.getIsbn())) {
            bookRepository.findByIsbn(book.getIsbn()).ifPresent(existing -> {
                throw new ValidationException("ISBN already exists: " + book.getIsbn());
            });
        }
        if (book.getCreatedAt() == null) {
            book.setCreatedAt(LocalDateTime.now(clock));
        }
        Book saved = bookRepository.save(book);
        auditLogService.record(actor, "BOOK_ADDED", "Book", saved.getId(), book.getTitle());
        return saved;
    }

    public List<Book> listBooks() {
        return bookRepository.findAll();
    }

    public List<Book> searchByTitle(String fragment) {
        return bookRepository.searchByTitle(fragment);
    }

    public Optional<Book> getBook(Long bookId) {
        return bookRepository.findById(bookId);
    }

    // --- Copies ---

    /**
     * Adds a physical copy to a book. New copies start {@link CopyStatus#AVAILABLE}.
     *
     * @throws ValidationException if the barcode is blank or already used
     */
    public BookCopy addCopy(BookCopy copy, String actor) {
        if (copy.getBookId() == null) {
            throw new ValidationException("A copy must belong to a book");
        }
        if (isBlank(copy.getBarcode())) {
            throw new ValidationException("Barcode is required");
        }
        bookCopyRepository.findByBarcode(copy.getBarcode()).ifPresent(existing -> {
            throw new ValidationException("Barcode already exists: " + copy.getBarcode());
        });
        if (copy.getStatus() == null) {
            copy.setStatus(CopyStatus.AVAILABLE);
        }
        if (copy.getCreatedAt() == null) {
            copy.setCreatedAt(LocalDateTime.now(clock));
        }
        BookCopy saved = bookCopyRepository.save(copy);
        auditLogService.record(actor, "COPY_ADDED", "BookCopy", saved.getId(), copy.getBarcode());
        return saved;
    }

    /** Manually changes a copy's status (e.g. mark LOST or DAMAGED). */
    public BookCopy updateCopyStatus(Long copyId, CopyStatus status, String actor) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ValidationException("Book copy not found: " + copyId));
        copy.setStatus(status);
        BookCopy saved = bookCopyRepository.save(copy);
        auditLogService.record(actor, "COPY_STATUS_CHANGED", "BookCopy", copyId, status.name());
        return saved;
    }

    public List<BookCopy> listCopies(Long bookId) {
        return bookCopyRepository.findByBookId(bookId);
    }

    public Optional<BookCopy> findCopyByBarcode(String barcode) {
        return bookCopyRepository.findByBarcode(barcode);
    }

    // --- Reference data (authors / publishers / categories) ---

    public Author addAuthor(String name, String actor) {
        Author saved = authorRepository.save(new Author(null, requireName(name)));
        auditLogService.record(actor, "AUTHOR_ADDED", "Author", saved.id(), saved.name());
        return saved;
    }

    public List<Author> listAuthors() {
        return authorRepository.findAll();
    }

    public Publisher addPublisher(String name, String actor) {
        Publisher saved = publisherRepository.save(new Publisher(null, requireName(name)));
        auditLogService.record(actor, "PUBLISHER_ADDED", "Publisher", saved.id(), saved.name());
        return saved;
    }

    public List<Publisher> listPublishers() {
        return publisherRepository.findAll();
    }

    public Category addCategory(String name, String actor) {
        Category saved = categoryRepository.save(new Category(null, requireName(name)));
        auditLogService.record(actor, "CATEGORY_ADDED", "Category", saved.id(), saved.name());
        return saved;
    }

    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    private static String requireName(String name) {
        if (isBlank(name)) {
            throw new ValidationException("Name is required");
        }
        return name.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
