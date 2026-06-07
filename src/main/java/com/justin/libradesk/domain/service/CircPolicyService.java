package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.CircPolicy;
import com.justin.libradesk.repository.CircPolicyRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resolves the circulation policy that governs a loan. A row in the policy
 * matrix (patron type × material type) wins; an exact material match overrides
 * the patron-type default. When the matrix has no applicable row the policy is
 * synthesized from the legacy flat {@link SettingsService} values, so behaviour
 * is unchanged for libraries that have not populated the matrix.
 */
public class CircPolicyService {

    private static final BigDecimal DEFAULT_FINE_PER_DAY = new BigDecimal("0.50");

    private final CircPolicyRepository circPolicyRepository;
    private final SettingsService settingsService;
    private final AuditLogService auditLogService;

    public CircPolicyService(CircPolicyRepository circPolicyRepository,
                             SettingsService settingsService,
                             AuditLogService auditLogService) {
        this.circPolicyRepository = circPolicyRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
    }

    /** @return the effective policy for a patron/material pair (never null). */
    public CircPolicy policyFor(PatronType patronType, MaterialType materialType) {
        return circPolicyRepository.findFor(patronType, materialType)
                .orElseGet(() -> fromSettings(patronType));
    }

    public List<CircPolicy> list() {
        return circPolicyRepository.findAll();
    }

    public CircPolicy save(CircPolicy policy, String actor) {
        CircPolicy saved = circPolicyRepository.save(policy);
        auditLogService.record(actor, "CIRC_POLICY_SAVED", "CircPolicy", saved.id(),
                saved.patronType() + "/" + saved.materialType());
        return saved;
    }

    public void delete(Long id, String actor) {
        circPolicyRepository.deleteById(id);
        auditLogService.record(actor, "CIRC_POLICY_DELETED", "CircPolicy", id, null);
    }

    /** Builds a policy from the legacy flat settings as a fallback default. */
    private CircPolicy fromSettings(PatronType patronType) {
        int loanDays = settingsService.getInt("loan.period.days", 14);
        int maxLoans = settingsService.getInt("borrow.limit." + patronType.name().toLowerCase(),
                defaultLimitFor(patronType));
        BigDecimal finePerDay = settingsService.getBigDecimal("fine.per.day", DEFAULT_FINE_PER_DAY);
        return new CircPolicy(null, patronType, null, loanDays, maxLoans,
                2, 5, finePerDay, BigDecimal.ZERO, 0);
    }

    private static int defaultLimitFor(PatronType type) {
        return switch (type) {
            case STUDENT -> 5;
            case STAFF -> 10;
            case PUBLIC -> 3;
        };
    }
}
