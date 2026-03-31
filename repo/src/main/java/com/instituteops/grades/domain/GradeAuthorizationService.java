package com.instituteops.grades.domain;

import com.instituteops.security.repo.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class GradeAuthorizationService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public GradeAuthorizationService(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void assertCanManageGradeEntry(Authentication authentication, Long studentId, Long classId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        if (hasAnyRole(authentication, "ROLE_SYSTEM_ADMIN", "ROLE_REGISTRAR_FINANCE_CLERK")) {
            return;
        }
        if (hasAnyRole(authentication, "ROLE_INSTRUCTOR")) {
            Long instructorUserId = userRepository.findIdByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Instructor account is not linked to an active user"));
            if (instructorCanManageGradeTarget(instructorUserId, studentId, classId)) {
                return;
            }
            throw new AccessDeniedException("Instructor can only manage grades for assigned students/classes");
        }
        throw new AccessDeniedException("Role is not permitted to manage grades");
    }

    private boolean instructorCanManageGradeTarget(Long instructorUserId, Long studentId, Long classId) {
        if (instructorUserId == null || studentId == null || classId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM enrollments e
                JOIN classes c ON c.id = e.class_id
                WHERE e.student_id = ?
                  AND e.class_id = ?
                  AND e.deleted_at IS NULL
                  AND e.enrollment_status IN ('ENROLLED', 'WAITLISTED', 'COMPLETED')
                  AND c.instructor_user_id = ?
            """,
            Integer.class,
            studentId,
            classId,
            instructorUserId
        );
        return count != null && count > 0;
    }

    private static boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null) {
            return false;
        }
        java.util.Set<String> granted = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toSet());
        for (String role : roles) {
            if (granted.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
