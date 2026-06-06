package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.AuditLog;
import com.justin.libradesk.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes audit records for important operations. Other services depend on this
 * rather than on the repository directly, so the "what counts as auditable"
 * policy lives in one place.
 */
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditLogService(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    /**
     * Records an auditable event.
     *
     * @param actor      username performing the action
     * @param action     stable action code, e.g. {@code LOAN_CREATED}
     * @param entityType affected entity type, e.g. {@code Loan} (nullable)
     * @param entityId   affected entity id (nullable)
     * @param detail     free-text detail (nullable)
     */
    public AuditLog record(String actor, String action, String entityType, Long entityId, String detail) {
        AuditLog entry = new AuditLog(null, actor, action, entityType, entityId, detail,
                LocalDateTime.now(clock));
        AuditLog saved = auditLogRepository.save(entry);
        log.debug("Audit: actor={} action={} entity={}#{}", actor, action, entityType, entityId);
        return saved;
    }

    /** @return all audit entries, newest first (for the audit viewer). */
    public List<AuditLog> recent() {
        return auditLogRepository.findAll();
    }
}

