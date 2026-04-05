-- Allow grade_ledger_entries.student_id to be nullable so purge can preserve
-- immutable ledger history when a student is permanently removed.
ALTER TABLE grade_ledger_entries MODIFY student_id BIGINT NULL;
