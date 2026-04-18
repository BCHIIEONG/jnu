CREATE TABLE IF NOT EXISTS task_prestudy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description LONGTEXT,
    version INT NOT NULL DEFAULT 1,
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_prestudy_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT uk_task_prestudy_task UNIQUE (task_id)
);

CREATE TABLE IF NOT EXISTS task_prestudy_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prestudy_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_prestudy_attachment_prestudy FOREIGN KEY (prestudy_id) REFERENCES task_prestudy (id),
    CONSTRAINT fk_task_prestudy_attachment_user FOREIGN KEY (uploaded_by) REFERENCES sys_user (id)
);

CREATE INDEX idx_task_prestudy_attachment_prestudy
    ON task_prestudy_attachment (prestudy_id);

CREATE TABLE IF NOT EXISTS task_prestudy_read_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prestudy_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    last_read_version INT NOT NULL DEFAULT 0,
    last_read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_prestudy_read_prestudy FOREIGN KEY (prestudy_id) REFERENCES task_prestudy (id),
    CONSTRAINT fk_task_prestudy_read_student FOREIGN KEY (student_id) REFERENCES sys_user (id),
    CONSTRAINT uk_task_prestudy_read_student UNIQUE (prestudy_id, student_id)
);

CREATE INDEX idx_task_prestudy_read_student
    ON task_prestudy_read_state (student_id);
