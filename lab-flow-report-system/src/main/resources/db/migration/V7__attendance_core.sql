CREATE TABLE IF NOT EXISTS attendance_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NULL,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_session_schedule FOREIGN KEY (schedule_id) REFERENCES course_schedule (id),
    CONSTRAINT fk_att_session_semester FOREIGN KEY (semester_id) REFERENCES semester (id),
    CONSTRAINT fk_att_session_class FOREIGN KEY (class_id) REFERENCES org_class (id),
    CONSTRAINT fk_att_session_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_att_session_class ON attendance_session (class_id);
CREATE INDEX idx_att_session_teacher ON attendance_session (teacher_id);
CREATE INDEX idx_att_session_started_at ON attendance_session (started_at);

CREATE TABLE IF NOT EXISTS attendance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    method VARCHAR(16) NOT NULL,
    checked_in_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(64),
    user_agent VARCHAR(255),
    operator_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_record_session FOREIGN KEY (session_id) REFERENCES attendance_session (id),
    CONSTRAINT fk_att_record_student FOREIGN KEY (student_id) REFERENCES sys_user (id),
    CONSTRAINT fk_att_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);

CREATE UNIQUE INDEX uk_att_record_session_student
    ON attendance_record (session_id, student_id);

CREATE INDEX idx_att_record_checked_in_at ON attendance_record (checked_in_at);

