package com.instituteops.grades.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface GradeRuleSetRepository extends JpaRepository<GradeRuleSetEntity, Long> {
}

interface GradeRuleVersionRepository extends JpaRepository<GradeRuleVersionEntity, Long> {
}

interface OverrideReasonCodeRepository extends JpaRepository<OverrideReasonCodeEntity, Long> {
}

interface GradeLedgerEntryRepository extends JpaRepository<GradeLedgerEntryEntity, Long> {
}

interface GradeRecalculationRepository extends JpaRepository<GradeRecalculationEntity, Long> {
}

interface GradeRecalculationDeltaRepository extends JpaRepository<GradeRecalculationDeltaEntity, Long> {
}
