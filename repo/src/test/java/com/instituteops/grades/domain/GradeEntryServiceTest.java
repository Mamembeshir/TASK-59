package com.instituteops.grades.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeEntryServiceTest {

    private static final String RULE_JSON = """
        {
          "creditPolicy": {"baseCredits": 3, "passingPercent": 60, "partialCreditEnabled": true},
          "gradingScale": [
            {"minPercent": 90, "grade": "A", "gpaPoints": 4.0},
            {"minPercent": 80, "grade": "B", "gpaPoints": 3.0},
            {"minPercent": 70, "grade": "C", "gpaPoints": 2.0},
            {"minPercent": 60, "grade": "D", "gpaPoints": 1.0},
            {"minPercent": 0, "grade": "F", "gpaPoints": 0.0}
          ]
        }
        """;

    @Mock
    private GradeRuleSetRepository gradeRuleSetRepository;
    @Mock
    private GradeRuleVersionRepository gradeRuleVersionRepository;
    @Mock
    private OverrideReasonCodeRepository overrideReasonCodeRepository;
    @Mock
    private GradeLedgerEntryRepository gradeLedgerEntryRepository;
    @Mock
    private UserIdentityService userIdentityService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GradeRuleSetEntity ruleSet;
    @Mock
    private GradeRuleVersionEntity ruleVersion;

    private GradeEntryService service;

    @BeforeEach
    void setUp() {
        service = new GradeEntryService(
            gradeRuleSetRepository,
            gradeRuleVersionRepository,
            overrideReasonCodeRepository,
            gradeLedgerEntryRepository,
            userIdentityService,
            userRepository,
            new ObjectMapper()
        );
    }

    private void stubActiveRules() {
        when(gradeRuleSetRepository.findActiveAt(any())).thenReturn(List.of(ruleSet));
        when(ruleSet.getId()).thenReturn(1L);
        when(gradeRuleVersionRepository.findTopByRulesetIdOrderByVersionNoDesc(1L)).thenReturn(Optional.of(ruleVersion));
        when(ruleVersion.getRuleJson()).thenReturn(RULE_JSON);
    }

    @Test
    void preview_calculatesDeterministicImpactFromRules() {
        stubActiveRules();

        GradeEntryService.GradeImpact impact = service.preview(new GradeEntryService.GradeEntryRequest(
            1L, 1L, null, null, "MIDTERM", new BigDecimal("95"), new BigDecimal("100"), "ENTRY", null, null
        ));

        assertThat(impact.percent()).isEqualByComparingTo("95.000");
        assertThat(impact.gradeLetter()).isEqualTo("A");
        assertThat(impact.creditsEarned()).isEqualByComparingTo("3.000");
        assertThat(impact.gpaImpact()).isEqualByComparingTo("12.000");
    }

    @Test
    void appendLedgerEntry_editIncludesDeltaAgainstPreviousEntry() {
        stubActiveRules();
        when(ruleVersion.getId()).thenReturn(2L);

        GradeLedgerEntryEntity previous = new GradeLedgerEntryEntity();
        previous.setStudentId(1L);
        previous.setClassId(1L);
        previous.setAssessmentKey("MIDTERM");
        previous.setRawScore(new BigDecimal("80.000"));
        previous.setCreditsEarned(new BigDecimal("3.000"));
        previous.setGpaPoints(new BigDecimal("9.000"));

        when(gradeLedgerEntryRepository.findById(anyLong())).thenReturn(Optional.of(previous));
        when(userIdentityService.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(gradeLedgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GradeLedgerEntryEntity saved = service.appendLedgerEntry(new GradeEntryService.GradeEntryRequest(
            1L, 1L, null, null, "MIDTERM", new BigDecimal("90"), new BigDecimal("100"), "EDIT", null, 55L
        ));

        assertThat(saved.getGradeLetter()).isEqualTo("A");
        assertThat(saved.getDeltaScore()).isEqualByComparingTo("10.000");
        assertThat(saved.getDeltaCredits()).isEqualByComparingTo("0.000");
        assertThat(saved.getDeltaGpaPoints()).isEqualByComparingTo("3.000");
    }

    @Test
    void manualOverride_requiresImmutableReasonCode() {
        assertThatThrownBy(() -> service.appendLedgerEntry(new GradeEntryService.GradeEntryRequest(
            1L, 1L, null, null, "MIDTERM", new BigDecimal("90"), new BigDecimal("100"), "MANUAL_OVERRIDE", null, 10L
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Immutable reason code is required");
    }
}
