CREATE TABLE IF NOT EXISTS task_device_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    max_quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_device_config_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT fk_task_device_config_device FOREIGN KEY (device_id) REFERENCES device (id),
    CONSTRAINT uk_task_device_config_task_device UNIQUE (task_id, device_id)
);

CREATE INDEX idx_task_device_config_task ON task_device_config (task_id);
