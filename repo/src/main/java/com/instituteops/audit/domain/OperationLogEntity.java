package com.instituteops.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs")
public class OperationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", length = 64)
    private String actorUsername;

    @Column(name = "role_snapshot", length = 255)
    private String roleSnapshot;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public void setRoleSnapshot(String roleSnapshot) {
        this.roleSnapshot = roleSnapshot;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
