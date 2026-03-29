package com.instituteops.student.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, Long> {

    List<StudentProfileEntity> findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();

    List<StudentProfileEntity> findByDeletedAtIsNullAndFirstNameContainingIgnoreCaseOrDeletedAtIsNullAndLastNameContainingIgnoreCase(
        String firstName,
        String lastName
    );
}

interface EnrollmentRecordRepository extends JpaRepository<EnrollmentRecordEntity, Long> {

    List<EnrollmentRecordEntity> findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(Long studentId);

    Optional<EnrollmentRecordEntity> findByIdAndStudentId(Long id, Long studentId);
}

interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, Long> {

    List<PaymentRecordEntity> findByStudentIdOrderByRecordedAtDesc(Long studentId);
}

interface StudentAttendanceRepository extends JpaRepository<StudentAttendanceEntity, Long> {

    List<StudentAttendanceEntity> findByEnrollmentIdInOrderByCreatedAtDesc(Collection<Long> enrollmentIds);
}

interface InstructorCommentRecordRepository extends JpaRepository<InstructorCommentRecordEntity, Long> {

    List<InstructorCommentRecordEntity> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}

interface HomeworkAttachmentRecordRepository extends JpaRepository<HomeworkAttachmentRecordEntity, Long> {

    List<HomeworkAttachmentRecordEntity> findByStudentIdOrderByUploadedAtDesc(Long studentId);

    Optional<HomeworkAttachmentRecordEntity> findByIdAndStudentId(Long id, Long studentId);
}

interface CourseClassRefRepository extends JpaRepository<CourseClassRefEntity, Long> {
}

interface ClassSessionRefRepository extends JpaRepository<ClassSessionRefEntity, Long> {

    List<ClassSessionRefEntity> findByClassIdOrderBySessionDateAsc(Long classId);

    Optional<ClassSessionRefEntity> findByIdAndClassId(Long id, Long classId);
}
