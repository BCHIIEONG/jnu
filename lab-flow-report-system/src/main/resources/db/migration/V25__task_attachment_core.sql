CREATE TABLE IF NOT EXISTS task_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_attachment_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT fk_task_attachment_user FOREIGN KEY (uploaded_by) REFERENCES sys_user (id)
);

CREATE INDEX idx_task_attachment_task ON task_attachment (task_id);
