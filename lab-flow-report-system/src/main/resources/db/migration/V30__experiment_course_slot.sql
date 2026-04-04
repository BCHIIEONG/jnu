CREATE TABLE IF NOT EXISTS experiment_course_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    lesson_date DATE NOT NULL,
    slot_id BIGINT NOT NULL,
    lab_room_id BIGINT NOT NULL,
    capacity INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_experiment_course_slot_course FOREIGN KEY (course_id) REFERENCES experiment_course (id),
    CONSTRAINT fk_experiment_course_slot_slot FOREIGN KEY (slot_id) REFERENCES time_slot (id),
    CONSTRAINT fk_experiment_course_slot_room FOREIGN KEY (lab_room_id) REFERENCES lab_room (id)
);

CREATE INDEX idx_experiment_course_slot_course ON experiment_course_slot (course_id);
CREATE INDEX idx_experiment_course_slot_date ON experiment_course_slot (lesson_date);
