-- Extra indexes for common queries (teacher opens a submission -> best match lookup)
CREATE INDEX idx_plag_best_run_student ON plag_submission_best_match (run_id, student_id);
CREATE INDEX idx_plag_best_task_submission ON plag_submission_best_match (task_id, submission_id);

