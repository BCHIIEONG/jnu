ALTER TABLE experiment_course_enrollment ADD COLUMN join_source VARCHAR(32) NOT NULL DEFAULT 'STUDENT_SELF';
ALTER TABLE experiment_course_enrollment ADD COLUMN removed_at TIMESTAMP NULL;
ALTER TABLE experiment_course_enrollment ADD COLUMN removed_by_teacher_id BIGINT NULL;

UPDATE experiment_course_enrollment
SET join_source = 'STUDENT_SELF'
WHERE join_source IS NULL OR TRIM(join_source) = '';

CREATE TABLE IF NOT EXISTS experiment_course_blocked_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    blocked_by_teacher_id BIGINT NOT NULL,
    blocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_experiment_course_blocked_course FOREIGN KEY (course_id) REFERENCES experiment_course (id),
    CONSTRAINT fk_experiment_course_blocked_student FOREIGN KEY (student_id) REFERENCES sys_user (id),
    CONSTRAINT fk_experiment_course_blocked_teacher FOREIGN KEY (blocked_by_teacher_id) REFERENCES sys_user (id)
);

CREATE UNIQUE INDEX uk_experiment_course_blocked_course_student ON experiment_course_blocked_student (course_id, student_id);
CREATE INDEX idx_experiment_course_blocked_student ON experiment_course_blocked_student (student_id);
