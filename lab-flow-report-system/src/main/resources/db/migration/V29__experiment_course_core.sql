CREATE TABLE IF NOT EXISTS experiment_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    teacher_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    enroll_deadline_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_experiment_course_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user (id),
    CONSTRAINT fk_experiment_course_semester FOREIGN KEY (semester_id) REFERENCES semester (id)
);

CREATE INDEX idx_experiment_course_teacher ON experiment_course (teacher_id);
CREATE INDEX idx_experiment_course_semester ON experiment_course (semester_id);
CREATE INDEX idx_experiment_course_status ON experiment_course (status);
