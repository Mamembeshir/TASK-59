package com.instituteops.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.instituteops.student.model.StudentProfileEntity;
import com.instituteops.student.model.StudentProfileLookupRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditFixVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentProfileLookupRepository studentProfileLookupRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long student1Id;
    private Long student2Id;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("ALTER TABLE classes ADD COLUMN IF NOT EXISTS instructor_user_id BIGINT");
        jdbcTemplate.update("DELETE FROM enrollments");
        studentProfileLookupRepository.deleteAll();

        StudentProfileEntity student1 = new StudentProfileEntity();
        student1.setStudentNo("audit-s1");
        student1.setFirstName("Alice");
        student1.setLastName("Assigned");
        student1.setDateOfBirth(LocalDate.parse("2010-01-01"));
        student1.setStatus("ACTIVE");
        studentProfileLookupRepository.save(student1);
        student1Id = student1.getId();

        StudentProfileEntity student2 = new StudentProfileEntity();
        student2.setStudentNo("audit-s2");
        student2.setFirstName("Bob");
        student2.setLastName("Unassigned");
        student2.setDateOfBirth(LocalDate.parse("2010-02-02"));
        student2.setStatus("ACTIVE");
        studentProfileLookupRepository.save(student2);
        student2Id = student2.getId();

        seedTestUsers();
        seedInstructorWithAssignment(student1Id);
    }

    // ===== Finding 1: Instructor student list isolation =====

    @Test
    @WithMockUser(username = "instructor", roles = "INSTRUCTOR")
    void instructorSearch_returnsOnlyAssignedStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].firstName").value("Alice"));
    }

    @Test
    @WithMockUser(username = "instructor", roles = "INSTRUCTOR")
    void instructorSearch_doesNotReturnUnassignedStudent() throws Exception {
        mockMvc.perform(get("/api/students").param("q", "Bob"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "instructor", roles = "INSTRUCTOR")
    void instructorSearch_findsAssignedStudentByName() throws Exception {
        mockMvc.perform(get("/api/students").param("q", "Alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].firstName").value("Alice"));
    }

    @Test
    @WithMockUser(username = "sysadmin", roles = "SYSTEM_ADMIN")
    void adminSearch_returnsAllStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR_FINANCE_CLERK")
    void registrarSearch_returnsAllStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    // ===== Finding 4: Trace ID consistency =====

    @Test
    @WithMockUser(username = "sysadmin", roles = "SYSTEM_ADMIN")
    void errorResponse_containsTraceId() throws Exception {
        mockMvc.perform(get("/api/students/99999/timeline"))
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    // ===== Finding 5: Governance data-access audit logging =====

    @Test
    @WithMockUser(username = "sysadmin", roles = "SYSTEM_ADMIN")
    void governanceExport_isAuditLogged() throws Exception {
        Long countBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM data_access_logs WHERE access_type = 'GOVERNANCE_EXPORT'",
            Long.class
        );

        mockMvc.perform(get("/api/governance/students/export"))
            .andExpect(status().isOk());

        Long countAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM data_access_logs WHERE access_type = 'GOVERNANCE_EXPORT'",
            Long.class
        );
        org.assertj.core.api.Assertions.assertThat(countAfter).isGreaterThan(countBefore);
    }

    @Test
    @WithMockUser(username = "sysadmin", roles = "SYSTEM_ADMIN")
    void governanceHistory_isAuditLogged() throws Exception {
        Long countBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM data_access_logs WHERE access_type = 'GOVERNANCE_HISTORY_READ'",
            Long.class
        );

        mockMvc.perform(get("/api/governance/students/" + student1Id + "/history"))
            .andExpect(status().isOk());

        Long countAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM data_access_logs WHERE access_type = 'GOVERNANCE_HISTORY_READ'",
            Long.class
        );
        org.assertj.core.api.Assertions.assertThat(countAfter).isGreaterThan(countBefore);
    }

    // ===== Finding 6: Homework checksum context-safe uniqueness =====

    @Test
    @WithMockUser(username = "sysadmin", roles = "SYSTEM_ADMIN")
    void twoStudents_canSubmitIdenticalHomeworkChecksum() {
        String checksum = "a".repeat(64);
        jdbcTemplate.update(
            "INSERT INTO homework_attachments (student_id, original_file_name, stored_file_name, mime_type, file_size_bytes, sha256_checksum, upload_path, uploaded_by, uploaded_at) " +
                "VALUES (?, 'test.pdf', 'stored1.pdf', 'application/pdf', 1024, ?, '/tmp/stored1.pdf', 1, CURRENT_TIMESTAMP())",
            student1Id, checksum
        );

        // Second student submitting the same file should succeed
        jdbcTemplate.update(
            "INSERT INTO homework_attachments (student_id, original_file_name, stored_file_name, mime_type, file_size_bytes, sha256_checksum, upload_path, uploaded_by, uploaded_at) " +
                "VALUES (?, 'test.pdf', 'stored2.pdf', 'application/pdf', 1024, ?, '/tmp/stored2.pdf', 1, CURRENT_TIMESTAMP())",
            student2Id, checksum
        );

        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM homework_attachments WHERE sha256_checksum = ?",
            Long.class, checksum
        );
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(2L);
    }

    @Test
    @WithMockUser(username = "sysadmin", roles = "SYSTEM_ADMIN")
    void sameStudent_cannotSubmitDuplicateChecksum() {
        String checksum = "b".repeat(64);
        jdbcTemplate.update(
            "INSERT INTO homework_attachments (student_id, original_file_name, stored_file_name, mime_type, file_size_bytes, sha256_checksum, upload_path, uploaded_by, uploaded_at) " +
                "VALUES (?, 'test.pdf', 'stored1.pdf', 'application/pdf', 1024, ?, '/tmp/stored1.pdf', 1, CURRENT_TIMESTAMP())",
            student1Id, checksum
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO homework_attachments (student_id, original_file_name, stored_file_name, mime_type, file_size_bytes, sha256_checksum, upload_path, uploaded_by, uploaded_at) " +
                    "VALUES (?, 'test2.pdf', 'stored2.pdf', 'application/pdf', 1024, ?, '/tmp/stored2.pdf', 1, CURRENT_TIMESTAMP())",
                student1Id, checksum
            )
        ).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ===== Helpers =====

    private void seedTestUsers() {
        for (String[] user : new String[][]{
            {"sysadmin", "System Admin"},
            {"registrar", "Registrar Clerk"},
            {"instructor", "Instructor"},
            {"approver", "Procurement Approver"},
            {"student1", "Student User"}
        }) {
            jdbcTemplate.update(
                "MERGE INTO users (username, password_hash, display_name, active) KEY(username) VALUES (?, ?, ?, ?)",
                user[0], "hashed", user[1], true
            );
        }
    }

    private void seedInstructorWithAssignment(Long studentId) {
        jdbcTemplate.update(
            "MERGE INTO users (username, password_hash, display_name, active) KEY(username) VALUES (?, ?, ?, ?)",
            "instructor", "hashed", "Instructor", true
        );
        Long instructorUserId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "instructor");

        jdbcTemplate.update(
            "MERGE INTO classes (class_code, class_name, instructor_user_id) KEY(class_code) VALUES (?, ?, ?)",
            "AUDIT-TST-CLASS", "Audit Test Class", instructorUserId
        );
        Long classId = jdbcTemplate.queryForObject("SELECT id FROM classes WHERE class_code = ?", Long.class, "AUDIT-TST-CLASS");
        jdbcTemplate.update("UPDATE classes SET instructor_user_id = ? WHERE id = ?", instructorUserId, classId);

        jdbcTemplate.update(
            "INSERT INTO enrollments (student_id, class_id, enrollment_status, enrolled_at, deleted_at) VALUES (?, ?, 'ENROLLED', CURRENT_TIMESTAMP(), NULL)",
            studentId, classId
        );
    }
}
