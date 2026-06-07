package com.justin.libradesk.domain.service;

import com.justin.libradesk.repository.AuthorityRepository;
import com.justin.libradesk.validation.ValidationException;

import java.util.List;
import java.util.Optional;

/**
 * Manages authority control: see-from variant headings that cross-reference an
 * authorized author/subject. {@link #resolveAuthor}/{@link #resolveSubject} let
 * the catalog normalise a variant heading to its authorized record during import.
 */
public class AuthorityService {

    private final AuthorityRepository authorityRepository;
    private final AuditLogService auditLogService;

    public AuthorityService(AuthorityRepository authorityRepository, AuditLogService auditLogService) {
        this.authorityRepository = authorityRepository;
        this.auditLogService = auditLogService;
    }

    public void addAuthorVariant(long authorId, String variantForm, String actor) {
        String form = require(variantForm);
        if (authorityRepository.findAuthorIdByVariant(form).isPresent()) {
            throw new ValidationException("That variant is already mapped to an author");
        }
        authorityRepository.addAuthorVariant(authorId, form);
        auditLogService.record(actor, "AUTHOR_VARIANT_ADDED", "Author", authorId, form);
    }

    public List<String> listAuthorVariants(long authorId) {
        return authorityRepository.listAuthorVariants(authorId);
    }

    public Optional<Long> resolveAuthor(String variantForm) {
        return authorityRepository.findAuthorIdByVariant(variantForm);
    }

    public void addSubjectVariant(long subjectId, String variantForm, String actor) {
        String form = require(variantForm);
        if (authorityRepository.findSubjectIdByVariant(form).isPresent()) {
            throw new ValidationException("That variant is already mapped to a subject");
        }
        authorityRepository.addSubjectVariant(subjectId, form);
        auditLogService.record(actor, "SUBJECT_VARIANT_ADDED", "Subject", subjectId, form);
    }

    public List<String> listSubjectVariants(long subjectId) {
        return authorityRepository.listSubjectVariants(subjectId);
    }

    public Optional<Long> resolveSubject(String variantForm) {
        return authorityRepository.findSubjectIdByVariant(variantForm);
    }

    private static String require(String variantForm) {
        if (variantForm == null || variantForm.isBlank()) {
            throw new ValidationException("Variant form is required");
        }
        return variantForm.trim();
    }
}
