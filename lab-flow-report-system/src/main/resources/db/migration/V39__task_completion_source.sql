ALTER TABLE task_completion
    ADD COLUMN completion_source VARCHAR(32) NULL;

UPDATE task_completion
SET completion_source = CASE
    WHEN status = 'CONFIRMED' AND requested_at IS NULL THEN 'TEACHER_DIRECT'
    ELSE 'STUDENT_REQUEST'
END
WHERE completion_source IS NULL;
