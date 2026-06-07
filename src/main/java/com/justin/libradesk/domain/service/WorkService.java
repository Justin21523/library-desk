package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Work;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.WorkRepository;

import java.util.List;
import java.util.Locale;

/**
 * Groups bibliographic records into FRBR works. A work clusters manifestations
 * (editions/translations) that share a normalized key derived from the title and
 * first author. Pragmatic Group-1 grouping — no full uniform-title authority.
 */
public class WorkService {

    private final WorkRepository workRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final AuditLogService auditLogService;

    public WorkService(WorkRepository workRepository, BookRepository bookRepository,
                       AuthorRepository authorRepository, AuditLogService auditLogService) {
        this.workRepository = workRepository;
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.auditLogService = auditLogService;
    }

    /** @return the normalized work key (title + first author) for a bib. */
    public String workKey(Book book) {
        return normalize(book.getTitle()) + "|" + normalize(firstAuthorName(book));
    }

    /**
     * Assigns every bib a {@code work_id}, creating works as needed by key.
     *
     * @return the number of works that bibs are now grouped under
     */
    public int groupIntoWorks(String actor) {
        List<Book> books = bookRepository.findAll();
        for (Book book : books) {
            String key = workKey(book);
            Work work = workRepository.findByKey(key).orElseGet(() ->
                    workRepository.save(new Work(null, key, book.getTitle(), firstAuthorName(book))));
            if (!work.id().equals(book.getWorkId())) {
                book.setWorkId(work.id());
                bookRepository.save(book);
            }
        }
        int count = workRepository.findAll().size();
        auditLogService.record(actor, "WORKS_GROUPED", "Work", null, count + " works");
        return count;
    }

    public List<Work> listWorks() {
        return workRepository.findAll();
    }

    /** @return the bibs (manifestations) grouped under a work. */
    public List<Book> manifestationsOf(Long workId) {
        return bookRepository.findAll().stream()
                .filter(b -> workId.equals(b.getWorkId()))
                .toList();
    }

    private String firstAuthorName(Book book) {
        if (book.getAuthorIds().isEmpty()) {
            return "";
        }
        return authorRepository.findById(book.getAuthorIds().get(0))
                .map(a -> a.name())
                .orElse("");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
