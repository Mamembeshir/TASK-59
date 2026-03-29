package com.instituteops.governance.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class GovernanceAuditService {

    private final ChangeHistoryRepository changeHistoryRepository;
    private final RecycleBinRepository recycleBinRepository;
    private final UserIdentityService userIdentityService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public GovernanceAuditService(
        ChangeHistoryRepository changeHistoryRepository,
        RecycleBinRepository recycleBinRepository,
        UserIdentityService userIdentityService,
        UserRepository userRepository,
        ObjectMapper objectMapper
    ) {
        this.changeHistoryRepository = changeHistoryRepository;
        this.recycleBinRepository = recycleBinRepository;
        this.userIdentityService = userIdentityService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordChange(String entityType, Long entityId, String operation, Object oldData, Object newData, String reasonCode) {
        ChangeHistoryEntity entity = new ChangeHistoryEntity();
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setOperation(operation);
        entity.setChangedBy(currentUserId());
        entity.setChangedAt(LocalDateTime.now());
        entity.setOldData(toJson(oldData));
        entity.setNewData(toJson(newData));
        entity.setReasonCode(reasonCode);
        changeHistoryRepository.save(entity);
    }

    @Transactional
    public RecycleBinEntity recordRecycle(String entityType, Long entityId, Object payload) {
        RecycleBinEntity entity = new RecycleBinEntity();
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setDeletedBy(currentUserId());
        entity.setDeletedAt(LocalDateTime.now());
        entity.setPurgeAfter(LocalDateTime.now().plusDays(30));
        entity.setPayload(toJson(payload));
        entity.setRestored(false);
        return recycleBinRepository.save(entity);
    }

    @Transactional
    public void markRestored(String entityType, Long entityId) {
        for (RecycleBinEntity entry : recycleBinRepository.findByRestoredFalseOrderByDeletedAtDesc()) {
            if (!entityType.equals(entry.getEntityType()) || !entityId.equals(entry.getEntityId())) {
                continue;
            }
            entry.setRestored(true);
            entry.setRestoredBy(currentUserId());
            entry.setRestoredAt(LocalDateTime.now());
            recycleBinRepository.save(entry);
            break;
        }
    }

    private Long currentUserId() {
        return userIdentityService.resolveCurrentUserId().orElseGet(() -> userRepository.findIdByUsername("sysadmin").orElse(1L));
    }

    private String toJson(Object source) {
        if (source == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"" + ex.getOriginalMessage().replace("\"", "'") + "\"}";
        }
    }
}
