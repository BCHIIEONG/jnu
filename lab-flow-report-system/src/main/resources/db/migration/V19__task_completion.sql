CREATE TABLE IF NOT EXISTS task_completion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    confirmed_by BIGINT NULL,
    CONSTRAINT fk_task_completion_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT fk_task_completion_student FOREIGN KEY (student_id) REFERENCES sys_user (id),
    CONSTRAINT fk_task_completion_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES sys_user (id),
    CONSTRAINT uk_task_completion_task_student UNIQUE (task_id, student_id)
);

CREATE INDEX idx_task_completion_status ON task_completion (status);
