-- Preserve immutable grade ledger entries when a student is permanently purged.
-- Drop the FK constraint so student deletion does not cascade to ledger rows.
-- Ledger rows retain student_id as a historical reference (orphan-safe).
ALTER TABLE grade_ledger_entries DROP FOREIGN KEY fk_grade_student;
