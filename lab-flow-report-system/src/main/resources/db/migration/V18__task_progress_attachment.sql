CREATE TABLE IF NOT EXISTS task_progress_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    progress_log_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    file_size BIGINT NOT NULL DEFAULT 0,
    relative_path VARCHAR(500) NOT NULL,
    file_sha256 CHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_progress_attachment_log FOREIGN KEY (progress_log_id) REFERENCES task_progress_log (id)
);

CREATE INDEX idx_task_progress_attachment_log ON task_progress_attachment (progress_log_id);
