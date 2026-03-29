package com.instituteops.governance.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface BulkJobRepository extends JpaRepository<BulkJobEntity, Long> {
}

interface ConsistencyIssueRepository extends JpaRepository<ConsistencyIssueEntity, Long> {

    void deleteByResolvedFalseAndIssueTypeIn(List<String> issueTypes);

    List<ConsistencyIssueEntity> findTop200ByOrderByDetectedAtDesc();
}

interface DuplicateDetectionResultRepository extends JpaRepository<DuplicateDetectionResultEntity, Long> {

    void deleteByReviewedFalse();

    List<DuplicateDetectionResultEntity> findTop200ByOrderByCreatedAtDesc();
}

interface ChangeHistoryRepository extends JpaRepository<ChangeHistoryEntity, Long> {

    List<ChangeHistoryEntity> findTop200ByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, Long entityId);
}

interface RecycleBinRepository extends JpaRepository<RecycleBinEntity, Long> {

    List<RecycleBinEntity> findByRestoredFalseOrderByDeletedAtDesc();

    List<RecycleBinEntity> findByRestoredFalseAndPurgeAfterBefore(LocalDateTime now);
}

interface SyncConfigRepository extends JpaRepository<SyncConfigEntity, Long> {

    java.util.Optional<SyncConfigEntity> findBySyncName(String syncName);
}
