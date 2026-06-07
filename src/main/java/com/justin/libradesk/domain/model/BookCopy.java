package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.CopyStatus;

import java.time.LocalDateTime;

/**
 * A physical, individually barcoded copy of a {@link Book}. Its
 * {@link CopyStatus} controls whether it can be loaned.
 */
public class BookCopy {

    private Long id;
    private Long bookId;
    private String barcode;
    private CopyStatus status;
    private String shelfLocation;
    private Long locationId;
    private LocalDateTime createdAt;

    public BookCopy() {
    }

    public BookCopy(Long id, Long bookId, String barcode, CopyStatus status,
                    String shelfLocation, LocalDateTime createdAt) {
        this.id = id;
        this.bookId = bookId;
        this.barcode = barcode;
        this.status = status;
        this.shelfLocation = shelfLocation;
        this.createdAt = createdAt;
    }

    /** @return {@code true} when this copy is free to be loaned. */
    public boolean isAvailable() {
        return status == CopyStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    /** @return the id of the assigned shelving {@code Location}, or {@code null} if unassigned. */
    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
