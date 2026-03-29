package com.instituteops.student.model;

import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentModuleService {

    private static final Set<String> STUDENT_STATUSES = Set.of("ACTIVE", "INACTIVE", "GRADUATED", "SUSPENDED", "WITHDRAWN");
    private static final Set<String> ENROLLMENT_STATUSES = Set.of("ENROLLED", "WAITLISTED", "WITHDRAWN", "COMPLETED", "FAILED");
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "CHECK", "OTHER_OFFLINE");
    private static final Set<String> ATTENDANCE_STATUSES = Set.of("PRESENT", "ABSENT", "LATE", "EXCUSED");
    private static final Set<String> COMMENT_VISIBILITY = Set.of("INTERNAL", "STUDENT_VISIBLE");
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private final StudentProfileRepository studentProfileRepository;
    private final EnrollmentRecordRepository enrollmentRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final InstructorCommentRecordRepository instructorCommentRecordRepository;
    private final HomeworkAttachmentRecordRepository homeworkAttachmentRecordRepository;
    private final CourseClassRefRepository courseClassRefRepository;
    private final ClassSessionRefRepository classSessionRefRepository;
    private final UserRepository userRepository;
    private final UserIdentityService userIdentityService;
    private final Path uploadRoot;

    public StudentModuleService(
        StudentProfileRepository studentProfileRepository,
        EnrollmentRecordRepository enrollmentRecordRepository,
        PaymentRecordRepository paymentRecordRepository,
        StudentAttendanceRepository studentAttendanceRepository,
        InstructorCommentRecordRepository instructorCommentRecordRepository,
        HomeworkAttachmentRecordRepository homeworkAttachmentRecordRepository,
        CourseClassRefRepository courseClassRefRepository,
        ClassSessionRefRepository classSessionRefRepository,
        UserRepository userRepository,
        UserIdentityService userIdentityService,
        @Value("${app.upload.base-path}") String uploadBasePath
    ) {
        this.studentProfileRepository = studentProfileRepository;
        this.enrollmentRecordRepository = enrollmentRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.studentAttendanceRepository = studentAttendanceRepository;
        this.instructorCommentRecordRepository = instructorCommentRecordRepository;
        this.homeworkAttachmentRecordRepository = homeworkAttachmentRecordRepository;
        this.courseClassRefRepository = courseClassRefRepository;
        this.classSessionRefRepository = classSessionRefRepository;
        this.userRepository = userRepository;
        this.userIdentityService = userIdentityService;
        this.uploadRoot = Path.of(uploadBasePath).toAbsolutePath().normalize();
    }

    @Transactional
    public StudentProfileEntity createStudent(StudentCreateRequest request) {
        StudentProfileEntity entity = new StudentProfileEntity();
        entity.setStudentNo(request.studentNo());
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setPreferredName(request.preferredName());
        entity.setDateOfBirth(request.dateOfBirth());
        entity.setStatus("ACTIVE");
        entity.setContactEmail(request.contactEmail());
        entity.setContactPhone(request.contactPhone());
        entity.setContactAddress(request.contactAddress());
        entity.setEmergencyContact(request.emergencyContact());
        entity.setMaskedEmail(maskEmail(request.contactEmail()));
        entity.setMaskedPhone(maskPhone(request.contactPhone()));
        return studentProfileRepository.save(entity);
    }

    public List<StudentProfileEntity> searchStudents(String query) {
        if (!StringUtils.hasText(query)) {
            return studentProfileRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();
        }
        return studentProfileRepository.findByDeletedAtIsNullAndFirstNameContainingIgnoreCaseOrDeletedAtIsNullAndLastNameContainingIgnoreCase(query, query);
    }

    @Transactional
    public StudentProfileEntity updateStatus(Long studentId, String newStatus) {
        String normalized = normalizeUpper(newStatus);
        if (!STUDENT_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported student status");
        }
        StudentProfileEntity student = findActiveStudent(studentId);
        student.setStatus(normalized);
        return studentProfileRepository.save(student);
    }

    @Transactional
    public void softDeleteStudent(Long studentId) {
        StudentProfileEntity student = findActiveStudent(studentId);
        student.setDeletedAt(LocalDateTime.now());
        student.setStatus("INACTIVE");
        studentProfileRepository.save(student);
    }

    @Transactional
    public void restoreStudent(Long studentId) {
        StudentProfileEntity student = studentProfileRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        student.setDeletedAt(null);
        if (!"GRADUATED".equals(student.getStatus())) {
            student.setStatus("ACTIVE");
        }
        studentProfileRepository.save(student);
    }

    @Transactional
    public EnrollmentRecordEntity enroll(Long studentId, EnrollmentCreateRequest request) {
        findActiveStudent(studentId);
        String status = normalizeUpper(request.enrollmentStatus());
        if (!ENROLLMENT_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unsupported enrollment status");
        }
        EnrollmentRecordEntity entity = new EnrollmentRecordEntity();
        entity.setStudentId(studentId);
        entity.setClassId(request.classId());
        entity.setEnrollmentStatus(status);
        entity.setEnrolledAt(LocalDateTime.now());
        entity.setCompletionDate(request.completionDate());
        return enrollmentRecordRepository.save(entity);
    }

    @Transactional
    public PaymentRecordEntity recordPayment(Long studentId, PaymentCreateRequest request) {
        findActiveStudent(studentId);
        String paymentMethod = normalizeUpper(request.paymentMethod());
        if (!PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Unsupported payment method");
        }
        PaymentRecordEntity entity = new PaymentRecordEntity();
        entity.setStudentId(studentId);
        entity.setEnrollmentId(request.enrollmentId());
        entity.setPaymentMethod(paymentMethod);
        entity.setAmount(request.amount());
        entity.setCurrencyCode(StringUtils.hasText(request.currencyCode()) ? request.currencyCode().toUpperCase(Locale.ROOT) : "USD");
        entity.setPaymentReference(request.paymentReference());
        entity.setNote(request.note());
        entity.setRecordedBy(currentUserId());
        entity.setRecordedAt(LocalDateTime.now());
        return paymentRecordRepository.save(entity);
    }

    @Transactional
    public StudentAttendanceEntity recordAttendance(Long studentId, AttendanceCreateRequest request) {
        findActiveStudent(studentId);
        EnrollmentRecordEntity enrollment = enrollmentRecordRepository.findByIdAndStudentId(request.enrollmentId(), studentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found for student"));
        String status = normalizeUpper(request.attendanceStatus());
        if (!ATTENDANCE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unsupported attendance status");
        }
        classSessionRefRepository.findByIdAndClassId(request.classSessionId(), enrollment.getClassId())
            .orElseThrow(() -> new IllegalArgumentException("Class session does not belong to enrollment class"));

        StudentAttendanceEntity entity = new StudentAttendanceEntity();
        entity.setEnrollmentId(enrollment.getId());
        entity.setClassSessionId(request.classSessionId());
        entity.setAttendanceStatus(status);
        entity.setNote(request.note());
        entity.setCreatedBy(currentUserId());
        entity.setCreatedAt(LocalDateTime.now());
        return studentAttendanceRepository.save(entity);
    }

    @Transactional
    public InstructorCommentRecordEntity recordComment(Long studentId, CommentCreateRequest request) {
        findActiveStudent(studentId);
        String visibility = normalizeUpper(request.visibility());
        if (!COMMENT_VISIBILITY.contains(visibility)) {
            throw new IllegalArgumentException("Unsupported comment visibility");
        }
        InstructorCommentRecordEntity entity = new InstructorCommentRecordEntity();
        entity.setStudentId(studentId);
        entity.setClassId(request.classId());
        entity.setClassSessionId(request.classSessionId());
        entity.setInstructorUserId(currentUserId());
        entity.setCommentText(request.commentText());
        entity.setVisibility(visibility);
        entity.setCreatedAt(LocalDateTime.now());
        return instructorCommentRecordRepository.save(entity);
    }

    @Transactional
    public HomeworkAttachmentRecordEntity uploadHomework(Long studentId, HomeworkUploadRequest request, MultipartFile file) {
        findActiveStudent(studentId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Homework file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds 10MB limit");
        }

        String mimeType = normalizeMimeType(file.getContentType());
        String originalName = StringUtils.cleanPath(Optional.ofNullable(file.getOriginalFilename()).orElse("upload"));
        String extension = extensionOf(originalName);
        validateFileType(mimeType, extension);

        String computedChecksum = sha256Hex(file);
        if (StringUtils.hasText(request.expectedChecksum()) && !computedChecksum.equalsIgnoreCase(request.expectedChecksum())) {
            throw new IllegalArgumentException("Checksum mismatch");
        }

        try {
            Path studentDir = uploadRoot.resolve("homework").resolve(String.valueOf(studentId));
            Files.createDirectories(studentDir);
            String storedName = UUID.randomUUID() + extension;
            Path target = studentDir.resolve(storedName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            HomeworkAttachmentRecordEntity entity = new HomeworkAttachmentRecordEntity();
            entity.setStudentId(studentId);
            entity.setClassId(request.classId());
            entity.setClassSessionId(request.classSessionId());
            entity.setOriginalFileName(originalName);
            entity.setStoredFileName(storedName);
            entity.setMimeType(mimeType);
            entity.setFileSizeBytes(file.getSize());
            entity.setSha256Checksum(computedChecksum);
            entity.setUploadPath(target.toString());
            entity.setUploadedBy(currentUserId());
            entity.setUploadedAt(LocalDateTime.now());
            return homeworkAttachmentRecordRepository.save(entity);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to persist homework attachment", ex);
        }
    }

    public StudentTimelineView timeline(Long studentId, boolean allowUnmask) {
        StudentProfileEntity student = findActiveStudent(studentId);
        List<EnrollmentRecordEntity> enrollments = enrollmentRecordRepository.findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(studentId);
        List<PaymentRecordEntity> payments = paymentRecordRepository.findByStudentIdOrderByRecordedAtDesc(studentId);
        List<InstructorCommentRecordEntity> comments = instructorCommentRecordRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        List<HomeworkAttachmentRecordEntity> attachments = homeworkAttachmentRecordRepository.findByStudentIdOrderByUploadedAtDesc(studentId);

        Map<Long, CourseClassRefEntity> classMap = new HashMap<>();
        for (CourseClassRefEntity classEntity : courseClassRefRepository.findAll()) {
            classMap.put(classEntity.getId(), classEntity);
        }

        List<Long> enrollmentIds = enrollments.stream().map(EnrollmentRecordEntity::getId).toList();
        List<StudentAttendanceEntity> attendance = enrollmentIds.isEmpty()
            ? List.of()
            : studentAttendanceRepository.findByEnrollmentIdInOrderByCreatedAtDesc(enrollmentIds);

        Map<Long, ClassSessionRefEntity> sessionMap = new HashMap<>();
        for (ClassSessionRefEntity session : classSessionRefRepository.findAll()) {
            sessionMap.put(session.getId(), session);
        }

        List<TimelineEvent> events = new ArrayList<>();
        for (EnrollmentRecordEntity enrollment : enrollments) {
            events.add(TimelineEvent.of(
                enrollment.getEnrolledAt(),
                "ENROLLMENT",
                classLabel(classMap, enrollment.getClassId()) + " - " + enrollment.getEnrollmentStatus()
            ));
        }
        for (PaymentRecordEntity payment : payments) {
            events.add(TimelineEvent.of(
                payment.getRecordedAt(),
                "PAYMENT",
                payment.getPaymentMethod() + " " + payment.getCurrencyCode() + " " + payment.getAmount()
            ));
        }
        for (StudentAttendanceEntity att : attendance) {
            ClassSessionRefEntity session = sessionMap.get(att.getClassSessionId());
            String detail = "Session " + (session == null ? att.getClassSessionId() : session.getSessionNo()) + " - " + att.getAttendanceStatus();
            events.add(TimelineEvent.of(att.getCreatedAt(), "ATTENDANCE", detail));
        }
        for (InstructorCommentRecordEntity comment : comments) {
            events.add(TimelineEvent.of(comment.getCreatedAt(), "COMMENT", comment.getVisibility() + " - " + comment.getCommentText()));
        }
        for (HomeworkAttachmentRecordEntity attachment : attachments) {
            events.add(TimelineEvent.of(
                attachment.getUploadedAt(),
                "HOMEWORK",
                attachment.getOriginalFileName() + " (" + attachment.getMimeType() + ")"
            ));
        }
        events.sort(Comparator.comparing(TimelineEvent::occurredAt).reversed());

        StudentContactView contact = allowUnmask
            ? new StudentContactView(student.getContactEmail(), student.getContactPhone(), student.getContactAddress(), student.getEmergencyContact())
            : new StudentContactView(student.getMaskedEmail(), student.getMaskedPhone(), maskAddress(student.getContactAddress()), maskEmergency(student.getEmergencyContact()));

        return new StudentTimelineView(student, contact, enrollments, payments, attendance, comments, attachments, events, classMap, sessionMap);
    }

    public List<CourseClassRefEntity> classes() {
        return courseClassRefRepository.findAll();
    }

    public List<ClassSessionRefEntity> sessionsForClass(Long classId) {
        return classSessionRefRepository.findByClassIdOrderBySessionDateAsc(classId);
    }

    public boolean canUnmask(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> "ROLE_SYSTEM_ADMIN".equals(role) || "ROLE_REGISTRAR_FINANCE_CLERK".equals(role));
    }

    private StudentProfileEntity findActiveStudent(Long studentId) {
        StudentProfileEntity student = studentProfileRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        if (student.getDeletedAt() != null) {
            throw new IllegalArgumentException("Student has been soft-deleted");
        }
        return student;
    }

    private Long currentUserId() {
        return userIdentityService.resolveCurrentUserId().orElseGet(() -> userRepository.findIdByUsername("sysadmin").orElse(1L));
    }

    private static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return null;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2 ? "**" : local.substring(0, 2) + "***";
        return masked + "@" + parts[1];
    }

    private static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String normalized = phone.replaceAll("\\D", "");
        if (normalized.length() <= 4) {
            return "****";
        }
        return "***-***-" + normalized.substring(normalized.length() - 4);
    }

    private static String maskAddress(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        return "Address masked";
    }

    private static String maskEmergency(String emergency) {
        if (!StringUtils.hasText(emergency)) {
            return null;
        }
        return "Emergency contact masked";
    }

    private static String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeMimeType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return fileName.substring(idx).toLowerCase(Locale.ROOT);
    }

    private static void validateFileType(String mimeType, String extension) {
        boolean pdf = "application/pdf".equals(mimeType) && ".pdf".equals(extension);
        boolean jpg = "image/jpeg".equals(mimeType) && (".jpg".equals(extension) || ".jpeg".equals(extension));
        if (!pdf && !jpg) {
            throw new IllegalArgumentException("Only PDF and JPG homework attachments are allowed");
        }
    }

    private static String sha256Hex(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new IllegalStateException("Unable to compute checksum", ex);
        }
    }

    private static String classLabel(Map<Long, CourseClassRefEntity> classMap, Long classId) {
        CourseClassRefEntity classRef = classMap.get(classId);
        if (classRef == null) {
            return "Class " + classId;
        }
        return classRef.getClassCode() + " - " + classRef.getClassName();
    }

    public record StudentCreateRequest(
        String studentNo,
        String firstName,
        String lastName,
        String preferredName,
        LocalDate dateOfBirth,
        String contactEmail,
        String contactPhone,
        String contactAddress,
        String emergencyContact
    ) {
    }

    public record EnrollmentCreateRequest(Long classId, String enrollmentStatus, LocalDate completionDate) {
    }

    public record PaymentCreateRequest(Long enrollmentId, String paymentMethod, BigDecimal amount, String currencyCode, String paymentReference, String note) {
    }

    public record AttendanceCreateRequest(Long enrollmentId, Long classSessionId, String attendanceStatus, String note) {
    }

    public record CommentCreateRequest(Long classId, Long classSessionId, String commentText, String visibility) {
    }

    public record HomeworkUploadRequest(Long classId, Long classSessionId, String expectedChecksum) {
    }

    public record TimelineEvent(LocalDateTime occurredAt, String type, String details) {
        private static TimelineEvent of(LocalDateTime when, String type, String details) {
            return new TimelineEvent(when, type, details);
        }
    }

    public record StudentContactView(String email, String phone, String address, String emergencyContact) {
    }

    public record StudentTimelineView(
        StudentProfileEntity student,
        StudentContactView contact,
        List<EnrollmentRecordEntity> enrollments,
        List<PaymentRecordEntity> payments,
        List<StudentAttendanceEntity> attendance,
        List<InstructorCommentRecordEntity> comments,
        List<HomeworkAttachmentRecordEntity> attachments,
        List<TimelineEvent> timeline,
        Map<Long, CourseClassRefEntity> classMap,
        Map<Long, ClassSessionRefEntity> sessionMap
    ) {
    }
}
