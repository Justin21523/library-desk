package com.justin.libradesk.domain.model;

import java.time.LocalDateTime;

/**
 * An append-only record of an important operation (a loan, a return, a
 * configuration change, ...). Written through {@code AuditLogService}.
 */
public class AuditLog {

    private Long id;
    private String actor;
    private String action;
    private String entityType;
    private Long entityId;
    private String detail;
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(Long id, String actor, String action, String entityType, Long entityId,
                    String detail, LocalDateTime createdAt) {
        this.id = id;
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
