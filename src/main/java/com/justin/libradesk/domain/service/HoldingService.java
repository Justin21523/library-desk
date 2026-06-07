package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Holding;
import com.justin.libradesk.repository.HoldingRepository;

import java.util.List;

/**
 * Manages MFHD-style holdings records for a bib. A holding summarizes what the
 * library owns at a location; copies may link to it via {@code holding_id}.
 */
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final AuditLogService auditLogService;

    public HoldingService(HoldingRepository holdingRepository, AuditLogService auditLogService) {
        this.holdingRepository = holdingRepository;
        this.auditLogService = auditLogService;
    }

    public List<Holding> listForBook(Long bookId) {
        return holdingRepository.findByBook(bookId);
    }

    public Holding save(Holding holding, String actor) {
        Holding saved = holdingRepository.save(holding);
        auditLogService.record(actor, "HOLDING_SAVED", "Holding", saved.id(), "book=" + saved.bookId());
        return saved;
    }

    public void delete(Long id, String actor) {
        holdingRepository.deleteById(id);
        auditLogService.record(actor, "HOLDING_DELETED", "Holding", id, null);
    }
}
