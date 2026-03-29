package com.instituteops.audit;

import com.instituteops.audit.domain.DataAccessLogEntity;
import com.instituteops.audit.domain.OperationLogEntity;
import com.instituteops.audit.repo.DataAccessLogRepository;
import com.instituteops.audit.repo.OperationLogRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final OperationLogRepository operationLogRepository;
    private final DataAccessLogRepository dataAccessLogRepository;

    public AuditLogService(
        OperationLogRepository operationLogRepository,
        DataAccessLogRepository dataAccessLogRepository
    ) {
        this.operationLogRepository = operationLogRepository;
        this.dataAccessLogRepository = dataAccessLogRepository;
    }

    @Transactional
    public void logOperation(
        Long actorUserId,
        String actorUsername,
        String roleSnapshot,
        String action,
        String entityType,
        Long entityId,
        String requestId,
        String clientIp,
        boolean success,
        String message
    ) {
        OperationLogEntity entry = new OperationLogEntity();
        entry.setActorUserId(actorUserId);
        entry.setActorUsername(actorUsername);
        entry.setRoleSnapshot(roleSnapshot);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setRequestId(requestId);
        entry.setClientIp(clientIp);
        entry.setSuccess(success);
        entry.setMessage(message);
        entry.setOccurredAt(LocalDateTime.now());
        operationLogRepository.save(entry);
    }

    @Transactional
    public void logDataAccess(
        Long actorUserId,
        String entityType,
        Long entityId,
        String accessType,
        String reason,
        String requestId
    ) {
        DataAccessLogEntity entry = new DataAccessLogEntity();
        entry.setActorUserId(actorUserId);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAccessType(accessType);
        entry.setReason(reason);
        entry.setRequestId(requestId);
        entry.setOccurredAt(LocalDateTime.now());
        dataAccessLogRepository.save(entry);
    }
}
