package com.instituteops.grades.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GradeEntryService {

    private static final List<String> OPERATION_TYPES = List.of("ENTRY", "EDIT", "RECALCULATION", "MANUAL_OVERRIDE");

    private final GradeRuleSetRepository gradeRuleSetRepository;
    private final GradeRuleVersionRepository gradeRuleVersionRepository;
    private final OverrideReasonCodeRepository overrideReasonCodeRepository;
    private final GradeLedgerEntryRepository gradeLedgerEntryRepository;
    private final GradeRecalculationRepository gradeRecalculationRepository;
    private final GradeRecalculationDeltaRepository gradeRecalculationDeltaRepository;
    private final UserIdentityService userIdentityService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public GradeEntryService(
        GradeRuleSetRepository gradeRuleSetRepository,
        GradeRuleVersionRepository gradeRuleVersionRepository,
        OverrideReasonCodeRepository overrideReasonCodeRepository,
        GradeLedgerEntryRepository gradeLedgerEntryRepository,
        GradeRecalculationRepository gradeRecalculationRepository,
        GradeRecalculationDeltaRepository gradeRecalculationDeltaRepository,
        UserIdentityService userIdentityService,
        UserRepository userRepository,
        ObjectMapper objectMapper
    ) {
        this.gradeRuleSetRepository = gradeRuleSetRepository;
        this.gradeRuleVersionRepository = gradeRuleVersionRepository;
        this.overrideReasonCodeRepository = overrideReasonCodeRepository;
        this.gradeLedgerEntryRepository = gradeLedgerEntryRepository;
        this.gradeRecalculationRepository = gradeRecalculationRepository;
        this.gradeRecalculationDeltaRepository = gradeRecalculationDeltaRepository;
        this.userIdentityService = userIdentityService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public List<GradeLedgerEntryEntity> latestLedger() {
        return gradeLedgerEntryRepository.findTop200ByOrderByEnteredAtDesc();
    }

    public List<GradeLedgerEntryEntity> ledgerForStudentClass(Long studentId, Long classId) {
        return gradeLedgerEntryRepository.findByStudentIdAndClassIdOrderByEnteredAtDesc(studentId, classId);
    }

    public GradeImpact preview(GradeEntryRequest request) {
        CalculationContext context = resolveCalculationContext(request);
        GradeImpact impact = calculateImpact(context.ruleVersion(), request.rawScore(), request.maxScore());

        GradeLedgerEntryEntity previous = resolvePreviousEntry(request.previousEntryId(), request.studentId(), request.classId(), request.assessmentKey());
        return withDelta(impact, previous, request.rawScore());
    }

    @Transactional
    public GradeLedgerEntryEntity appendLedgerEntry(GradeEntryRequest request) {
        validateRequest(request);
        CalculationContext context = resolveCalculationContext(request);
        GradeImpact impact = calculateImpact(context.ruleVersion(), request.rawScore(), request.maxScore());

        GradeLedgerEntryEntity previous = resolvePreviousEntry(request.previousEntryId(), request.studentId(), request.classId(), request.assessmentKey());
        GradeImpact withDelta = withDelta(impact, previous, request.rawScore());

        GradeLedgerEntryEntity entity = new GradeLedgerEntryEntity();
        entity.setStudentId(request.studentId());
        entity.setClassId(request.classId());
        entity.setClassSessionId(request.classSessionId());
        entity.setEnrollmentId(request.enrollmentId());
        entity.setAssessmentKey(request.assessmentKey());
        entity.setRawScore(request.rawScore().setScale(3, RoundingMode.HALF_UP));
        entity.setMaxScore(request.maxScore().setScale(3, RoundingMode.HALF_UP));
        entity.setGradeLetter(withDelta.gradeLetter());
        entity.setCreditsEarned(withDelta.creditsEarned());
        entity.setGpaPoints(withDelta.gpaImpact());
        entity.setRulesetId(context.ruleSet().getId());
        entity.setRuleVersionId(context.ruleVersion().getId());
        entity.setOperationType(normalizeOperationType(request.operationType()));
        entity.setOperationReasonCode(StringUtils.hasText(request.reasonCode()) ? request.reasonCode().trim().toUpperCase(Locale.ROOT) : null);
        entity.setPreviousEntryId(previous == null ? null : previous.getId());
        entity.setDeltaScore(withDelta.deltaScore());
        entity.setDeltaCredits(withDelta.deltaCredits());
        entity.setDeltaGpaPoints(withDelta.deltaGpaImpact());
        entity.setEnteredBy(currentUserId());
        entity.setEnteredAt(LocalDateTime.now());
        entity.setAppendOnlyMarker(UUID.randomUUID().toString());
        return gradeLedgerEntryRepository.save(entity);
    }

    @Transactional
    public RecalculationResult recomputeStudentClass(Long studentId, Long classId, String reasonCode) {
        if (studentId == null || classId == null) {
            throw new IllegalArgumentException("studentId and classId are required");
        }
        if (!StringUtils.hasText(reasonCode)) {
            throw new IllegalArgumentException("reasonCode is required");
        }

        CalculationContext context = resolveCalculationContext();
        List<GradeLedgerEntryEntity> ledger = gradeLedgerEntryRepository.findByStudentIdAndClassIdOrderByEnteredAtDesc(studentId, classId);
        if (ledger.isEmpty()) {
            throw new IllegalArgumentException("No ledger entries found for student/class");
        }

        Map<String, GradeLedgerEntryEntity> latestByAssessment = new LinkedHashMap<>();
        for (GradeLedgerEntryEntity entry : ledger) {
            latestByAssessment.putIfAbsent(entry.getAssessmentKey(), entry);
        }

        GradeRecalculationEntity recalculation = new GradeRecalculationEntity();
        recalculation.setStudentId(studentId);
        recalculation.setClassId(classId);
        recalculation.setTriggeredBy(currentUserId());
        recalculation.setTriggeredAt(LocalDateTime.now());
        recalculation.setScopeJson(toJson(Map.of("studentId", studentId, "classId", classId, "reasonCode", reasonCode)));
        recalculation.setDeterministicHash(deterministicHash(context.ruleVersion().getId(), latestByAssessment.values().stream().toList()));
        recalculation.setStatus("COMPLETED");
        GradeRecalculationEntity savedRecalc = gradeRecalculationRepository.save(recalculation);

        List<RecalculationDelta> deltas = new ArrayList<>();
        for (GradeLedgerEntryEntity entry : latestByAssessment.values()) {
            GradeImpact recomputed = calculateImpact(context.ruleVersion(), entry.getRawScore(), entry.getMaxScore());
            Map<String, Object> previous = new LinkedHashMap<>();
            previous.put("gradeLetter", entry.getGradeLetter());
            previous.put("creditsEarned", entry.getCreditsEarned());
            previous.put("gpaPoints", entry.getGpaPoints());
            previous.put("ruleVersionId", entry.getRuleVersionId());

            Map<String, Object> next = new LinkedHashMap<>();
            next.put("gradeLetter", recomputed.gradeLetter());
            next.put("creditsEarned", recomputed.creditsEarned());
            next.put("gpaPoints", recomputed.gpaImpact());
            next.put("ruleVersionId", context.ruleVersion().getId());
            Map<String, Object> delta = Map.of(
                "gradeChanged", !String.valueOf(entry.getGradeLetter()).equals(recomputed.gradeLetter()),
                "deltaCredits", recomputed.creditsEarned().subtract(entry.getCreditsEarned()).setScale(3, RoundingMode.HALF_UP),
                "deltaGpaPoints", recomputed.gpaImpact().subtract(entry.getGpaPoints()).setScale(3, RoundingMode.HALF_UP)
            );

            GradeRecalculationDeltaEntity deltaEntity = new GradeRecalculationDeltaEntity();
            deltaEntity.setRecalculationId(savedRecalc.getId());
            deltaEntity.setGradeLedgerEntryId(entry.getId());
            deltaEntity.setPreviousResultJson(toJson(previous));
            deltaEntity.setNewResultJson(toJson(next));
            deltaEntity.setDeltaJson(toJson(delta));
            deltaEntity.setCreatedAt(LocalDateTime.now());
            gradeRecalculationDeltaRepository.save(deltaEntity);

            deltas.add(new RecalculationDelta(entry.getId(), previous, next, delta));
        }

        return new RecalculationResult(savedRecalc.getId(), savedRecalc.getDeterministicHash(), deltas);
    }

    private void validateRequest(GradeEntryRequest request) {
        if (request.studentId() == null || request.classId() == null || !StringUtils.hasText(request.assessmentKey())) {
            throw new IllegalArgumentException("studentId, classId, assessmentKey are required");
        }
        if (request.rawScore() == null || request.maxScore() == null || request.maxScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("rawScore/maxScore must be valid and maxScore > 0");
        }
        if (request.rawScore().compareTo(BigDecimal.ZERO) < 0 || request.rawScore().compareTo(request.maxScore()) > 0) {
            throw new IllegalArgumentException("rawScore must be between 0 and maxScore");
        }
        String operation = normalizeOperationType(request.operationType());
        if (!OPERATION_TYPES.contains(operation)) {
            throw new IllegalArgumentException("Unsupported operation type");
        }
        if ("MANUAL_OVERRIDE".equals(operation)) {
            if (!StringUtils.hasText(request.reasonCode())) {
                throw new IllegalArgumentException("Immutable reason code is required for manual override");
            }
            OverrideReasonCodeEntity reason = overrideReasonCodeRepository.findByReasonCode(request.reasonCode().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Unknown reason code"));
            if (!reason.isImmutable()) {
                throw new IllegalArgumentException("Reason code must be immutable");
            }
        }
        if (("EDIT".equals(operation) || "MANUAL_OVERRIDE".equals(operation)) && request.previousEntryId() == null) {
            throw new IllegalArgumentException("previousEntryId is required for edit/manual override");
        }
    }

    private CalculationContext resolveCalculationContext(GradeEntryRequest request) {
        return resolveCalculationContext();
    }

    private CalculationContext resolveCalculationContext() {
        GradeRuleSetEntity ruleset = gradeRuleSetRepository.findActiveAt(LocalDateTime.now()).stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No active grade ruleset configured"));
        GradeRuleVersionEntity version = gradeRuleVersionRepository.findTopByRulesetIdOrderByVersionNoDesc(ruleset.getId())
            .orElseThrow(() -> new IllegalStateException("No grade rule version configured for active ruleset"));
        return new CalculationContext(ruleset, version);
    }

    private GradeLedgerEntryEntity resolvePreviousEntry(Long previousEntryId, Long studentId, Long classId, String assessmentKey) {
        if (previousEntryId == null) {
            return null;
        }
        GradeLedgerEntryEntity entry = gradeLedgerEntryRepository.findById(previousEntryId)
            .orElseThrow(() -> new IllegalArgumentException("Previous ledger entry not found"));
        if (!entry.getStudentId().equals(studentId)
            || !entry.getClassId().equals(classId)
            || !entry.getAssessmentKey().equals(assessmentKey)) {
            throw new IllegalArgumentException("Previous entry must match student/class/assessment");
        }
        return entry;
    }

    private GradeImpact calculateImpact(GradeRuleVersionEntity version, BigDecimal rawScore, BigDecimal maxScore) {
        Map<String, Object> json;
        try {
            json = objectMapper.readValue(version.getRuleJson(), new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid grade rule JSON", ex);
        }

        BigDecimal percent = rawScore
            .multiply(BigDecimal.valueOf(100))
            .divide(maxScore, 6, RoundingMode.HALF_UP);

        Map<String, Object> creditPolicy = asMap(json.get("creditPolicy"));
        BigDecimal baseCredits = asDecimal(creditPolicy.getOrDefault("baseCredits", BigDecimal.valueOf(3)));
        BigDecimal passingPercent = asDecimal(creditPolicy.getOrDefault("passingPercent", BigDecimal.valueOf(60)));
        boolean partialCreditEnabled = Boolean.TRUE.equals(creditPolicy.get("partialCreditEnabled"));

        List<Map<String, Object>> gradingScale = asMapList(json.get("gradingScale"));
        gradingScale.sort(Comparator.comparing((Map<String, Object> m) -> asDecimal(m.get("minPercent"))).reversed());

        String gradeLetter = "F";
        BigDecimal gpaPoint = BigDecimal.ZERO;
        for (Map<String, Object> row : gradingScale) {
            BigDecimal minPercent = asDecimal(row.get("minPercent"));
            if (percent.compareTo(minPercent) >= 0) {
                gradeLetter = String.valueOf(row.getOrDefault("grade", "F"));
                gpaPoint = asDecimal(row.getOrDefault("gpaPoints", BigDecimal.ZERO));
                break;
            }
        }

        BigDecimal creditsEarned;
        if (percent.compareTo(passingPercent) >= 0) {
            creditsEarned = baseCredits;
        } else if (partialCreditEnabled) {
            BigDecimal ratio = percent.divide(passingPercent, 4, RoundingMode.HALF_UP);
            creditsEarned = baseCredits.multiply(ratio).setScale(3, RoundingMode.HALF_UP);
        } else {
            creditsEarned = BigDecimal.ZERO;
        }

        BigDecimal gpaImpact = gpaPoint.multiply(creditsEarned).setScale(3, RoundingMode.HALF_UP);
        return new GradeImpact(
            percent.setScale(3, RoundingMode.HALF_UP),
            gradeLetter,
            creditsEarned.setScale(3, RoundingMode.HALF_UP),
            gpaImpact,
            null,
            null,
            null
        );
    }

    private GradeImpact withDelta(GradeImpact current, GradeLedgerEntryEntity previous, BigDecimal rawScore) {
        if (previous == null) {
            return current;
        }
        return new GradeImpact(
            current.percent(),
            current.gradeLetter(),
            current.creditsEarned(),
            current.gpaImpact(),
            rawScore.subtract(previous.getRawScore()).setScale(3, RoundingMode.HALF_UP),
            current.creditsEarned().subtract(previous.getCreditsEarned()).setScale(3, RoundingMode.HALF_UP),
            current.gpaImpact().subtract(previous.getGpaPoints()).setScale(3, RoundingMode.HALF_UP)
        );
    }

    private Long currentUserId() {
        return userIdentityService.resolveCurrentUserId()
            .orElseThrow(() -> new IllegalStateException("Authenticated user context is required but not available"));
    }

    private String deterministicHash(Long ruleVersionId, List<GradeLedgerEntryEntity> entries) {
        try {
            StringBuilder source = new StringBuilder("rv=").append(ruleVersionId);
            for (GradeLedgerEntryEntity entry : entries) {
                source.append('|')
                    .append(entry.getId()).append(':')
                    .append(entry.getAssessmentKey()).append(':')
                    .append(entry.getRawScore()).append('/').append(entry.getMaxScore());
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute recalculation hash", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize recalculation payload", ex);
        }
    }

    private static String normalizeOperationType(String operationType) {
        if (!StringUtils.hasText(operationType)) {
            return "ENTRY";
        }
        return operationType.trim().toUpperCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asMapList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private record CalculationContext(GradeRuleSetEntity ruleSet, GradeRuleVersionEntity ruleVersion) {
    }

    public record GradeEntryRequest(
        Long studentId,
        Long classId,
        Long classSessionId,
        Long enrollmentId,
        String assessmentKey,
        BigDecimal rawScore,
        BigDecimal maxScore,
        String operationType,
        String reasonCode,
        Long previousEntryId
    ) {
    }

    public record GradeImpact(
        BigDecimal percent,
        String gradeLetter,
        BigDecimal creditsEarned,
        BigDecimal gpaImpact,
        BigDecimal deltaScore,
        BigDecimal deltaCredits,
        BigDecimal deltaGpaImpact
    ) {
    }

    public record RecalculationDelta(Long gradeLedgerEntryId, Map<String, Object> previousResult, Map<String, Object> newResult, Map<String, Object> delta) {
    }

    public record RecalculationResult(Long recalculationId, String deterministicHash, List<RecalculationDelta> deltas) {
    }
}
