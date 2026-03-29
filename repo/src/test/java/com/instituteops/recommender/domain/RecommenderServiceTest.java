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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
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

    @Test
    void trainNewVersion_userCfGeneratesCrossItemRecommendationsForSimilarStudents() {
        RecommenderModelEntity model = new RecommenderModelEntity();
        setId(model, 10L);
        model.setModelCode("USER_CF_COSINE");
        model.setAlgorithmFamily("USER_CF");
        model.setSimilarityMetric("COSINE");
        model.setTimeDecayHalfLifeDays(90);
        model.setPopularityPenalty(new BigDecimal("0.01"));

        when(modelRepository.findByModelCode("USER_CF_COSINE")).thenReturn(Optional.of(model));
        when(versionRepository.findByModelIdOrderByVersionNoDesc(10L)).thenReturn(List.of());
        when(userIdentityService.resolveCurrentUserId()).thenReturn(Optional.of(7L));

        RecommenderModelVersionEntity running = new RecommenderModelVersionEntity();
        setId(running, 100L);
        running.setModelId(10L);
        running.setVersionNo(1);

        when(versionRepository.save(any())).thenReturn(running);
        when(recommendationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        RecommenderEventEntity e1 = event(1L, 1L, 101L, "2.0");
        RecommenderEventEntity e2 = event(2L, 1L, 102L, "1.0");
        RecommenderEventEntity e3 = event(3L, 2L, 101L, "2.0");
        RecommenderEventEntity e4 = event(4L, 2L, 103L, "3.0");
        when(eventRepository.findByOccurredAtBetweenOrderByOccurredAtAsc(any(), any())).thenReturn(List.of(e1, e2, e3, e4));

        RecommenderService.TrainingResult result = service.trainNewVersion("USER_CF_COSINE", 2);

        assertThat(result.recommendationsGenerated()).isGreaterThan(0);
        assertThat(result.metrics()).containsEntry("algorithmFamily", "USER_CF");

        ArgumentCaptor<List<RecommenderRecommendationEntity>> recCaptor = ArgumentCaptor.forClass(List.class);
        verify(recommendationRepository).saveAll(recCaptor.capture());
        List<RecommenderRecommendationEntity> saved = new ArrayList<>(recCaptor.getValue());

        Map<Long, List<RecommenderRecommendationEntity>> byStudent = saved.stream()
            .collect(java.util.stream.Collectors.groupingBy(RecommenderRecommendationEntity::getStudentId));

        assertThat(byStudent.get(1L)).isNotNull();
        assertThat(byStudent.get(1L).stream().map(RecommenderRecommendationEntity::getItemId)).contains(103L);
        assertThat(byStudent.get(1L).stream().map(RecommenderRecommendationEntity::getItemId)).doesNotContain(101L, 102L);
    }

    private static RecommenderEventEntity event(Long id, Long studentId, Long itemId, String value) {
        RecommenderEventEntity event = new RecommenderEventEntity();
        setId(event, id);
        event.setEventType("ORDER");
        event.setStudentId(studentId);
        event.setItemType("SKU");
        event.setItemId(itemId);
        event.setEventValue(new BigDecimal(value));
        event.setOccurredAt(LocalDateTime.now().minusDays(1));
        event.setSource("TEST");
        event.setProcessed(false);
        return event;
    }

    private static void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
