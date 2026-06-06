package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Book;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends Repository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    /** Case-insensitive partial match on the title. */
    List<Book> searchByTitle(String fragment);
}
