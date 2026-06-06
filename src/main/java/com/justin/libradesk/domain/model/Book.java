package com.justin.libradesk.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A bibliographic record in the catalog. Physical, loanable items are
 * represented separately by {@link BookCopy}; a book may have many copies.
 */
public class Book {

    private Long id;
    private String isbn;
    private String title;
    private Long publisherId;
    private Long categoryId;
    private Integer publishedYear;
    private LocalDateTime createdAt;

    /** Author ids associated with this book (populated by the catalog service). */
    private final List<Long> authorIds = new ArrayList<>();

    public Book() {
    }

    public Book(Long id, String isbn, String title, Long publisherId, Long categoryId,
                Integer publishedYear, LocalDateTime createdAt) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.publisherId = publisherId;
        this.categoryId = categoryId;
        this.publishedYear = publishedYear;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Long> getAuthorIds() {
        return authorIds;
    }
}
