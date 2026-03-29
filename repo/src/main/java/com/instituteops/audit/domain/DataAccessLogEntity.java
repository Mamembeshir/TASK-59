package com.instituteops.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_access_logs")
public class DataAccessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "access_type", nullable = false, length = 32)
    private String accessType;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
