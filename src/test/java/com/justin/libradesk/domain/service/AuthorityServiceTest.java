package com.justin.libradesk.domain.service;

import com.justin.libradesk.repository.AuthorityRepository;
import com.justin.libradesk.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private AuditLogService auditLogService;

    private AuthorityService authorityService;

    @BeforeEach
    void setUp() {
        authorityService = new AuthorityService(authorityRepository, auditLogService);
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
    void addAuthorVariantRejectsBlank() {
        assertThrows(ValidationException.class, () -> authorityService.addAuthorVariant(5L, "  ", "admin"));
    }

    @Test
    void resolveAuthorReturnsAuthorizedId() {
        when(authorityRepository.findAuthorIdByVariant("Clemens, Samuel")).thenReturn(Optional.of(5L));

        assertEquals(Optional.of(5L), authorityService.resolveAuthor("Clemens, Samuel"));
    }
}
