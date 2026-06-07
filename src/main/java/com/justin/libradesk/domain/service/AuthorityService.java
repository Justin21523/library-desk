package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Subject;
import com.justin.libradesk.dto.AuthoritySuggestion;
import com.justin.libradesk.infrastructure.marc.LocAuthorityClient;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.AuthorityRepository;
import com.justin.libradesk.repository.SubjectRepository;
import com.justin.libradesk.validation.ValidationException;

import java.util.List;
import java.util.Optional;

/**
 * Authority control: see-from variants, global heading change (rename),
 * heading merge/de-duplication, and online authorized-heading lookup
 * (id.loc.gov). Because bibs link to authors/subjects by id, renaming or merging
 * a heading is reflected by every record automatically.
 */
public class AuthorityService {

    private final AuthorityRepository authorityRepository;
    private final AuthorRepository authorRepository;
    private final SubjectRepository subjectRepository;
    private final LocAuthorityClient locAuthorityClient;
    private final AuditLogService auditLogService;

    public AuthorityService(AuthorityRepository authorityRepository,
                            AuthorRepository authorRepository,
                            SubjectRepository subjectRepository,
                            LocAuthorityClient locAuthorityClient,
                            AuditLogService auditLogService) {
        this.authorityRepository = authorityRepository;
        this.authorRepository = authorRepository;
        this.subjectRepository = subjectRepository;
        this.locAuthorityClient = locAuthorityClient;
        this.auditLogService = auditLogService;
    }

    // --- See-from variants ---

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

    // --- Global heading change (rename) ---

    public void renameAuthor(long authorId, String newName, String actor) {
        String name = require(newName);
        authorRepository.findAll().stream()
                .filter(a -> !a.id().equals(authorId) && a.name().equalsIgnoreCase(name))
                .findAny().ifPresent(a -> {
                    throw new ValidationException("An author with that name already exists");
                });
        authorRepository.save(new Author(authorId, name));
        auditLogService.record(actor, "AUTHOR_RENAMED", "Author", authorId, name);
    }

    public void renameSubject(long subjectId, String newTerm, String actor) {
        String term = require(newTerm);
        subjectRepository.findAll().stream()
                .filter(s -> !s.id().equals(subjectId) && s.term().equalsIgnoreCase(term))
                .findAny().ifPresent(s -> {
                    throw new ValidationException("A subject with that term already exists");
                });
        subjectRepository.save(new Subject(subjectId, term));
        auditLogService.record(actor, "SUBJECT_RENAMED", "Subject", subjectId, term);
    }

    // --- Merge / de-duplicate headings ---

    public void mergeAuthor(long fromId, long intoId, String actor) {
        if (fromId == intoId) {
            throw new ValidationException("Cannot merge a heading into itself");
        }
        authorityRepository.mergeAuthor(fromId, intoId);
        auditLogService.record(actor, "AUTHOR_MERGED", "Author", intoId, "merged from " + fromId);
    }

    public void mergeSubject(long fromId, long intoId, String actor) {
        if (fromId == intoId) {
            throw new ValidationException("Cannot merge a heading into itself");
        }
        authorityRepository.mergeSubject(fromId, intoId);
        auditLogService.record(actor, "SUBJECT_MERGED", "Subject", intoId, "merged from " + fromId);
    }

    // --- Online authorized-heading lookup (id.loc.gov) ---

    public List<AuthoritySuggestion> suggestNames(String query) {
        return locAuthorityClient.suggestNames(query);
    }

    public List<AuthoritySuggestion> suggestSubjects(String query) {
        return locAuthorityClient.suggestSubjects(query);
    }

    private static String require(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("A value is required");
        }
        return value.trim();
    }
}
