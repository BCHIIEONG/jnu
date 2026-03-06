CREATE TABLE IF NOT EXISTS exp_task_target_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_target_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT fk_task_target_class FOREIGN KEY (class_id) REFERENCES org_class (id)
);

CREATE UNIQUE INDEX uk_task_target_task_class
    ON exp_task_target_class (task_id, class_id);

CREATE INDEX idx_task_target_task
    ON exp_task_target_class (task_id);

CREATE INDEX idx_task_target_class
    ON exp_task_target_class (class_id);

