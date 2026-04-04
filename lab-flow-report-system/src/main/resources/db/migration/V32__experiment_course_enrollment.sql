CREATE TABLE IF NOT EXISTS experiment_course_enrollment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    selected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_experiment_course_enrollment_course FOREIGN KEY (course_id) REFERENCES experiment_course (id),
    CONSTRAINT fk_experiment_course_enrollment_slot FOREIGN KEY (slot_id) REFERENCES experiment_course_slot (id),
    CONSTRAINT fk_experiment_course_enrollment_student FOREIGN KEY (student_id) REFERENCES sys_user (id)
);

CREATE UNIQUE INDEX uk_experiment_course_enrollment_course_student ON experiment_course_enrollment (course_id, student_id);
CREATE INDEX idx_experiment_course_enrollment_slot ON experiment_course_enrollment (slot_id);
CREATE INDEX idx_experiment_course_enrollment_student ON experiment_course_enrollment (student_id);
