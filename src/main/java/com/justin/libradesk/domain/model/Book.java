package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.ClassificationScheme;
import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.RecordStatus;

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

    // Richer MARC-derived bibliographic fields (Phase 10). Populated by MARC
    // import or, for some, the catalog form; null when unknown.
    private String edition;
    private String pubPlace;
    private String extent;
    private String series;
    private String language;
    private MaterialType materialType;
    private String controlNumber;
    private String summary;
    private String marcXml;
    private String callNumber;
    private ClassificationScheme classificationScheme;
    private RecordStatus recordStatus;
    private Long workId;

    /** Author ids associated with this book (populated by the catalog service). */
    private final List<Long> authorIds = new ArrayList<>();

    /** Subject ids associated with this book (MARC 6xx). */
    private final List<Long> subjectIds = new ArrayList<>();

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

    public List<Long> getSubjectIds() {
        return subjectIds;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getPubPlace() {
        return pubPlace;
    }

    public void setPubPlace(String pubPlace) {
        this.pubPlace = pubPlace;
    }

    public String getExtent() {
        return extent;
    }

    public void setExtent(String extent) {
        this.extent = extent;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public String getControlNumber() {
        return controlNumber;
    }

    public void setControlNumber(String controlNumber) {
        this.controlNumber = controlNumber;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getMarcXml() {
        return marcXml;
    }

    public void setMarcXml(String marcXml) {
        this.marcXml = marcXml;
    }

    public String getCallNumber() {
        return callNumber;
    }

    public void setCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }

    public ClassificationScheme getClassificationScheme() {
        return classificationScheme;
    }

    public void setClassificationScheme(ClassificationScheme classificationScheme) {
        this.classificationScheme = classificationScheme;
    }

    public RecordStatus getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(RecordStatus recordStatus) {
        this.recordStatus = recordStatus;
    }

    /** @return the id of the FRBR {@code Work} this bib belongs to, or {@code null} if ungrouped. */
    public Long getWorkId() {
        return workId;
    }

    public void setWorkId(Long workId) {
        this.workId = workId;
    }
}
