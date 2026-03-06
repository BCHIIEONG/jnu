CREATE TABLE IF NOT EXISTS plag_task_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    algo_version VARCHAR(20) NOT NULL,
    text_threshold DECIMAL(5, 4) NOT NULL,
    image_threshold DECIMAL(5, 4) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    summary_json LONGTEXT
);

CREATE INDEX idx_plag_run_task_started ON plag_task_run (task_id, started_at);

CREATE TABLE IF NOT EXISTS plag_artifact_fp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    attachment_id BIGINT NULL,
    artifact_type VARCHAR(30) NOT NULL,
    algo VARCHAR(20) NOT NULL,
    fp64_hex VARCHAR(16) NOT NULL,
    byte_len BIGINT NOT NULL DEFAULT 0,
    content_type VARCHAR(100),
    file_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plag_fp_run_submission ON plag_artifact_fp (run_id, submission_id);
CREATE INDEX idx_plag_fp_run_student ON plag_artifact_fp (run_id, student_id);
CREATE INDEX idx_plag_fp_run_algo ON plag_artifact_fp (run_id, algo);

CREATE TABLE IF NOT EXISTS plag_submission_best_match (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    best_other_submission_id BIGINT NULL,
    best_other_student_id BIGINT NULL,
    max_score DECIMAL(5, 4) NOT NULL DEFAULT 0,
    evidence_json LONGTEXT,
    skipped_attachments_json LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_plag_best_run_submission ON plag_submission_best_match (run_id, submission_id);
CREATE INDEX idx_plag_best_run_score ON plag_submission_best_match (run_id, max_score);

