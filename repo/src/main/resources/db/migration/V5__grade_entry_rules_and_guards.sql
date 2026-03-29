INSERT INTO grade_rule_sets (ruleset_code, name, description, active, effective_from, created_by)
SELECT
    'DEFAULT_2026',
    'Default Grading Ruleset 2026',
    'Deterministic baseline grading with credit and GPA impact',
    TRUE,
    NOW(6),
    u.id
FROM users u
WHERE u.username = 'sysadmin'
  AND NOT EXISTS (SELECT 1 FROM grade_rule_sets WHERE ruleset_code = 'DEFAULT_2026');

INSERT INTO grade_rule_versions (ruleset_id, version_no, rule_json, checksum_sha256, created_by)
SELECT
    rs.id,
    1,
    JSON_OBJECT(
        'creditPolicy', JSON_OBJECT('baseCredits', 3.0, 'passingPercent', 60.0, 'partialCreditEnabled', TRUE),
        'gradingScale', JSON_ARRAY(
            JSON_OBJECT('minPercent', 97.0, 'grade', 'A+', 'gpaPoints', 4.0),
            JSON_OBJECT('minPercent', 93.0, 'grade', 'A',  'gpaPoints', 4.0),
            JSON_OBJECT('minPercent', 90.0, 'grade', 'A-', 'gpaPoints', 3.7),
            JSON_OBJECT('minPercent', 87.0, 'grade', 'B+', 'gpaPoints', 3.3),
            JSON_OBJECT('minPercent', 83.0, 'grade', 'B',  'gpaPoints', 3.0),
            JSON_OBJECT('minPercent', 80.0, 'grade', 'B-', 'gpaPoints', 2.7),
            JSON_OBJECT('minPercent', 77.0, 'grade', 'C+', 'gpaPoints', 2.3),
            JSON_OBJECT('minPercent', 73.0, 'grade', 'C',  'gpaPoints', 2.0),
            JSON_OBJECT('minPercent', 70.0, 'grade', 'C-', 'gpaPoints', 1.7),
            JSON_OBJECT('minPercent', 67.0, 'grade', 'D+', 'gpaPoints', 1.3),
            JSON_OBJECT('minPercent', 63.0, 'grade', 'D',  'gpaPoints', 1.0),
            JSON_OBJECT('minPercent', 60.0, 'grade', 'D-', 'gpaPoints', 0.7),
            JSON_OBJECT('minPercent', 0.0,  'grade', 'F',  'gpaPoints', 0.0)
        )
    ),
    SHA2('DEFAULT_2026_V1', 256),
    u.id
FROM grade_rule_sets rs
JOIN users u ON u.username = 'sysadmin'
WHERE rs.ruleset_code = 'DEFAULT_2026'
  AND NOT EXISTS (
      SELECT 1 FROM grade_rule_versions rv
      WHERE rv.ruleset_id = rs.id AND rv.version_no = 1
  );

UPDATE override_reason_codes
SET immutable = TRUE
WHERE reason_code IN ('POLICY_EXCEPTION', 'DATA_CORRECTION', 'APPEAL_DECISION');

CREATE INDEX idx_grade_ledger_assessment
    ON grade_ledger_entries (student_id, class_id, assessment_key, entered_at);

CREATE INDEX idx_grade_ledger_previous
    ON grade_ledger_entries (previous_entry_id);
