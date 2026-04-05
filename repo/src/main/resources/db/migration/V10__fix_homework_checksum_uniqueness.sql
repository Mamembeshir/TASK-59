-- Fix: change homework checksum uniqueness from global to per-student.
-- Global uniqueness blocks legitimate duplicate submissions from different students.
ALTER TABLE homework_attachments DROP INDEX uq_homework_checksum;
ALTER TABLE homework_attachments ADD UNIQUE KEY uq_homework_student_checksum (student_id, sha256_checksum);
