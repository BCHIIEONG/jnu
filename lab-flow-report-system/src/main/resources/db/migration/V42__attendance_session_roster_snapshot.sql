CREATE TABLE IF NOT EXISTS attendance_session_roster (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_session_roster_session FOREIGN KEY (session_id) REFERENCES attendance_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_att_session_roster_student FOREIGN KEY (student_id) REFERENCES sys_user (id),
    CONSTRAINT fk_att_session_roster_class FOREIGN KEY (class_id) REFERENCES org_class (id)
);

CREATE UNIQUE INDEX uk_att_session_roster_session_student ON attendance_session_roster (session_id, student_id);
CREATE INDEX idx_att_session_roster_session ON attendance_session_roster (session_id);
CREATE INDEX idx_att_session_roster_student ON attendance_session_roster (student_id);
