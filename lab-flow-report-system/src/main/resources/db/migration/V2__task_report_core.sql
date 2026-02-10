CREATE TABLE IF NOT EXISTS exp_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    publisher_id BIGINT NOT NULL,
    deadline_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_publisher FOREIGN KEY (publisher_id) REFERENCES sys_user (id)
);

CREATE TABLE IF NOT EXISTS report_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    content_md CLOB NOT NULL,
    submit_status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_submission_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES sys_user (id)
);

CREATE UNIQUE INDEX uk_submission_task_student_version
    ON report_submission (task_id, student_id, version_no);

CREATE INDEX idx_submission_task ON report_submission (task_id);
CREATE INDEX idx_submission_student ON report_submission (student_id);

CREATE TABLE IF NOT EXISTS report_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_submission FOREIGN KEY (submission_id) REFERENCES report_submission (id)
);

CREATE TABLE IF NOT EXISTS report_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    score DECIMAL(5, 2) NOT NULL,
    comment CLOB,
    reviewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_submission FOREIGN KEY (submission_id) REFERENCES report_submission (id),
    CONSTRAINT fk_review_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user (id)
);

CREATE UNIQUE INDEX uk_review_submission ON report_review (submission_id);

CREATE TABLE IF NOT EXISTS export_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT NOT NULL,
    export_type VARCHAR(64) NOT NULL,
    condition_json CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_export_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);
