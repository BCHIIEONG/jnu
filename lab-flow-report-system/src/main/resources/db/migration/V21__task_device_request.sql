CREATE TABLE IF NOT EXISTS task_device_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    note TEXT,
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    rejected_by BIGINT NULL,
    rejected_at TIMESTAMP NULL,
    checkout_by BIGINT NULL,
    checkout_at TIMESTAMP NULL,
    return_by BIGINT NULL,
    return_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_device_request_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT fk_task_device_request_student FOREIGN KEY (student_id) REFERENCES sys_user (id),
    CONSTRAINT fk_task_device_request_device FOREIGN KEY (device_id) REFERENCES device (id),
    CONSTRAINT fk_task_device_request_approved_by FOREIGN KEY (approved_by) REFERENCES sys_user (id),
    CONSTRAINT fk_task_device_request_rejected_by FOREIGN KEY (rejected_by) REFERENCES sys_user (id),
    CONSTRAINT fk_task_device_request_checkout_by FOREIGN KEY (checkout_by) REFERENCES sys_user (id),
    CONSTRAINT fk_task_device_request_return_by FOREIGN KEY (return_by) REFERENCES sys_user (id)
);

CREATE INDEX idx_task_device_request_task ON task_device_request (task_id);
CREATE INDEX idx_task_device_request_student ON task_device_request (student_id);
CREATE INDEX idx_task_device_request_device ON task_device_request (device_id);
CREATE INDEX idx_task_device_request_status ON task_device_request (status);
