CREATE TABLE IF NOT EXISTS task_progress_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_progress_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT fk_task_progress_student FOREIGN KEY (student_id) REFERENCES sys_user (id),
    CONSTRAINT uk_task_progress_task_student_step UNIQUE (task_id, student_id, step_no)
);

CREATE INDEX idx_task_progress_task_student ON task_progress_log (task_id, student_id);
CREATE INDEX idx_task_progress_created_at ON task_progress_log (created_at);
