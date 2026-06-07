package com.justin.libradesk.domain.service;

import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.repository.SettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves runtime-editable settings. A value set in the {@code settings} table
 * (via the Settings screen) overrides the corresponding {@code application.properties}
 * default, so changes take effect without restarting. This is the single source
 * of truth other services read for the loan period and borrowing limits.
 */
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    /** Keys surfaced on the Settings screen, in display order. */
    public static final List<String> EDITABLE_KEYS = List.of(
            "loan.period.days",
            "borrow.limit.student",
            "borrow.limit.staff",
            "borrow.limit.public",
            "overdue.sweep.minutes",
            "reservation.ready.expiry.days",
            "notice.due.soon.days",
            "overdue.block.count");

    private final SettingsRepository settingsRepository;
    private final AppConfig defaults;
    private final AuditLogService auditLogService;

    public SettingsService(SettingsRepository settingsRepository, AppConfig defaults,
                           AuditLogService auditLogService) {
        this.settingsRepository = settingsRepository;
        this.defaults = defaults;
        this.auditLogService = auditLogService;
    }

    public int getInt(String key, int fallback) {
        Optional<String> override = settingsRepository.find(key);
        if (override.isPresent()) {
            try {
                return Integer.parseInt(override.get().trim());
            } catch (NumberFormatException e) {
                log.warn("Setting {} is not an integer ('{}'); using default", key, override.get());
            }
        }
        return defaults.getInt(key, fallback);
    }

    public String getString(String key, String fallback) {
        return settingsRepository.find(key).orElseGet(() -> defaults.getString(key, fallback));
    }

    /** Reads a decimal setting (e.g. a money amount); falls back on a bad/absent value. */
    public BigDecimal getBigDecimal(String key, BigDecimal fallback) {
        try {
            return new BigDecimal(getString(key, fallback.toPlainString()).trim());
        } catch (NumberFormatException e) {
            log.warn("Setting {} is not a number; using default {}", key, fallback);
            return fallback;
        }
    }

    /** Updates an integer setting (validated as a non-negative number). */
    public void setInt(String key, int value, String actor) {
        if (value < 0) {
            throw new IllegalArgumentException("Setting " + key + " must not be negative");
        }
        settingsRepository.put(key, Integer.toString(value));
        auditLogService.record(actor, "SETTINGS_CHANGED", "Setting", null, key + "=" + value);
    }

    /** @return the current effective value of each editable key (override or default). */
    public Map<String, String> effectiveSettings() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : EDITABLE_KEYS) {
            result.put(key, getString(key, ""));
        }
        return result;
    }
}
