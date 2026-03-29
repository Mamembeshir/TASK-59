CREATE INDEX idx_enrollments_student_timeline
    ON enrollments (student_id, enrolled_at);

CREATE INDEX idx_attendance_enrollment_timeline
    ON attendance_records (enrollment_id, created_at);

CREATE INDEX idx_comments_student_timeline
    ON instructor_comments (student_id, created_at);

CREATE INDEX idx_homework_student_timeline
    ON homework_attachments (student_id, uploaded_at);

INSERT INTO programs (program_code, program_name, description, credits_required, active)
SELECT 'CUL-FOUND', 'Culinary Foundations', 'Foundational offline culinary program', 24.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM programs WHERE program_code = 'CUL-FOUND');

INSERT INTO classes (class_code, class_name, program_id, instructor_user_id, total_sessions, active)
SELECT 'CUL-101-A', 'Culinary Foundations - Cohort A', p.id, u.id, 12, TRUE
FROM programs p
JOIN users u ON u.username = 'instructor'
WHERE p.program_code = 'CUL-FOUND'
  AND NOT EXISTS (SELECT 1 FROM classes WHERE class_code = 'CUL-101-A');

INSERT INTO class_sessions (class_id, session_no, session_date, start_time, end_time, topic)
SELECT c.id, 1, CURRENT_DATE(), '09:00:00', '11:00:00', 'Orientation and kitchen safety'
FROM classes c
WHERE c.class_code = 'CUL-101-A'
  AND NOT EXISTS (SELECT 1 FROM class_sessions cs WHERE cs.class_id = c.id AND cs.session_no = 1);

INSERT INTO students (
    student_no,
    first_name,
    last_name,
    preferred_name,
    date_of_birth,
    status,
    contact_email_encrypted,
    contact_phone_encrypted,
    contact_address_encrypted,
    masked_email,
    masked_phone,
    emergency_contact_encrypted
)
SELECT
    'STU-0001',
    'Avery',
    'Nguyen',
    'Avery',
    '2001-04-18',
    'ACTIVE',
    NULL,
    NULL,
    NULL,
    'av***@local.test',
    '***-***-2301',
    NULL
WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_no = 'STU-0001');

INSERT INTO enrollments (student_id, class_id, enrollment_status, enrolled_at, created_by)
SELECT s.id, c.id, 'ENROLLED', NOW(6), u.id
FROM students s
JOIN classes c ON c.class_code = 'CUL-101-A'
JOIN users u ON u.username = 'registrar'
WHERE s.student_no = 'STU-0001'
  AND NOT EXISTS (SELECT 1 FROM enrollments e WHERE e.student_id = s.id AND e.class_id = c.id AND e.deleted_at IS NULL);
