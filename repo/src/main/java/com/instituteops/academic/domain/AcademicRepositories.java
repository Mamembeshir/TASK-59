package com.instituteops.academic.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProgramRepository extends JpaRepository<ProgramEntity, Long> {
}

interface CourseClassRepository extends JpaRepository<CourseClassEntity, Long> {
}

interface ClassSessionRepository extends JpaRepository<ClassSessionEntity, Long> {
}

interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {
}

interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, Long> {
}

interface InstructorCommentRepository extends JpaRepository<InstructorCommentEntity, Long> {
}

interface HomeworkAttachmentRepository extends JpaRepository<HomeworkAttachmentEntity, Long> {
}
