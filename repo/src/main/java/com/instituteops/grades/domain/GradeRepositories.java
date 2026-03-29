package com.instituteops.grades.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GradeRuleSetRepository extends JpaRepository<GradeRuleSetEntity, Long> {

    @Query("""
        select r from GradeRuleSetEntity r
        where r.active = true
          and r.effectiveFrom <= :now
          and (r.effectiveTo is null or r.effectiveTo >= :now)
        order by r.effectiveFrom desc
        """)
    List<GradeRuleSetEntity> findActiveAt(@Param("now") LocalDateTime now);
}

interface GradeRuleVersionRepository extends JpaRepository<GradeRuleVersionEntity, Long> {

    Optional<GradeRuleVersionEntity> findTopByRulesetIdOrderByVersionNoDesc(Long rulesetId);
}

interface OverrideReasonCodeRepository extends JpaRepository<OverrideReasonCodeEntity, Long> {

    Optional<OverrideReasonCodeEntity> findByReasonCode(String reasonCode);
}

interface GradeLedgerEntryRepository extends JpaRepository<GradeLedgerEntryEntity, Long> {

    List<GradeLedgerEntryEntity> findTop200ByOrderByEnteredAtDesc();

    List<GradeLedgerEntryEntity> findByStudentIdAndClassIdOrderByEnteredAtDesc(Long studentId, Long classId);
}

interface GradeRecalculationRepository extends JpaRepository<GradeRecalculationEntity, Long> {
}

interface GradeRecalculationDeltaRepository extends JpaRepository<GradeRecalculationDeltaEntity, Long> {

    List<GradeRecalculationDeltaEntity> findByRecalculationIdOrderByCreatedAtAsc(Long recalculationId);
}
