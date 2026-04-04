CREATE TABLE IF NOT EXISTS experiment_course_target_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    CONSTRAINT fk_experiment_course_target_class_course FOREIGN KEY (course_id) REFERENCES experiment_course (id),
    CONSTRAINT fk_experiment_course_target_class_class FOREIGN KEY (class_id) REFERENCES org_class (id)
);

CREATE UNIQUE INDEX uk_experiment_course_target_class ON experiment_course_target_class (course_id, class_id);
CREATE INDEX idx_experiment_course_target_class_class ON experiment_course_target_class (class_id);

CREATE TABLE IF NOT EXISTS experiment_course_target_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT fk_experiment_course_target_student_course FOREIGN KEY (course_id) REFERENCES experiment_course (id),
    CONSTRAINT fk_experiment_course_target_student_student FOREIGN KEY (student_id) REFERENCES sys_user (id)
);

CREATE UNIQUE INDEX uk_experiment_course_target_student ON experiment_course_target_student (course_id, student_id);
CREATE INDEX idx_experiment_course_target_student_student ON experiment_course_target_student (student_id);
