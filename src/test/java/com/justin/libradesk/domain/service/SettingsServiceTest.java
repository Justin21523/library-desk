package com.justin.libradesk.domain.service;

import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private SettingsRepository settingsRepository;
    @Mock
    private AuditLogService auditLogService;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        // Real config so the default (loan.period.days=14) comes from the bundled properties.
        settingsService = new SettingsService(settingsRepository, AppConfig.load(), auditLogService);
    }

    @Test
    void getIntFallsBackToDefaultWhenNoOverride() {
        when(settingsRepository.find("loan.period.days")).thenReturn(Optional.empty());

        assertEquals(14, settingsService.getInt("loan.period.days", 14));
    }

    @Test
    void getIntPrefersStoredOverride() {
        when(settingsRepository.find("loan.period.days")).thenReturn(Optional.of("21"));

        assertEquals(21, settingsService.getInt("loan.period.days", 14));
    }

    @Test
    void getIntIgnoresNonNumericOverride() {
        when(settingsRepository.find("loan.period.days")).thenReturn(Optional.of("oops"));

        assertEquals(14, settingsService.getInt("loan.period.days", 14));
    }

    @Test
    void setIntPersistsAndAudits() {
        settingsService.setInt("loan.period.days", 30, "admin");

        verify(settingsRepository).put("loan.period.days", "30");
        verify(auditLogService).record("admin", "SETTINGS_CHANGED", "Setting", null, "loan.period.days=30");
    }

    @Test
    void setIntRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> settingsService.setInt("loan.period.days", -1, "admin"));
    }

    @Test
    void effectiveSettingsCoversEveryEditableKey() {
        lenient().when(settingsRepository.find(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        assertEquals(SettingsService.EDITABLE_KEYS.size(), settingsService.effectiveSettings().size());
    }
}
