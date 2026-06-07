package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Work;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.WorkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Mock
    private WorkRepository workRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private AuditLogService auditLogService;

    private WorkService workService;

    @BeforeEach
    void setUp() {
        workService = new WorkService(workRepository, bookRepository, authorRepository, auditLogService);
    }

    private Book book(long id, String title, Long authorId) {
        Book book = new Book(id, "isbn" + id, title, null, null, 2020, NOW);
        if (authorId != null) {
            book.getAuthorIds().add(authorId);
        }
        return book;
    }

    @Test
    void workKeyNormalizesTitleAndAuthor() {
        Book book = book(5L, "Clean Code!", 1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(new Author(1L, "Martin, Robert")));

        assertEquals("clean code|martin robert", workService.workKey(book));
    }

    @Test
    void groupIntoWorksAssignsWorkIdByKey() {
        Book book = book(5L, "Clean Code", 1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(new Author(1L, "Martin, Robert")));
        when(bookRepository.findAll()).thenReturn(List.of(book));
        when(workRepository.findByKey(any())).thenReturn(Optional.empty());
        Work created = new Work(7L, "clean code|martin robert", "Clean Code", "Martin, Robert");
        when(workRepository.save(any(Work.class))).thenReturn(created);
        when(workRepository.findAll()).thenReturn(List.of(created));

        int works = workService.groupIntoWorks("admin");

        assertEquals(1, works);
        assertEquals(7L, book.getWorkId());
        verify(bookRepository).save(book);
    }
}
