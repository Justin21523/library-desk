package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.infrastructure.marc.LocAuthorityClient;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.AuthorityRepository;
import com.justin.libradesk.repository.SubjectRepository;
import com.justin.libradesk.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorityServiceTest {

    @Mock
    private AuthorityRepository authorityRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private LocAuthorityClient locAuthorityClient;
    @Mock
    private AuditLogService auditLogService;

    private AuthorityService authorityService;

    @BeforeEach
    void setUp() {
        authorityService = new AuthorityService(authorityRepository, authorRepository, subjectRepository,
                locAuthorityClient, auditLogService);
    }

    @Test
    void addAuthorVariantStoresAndAudits() {
        when(authorityRepository.findAuthorIdByVariant("Twain, Mark")).thenReturn(Optional.empty());

        authorityService.addAuthorVariant(5L, "Twain, Mark", "admin");

        verify(authorityRepository).addAuthorVariant(5L, "Twain, Mark");
        verify(auditLogService).record("admin", "AUTHOR_VARIANT_ADDED", "Author", 5L, "Twain, Mark");
    }

    @Test
    void addAuthorVariantRejectsDuplicate() {
        when(authorityRepository.findAuthorIdByVariant("Twain, Mark")).thenReturn(Optional.of(9L));

        assertThrows(ValidationException.class,
                () -> authorityService.addAuthorVariant(5L, "Twain, Mark", "admin"));
        verify(authorityRepository, never()).addAuthorVariant(anyLong(), anyString());
    }

    @Test
    void resolveAuthorReturnsAuthorizedId() {
        when(authorityRepository.findAuthorIdByVariant("Clemens, Samuel")).thenReturn(Optional.of(5L));

        assertEquals(Optional.of(5L), authorityService.resolveAuthor("Clemens, Samuel"));
    }

    @Test
    void renameAuthorRejectsDuplicateName() {
        when(authorRepository.findAll()).thenReturn(List.of(new Author(2L, "Existing Name")));

        assertThrows(ValidationException.class,
                () -> authorityService.renameAuthor(1L, "Existing Name", "admin"));
        verify(authorRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void renameAuthorSavesAndAudits() {
        when(authorRepository.findAll()).thenReturn(List.of());

        authorityService.renameAuthor(1L, "Twain, Mark", "admin");

        verify(authorRepository).save(new Author(1L, "Twain, Mark"));
        verify(auditLogService).record("admin", "AUTHOR_RENAMED", "Author", 1L, "Twain, Mark");
    }

    @Test
    void mergeAuthorDelegatesAndAudits() {
        authorityService.mergeAuthor(3L, 7L, "admin");

        verify(authorityRepository).mergeAuthor(3L, 7L);
        verify(auditLogService).record("admin", "AUTHOR_MERGED", "Author", 7L, "merged from 3");
    }

    @Test
    void mergeAuthorRejectsSelfMerge() {
        assertThrows(ValidationException.class, () -> authorityService.mergeAuthor(5L, 5L, "admin"));
        verify(authorityRepository, never()).mergeAuthor(anyLong(), anyLong());
    }
}
