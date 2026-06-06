package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.BookCopy;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends Repository<BookCopy, Long> {

    Optional<BookCopy> findByBarcode(String barcode);

    List<BookCopy> findByBookId(Long bookId);

    /** @return available copies for the given book, in arbitrary order. */
    List<BookCopy> findAvailableByBookId(Long bookId);
}
