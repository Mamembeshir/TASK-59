package com.instituteops.recommender.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RecommenderServiceTest {

    @Mock
    private RecommenderEventRepository eventRepository;
    @Mock
    private RecommenderModelRepository modelRepository;
    @Mock
    private RecommenderModelVersionRepository versionRepository;
    @Mock
    private RecommenderRecommendationRepository recommendationRepository;
    @Mock
    private RecommenderIncrementalUpdateRepository incrementalUpdateRepository;
    @Mock
    private UserIdentityService userIdentityService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private RecommenderService service;

    @BeforeEach
    void setUp() {
        service = new RecommenderService(
            eventRepository,
            modelRepository,
            versionRepository,
            recommendationRepository,
            incrementalUpdateRepository,
            userIdentityService,
            userRepository,
            jdbcTemplate,
            new ObjectMapper()
        );
    }

    @Test
    void recordEvent_appliesDefaultsAndUppercases() {
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RecommenderEventEntity event = service.recordEvent(new RecommenderService.RecordEventRequest(
            "order",
            1L,
            "sku",
            5L,
            null,
            null,
            null
        ));

        assertThat(event.getEventType()).isEqualTo("ORDER");
        assertThat(event.getItemType()).isEqualTo("SKU");
        assertThat(event.getEventValue()).isEqualByComparingTo("1");
        assertThat(event.getSource()).isEqualTo("LOCAL");
        assertThat(event.isProcessed()).isFalse();
    }

    @Test
    void rollback_createsNewRollbackVersionAndClonesRecommendations() {
        RecommenderModelEntity model = org.mockito.Mockito.mock(RecommenderModelEntity.class);
        when(model.getId()).thenReturn(1L);

        RecommenderModelVersionEntity target = new RecommenderModelVersionEntity();
        target.setModelId(1L);
        target.setVersionNo(1);
        target.setTrainingStatus("COMPLETED");
        target.setCreatedAt(LocalDateTime.now().minusDays(2));

        RecommenderRecommendationEntity existing = new RecommenderRecommendationEntity();
        existing.setStudentId(1L);
        existing.setItemType("SKU");
        existing.setItemId(9L);
        existing.setScore(new BigDecimal("0.88"));
        existing.setRankNo(1);

        when(modelRepository.findByModelCode("USER_CF_COSINE")).thenReturn(Optional.of(model));
        when(versionRepository.findByModelIdAndVersionNo(anyLong(), org.mockito.ArgumentMatchers.eq(1))).thenReturn(Optional.of(target));
        when(versionRepository.findByModelIdOrderByVersionNoDesc(anyLong())).thenReturn(List.of(target));
        when(userIdentityService.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(versionRepository.save(any())).thenAnswer(i -> {
            RecommenderModelVersionEntity v = i.getArgument(0);
            if (v.getVersionNo() == 2) {
                v.setRollbackOfVersion(44L);
            }
            return v;
        });
        when(recommendationRepository.findByModelVersionIdOrderByStudentIdAscRankNoAsc(any())).thenReturn(List.of(existing));

        RecommenderService.RollbackResult rollback = service.rollbackModelVersion("USER_CF_COSINE", 1);

        assertThat(rollback.rolledBackToVersion()).isEqualTo(1);
        assertThat(rollback.newRollbackVersion()).isEqualTo(2);
        assertThat(rollback.restoredRecommendations()).isEqualTo(1);
        verify(recommendationRepository).saveAll(any());
    }
}
