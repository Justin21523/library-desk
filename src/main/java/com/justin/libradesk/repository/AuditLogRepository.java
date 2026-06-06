package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.AuditLog;

public interface AuditLogRepository extends Repository<AuditLog, Long> {
    // Append-only; the inherited save() is the primary entry point.
}
