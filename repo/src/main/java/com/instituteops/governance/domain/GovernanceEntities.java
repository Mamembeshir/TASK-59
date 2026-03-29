package com.instituteops.governance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_jobs")
class BulkJobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_checksum", nullable = false)
    private String fileChecksum;

    @Column(name = "started_by", nullable = false)
    private Long startedBy;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "summary", columnDefinition = "json")
    private String summary;

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public void setStartedBy(Long startedBy) {
        this.startedBy = startedBy;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}

@Entity
@Table(name = "consistency_issues")
class ConsistencyIssueEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_type", nullable = false)
    private String issueType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "issue_details", nullable = false, columnDefinition = "json")
    private String issueDetails;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    public Long getId() {
        return id;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getIssueDetails() {
        return issueDetails;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setIssueDetails(String issueDetails) {
        this.issueDetails = issueDetails;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}

@Entity
@Table(name = "duplicate_detection_results")
class DuplicateDetectionResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_entity_type", nullable = false)
    private String sourceEntityType;

    @Column(name = "source_entity_id", nullable = false)
    private Long sourceEntityId;

    @Column(name = "matched_entity_type", nullable = false)
    private String matchedEntityType;

    @Column(name = "matched_entity_id", nullable = false)
    private Long matchedEntityId;

    @Column(name = "match_mode", nullable = false)
    private String matchMode;

    @Column(name = "score", nullable = false)
    private java.math.BigDecimal score;

    @Column(name = "matched_on", nullable = false)
    private String matchedOn;

    @Column(name = "reviewed", nullable = false)
    private boolean reviewed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getMatchMode() {
        return matchMode;
    }

    public Long getSourceEntityId() {
        return sourceEntityId;
    }

    public Long getMatchedEntityId() {
        return matchedEntityId;
    }

    public java.math.BigDecimal getScore() {
        return score;
    }

    public String getMatchedOn() {
        return matchedOn;
    }

    public void setSourceEntityType(String sourceEntityType) {
        this.sourceEntityType = sourceEntityType;
    }

    public void setSourceEntityId(Long sourceEntityId) {
        this.sourceEntityId = sourceEntityId;
    }

    public void setMatchedEntityType(String matchedEntityType) {
        this.matchedEntityType = matchedEntityType;
    }

    public void setMatchedEntityId(Long matchedEntityId) {
        this.matchedEntityId = matchedEntityId;
    }

    public void setMatchMode(String matchMode) {
        this.matchMode = matchMode;
    }

    public void setScore(java.math.BigDecimal score) {
        this.score = score;
    }

    public void setMatchedOn(String matchedOn) {
        this.matchedOn = matchedOn;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

@Entity
@Table(name = "change_history")
class ChangeHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "old_data", columnDefinition = "json")
    private String oldData;

    @Column(name = "new_data", columnDefinition = "json")
    private String newData;

    @Column(name = "reason_code")
    private String reasonCode;

    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getOperation() {
        return operation;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getOldData() {
        return oldData;
    }

    public String getNewData() {
        return newData;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public void setOldData(String oldData) {
        this.oldData = oldData;
    }

    public void setNewData(String newData) {
        this.newData = newData;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }
}

@Entity
@Table(name = "recycle_bin")
class RecycleBinEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "deleted_by", nullable = false)
    private Long deletedBy;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    @Column(name = "purge_after", nullable = false)
    private LocalDateTime purgeAfter;

    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "restored", nullable = false)
    private boolean restored;

    @Column(name = "restored_by")
    private Long restoredBy;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Long getDeletedBy() {
        return deletedBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getPurgeAfter() {
        return purgeAfter;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isRestored() {
        return restored;
    }

    public Long getRestoredBy() {
        return restoredBy;
    }

    public LocalDateTime getRestoredAt() {
        return restoredAt;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setDeletedBy(Long deletedBy) {
        this.deletedBy = deletedBy;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setPurgeAfter(LocalDateTime purgeAfter) {
        this.purgeAfter = purgeAfter;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setRestored(boolean restored) {
        this.restored = restored;
    }

    public void setRestoredBy(Long restoredBy) {
        this.restoredBy = restoredBy;
    }

    public void setRestoredAt(LocalDateTime restoredAt) {
        this.restoredAt = restoredAt;
    }
}

@Entity
@Table(name = "sync_config")
class SyncConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_name", nullable = false)
    private String syncName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "lan_only", nullable = false)
    private boolean lanOnly;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "api_key_hash")
    private String apiKeyHash;

    public Long getId() {
        return id;
    }

    public String getSyncName() {
        return syncName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLanOnly() {
        return lanOnly;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }
}
