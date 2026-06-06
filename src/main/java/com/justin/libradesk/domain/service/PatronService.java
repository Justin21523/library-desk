package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.validation.PatronValidator;
import com.justin.libradesk.validation.ValidationException;

import java.util.List;

/**
 * Manages patron records: registration, lookup, and status changes such as
 * suspension (which the circulation rules then honour).
 */
public class PatronService {

    private final PatronRepository patronRepository;
    private final AuditLogService auditLogService;

    public PatronService(PatronRepository patronRepository, AuditLogService auditLogService) {
        this.patronRepository = patronRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Registers a new patron after field validation and uniqueness check.
     *
     * @throws ValidationException if data is invalid or the membership number is taken
     */
    public Patron register(Patron patron, String actor) {
        PatronValidator.validate(patron);
        patronRepository.findByMembershipNo(patron.getMembershipNo()).ifPresent(existing -> {
            throw new ValidationException("Membership number already exists: " + patron.getMembershipNo());
        });
        Patron saved = patronRepository.save(patron);
        auditLogService.record(actor, "PATRON_REGISTERED", "Patron", saved.getId(),
                patron.getMembershipNo());
        return saved;
    }

    public List<Patron> listAll() {
        return patronRepository.findAll();
    }

    /** Suspends a patron so the borrowing rules will block further loans. */
    public Patron suspend(Long patronId, String actor) {
        Patron patron = patronRepository.findById(patronId)
                .orElseThrow(() -> new ValidationException("Patron not found: " + patronId));
        patron.setStatus(PatronStatus.SUSPENDED);
        Patron saved = patronRepository.save(patron);
        auditLogService.record(actor, "PATRON_SUSPENDED", "Patron", patronId, null);
        return saved;
    }
}
