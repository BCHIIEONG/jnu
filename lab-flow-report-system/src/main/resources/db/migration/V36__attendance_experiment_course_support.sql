ALTER TABLE attendance_session ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'CLASS_SCHEDULE';
ALTER TABLE attendance_session ADD COLUMN experiment_course_id BIGINT NULL;
ALTER TABLE attendance_session ADD COLUMN experiment_course_slot_id BIGINT NULL;
ALTER TABLE attendance_session ADD COLUMN experiment_course_instance_id BIGINT NULL;
ALTER TABLE attendance_session MODIFY COLUMN class_id BIGINT NULL;

ALTER TABLE attendance_session
    ADD CONSTRAINT fk_att_session_experiment_course FOREIGN KEY (experiment_course_id) REFERENCES experiment_course (id);
ALTER TABLE attendance_session
    ADD CONSTRAINT fk_att_session_experiment_slot FOREIGN KEY (experiment_course_slot_id) REFERENCES experiment_course_slot (id);
ALTER TABLE attendance_session
    ADD CONSTRAINT fk_att_session_experiment_instance FOREIGN KEY (experiment_course_instance_id) REFERENCES experiment_course_slot_instance (id);

CREATE INDEX idx_att_session_source_type ON attendance_session (source_type);
CREATE INDEX idx_att_session_experiment_course ON attendance_session (experiment_course_id);
CREATE INDEX idx_att_session_experiment_slot ON attendance_session (experiment_course_slot_id);
CREATE INDEX idx_att_session_experiment_instance ON attendance_session (experiment_course_instance_id);
