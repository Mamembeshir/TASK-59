package com.instituteops.governance.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface BulkJobRepository extends JpaRepository<BulkJobEntity, Long> {
}

interface ConsistencyIssueRepository extends JpaRepository<ConsistencyIssueEntity, Long> {
}

interface DuplicateDetectionResultRepository extends JpaRepository<DuplicateDetectionResultEntity, Long> {
}

interface ChangeHistoryRepository extends JpaRepository<ChangeHistoryEntity, Long> {
}

interface RecycleBinRepository extends JpaRepository<RecycleBinEntity, Long> {
}

interface SyncConfigRepository extends JpaRepository<SyncConfigEntity, Long> {
}
