package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.ELink;
import com.justin.libradesk.repository.ELinkRepository;
import com.justin.libradesk.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ELinkServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Mock
    private ELinkRepository eLinkRepository;
    @Mock
    private AuditLogService auditLogService;

    private ELinkService eLinkService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
        // Fake link checker: always reports 200.
        eLinkService = new ELinkService(eLinkRepository, url -> 200, auditLogService, clock);
    }

    @Test
    void addRejectsBlankUrl() {
        assertThrows(ValidationException.class, () -> eLinkService.add(1L, "  ", "label", "admin"));
        verify(eLinkRepository, never()).save(any());
    }

    @Test
    void checkLinksStoresStatusAndTimestamp() {
        ELink link = new ELink(5L, 1L, "https://example.com", "Site", null, null);
        when(eLinkRepository.findByBook(1L)).thenReturn(List.of(link), List.of(link));
        when(eLinkRepository.save(any(ELink.class))).thenAnswer(i -> i.getArgument(0));

        eLinkService.checkLinks(1L, "admin");

        ArgumentCaptor<ELink> captor = ArgumentCaptor.forClass(ELink.class);
        verify(eLinkRepository).save(captor.capture());
        assertEquals(200, captor.getValue().lastStatus());
        assertEquals(NOW, captor.getValue().lastChecked());
    }
}
