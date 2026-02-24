CREATE TABLE IF NOT EXISTS time_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_time_slot_code ON time_slot (code);

CREATE TABLE IF NOT EXISTS course_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    lab_room_id BIGINT NULL,
    lesson_date DATE NOT NULL,
    slot_id BIGINT NOT NULL,
    course_name VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_schedule_semester FOREIGN KEY (semester_id) REFERENCES semester (id),
    CONSTRAINT fk_course_schedule_class FOREIGN KEY (class_id) REFERENCES org_class (id),
    CONSTRAINT fk_course_schedule_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user (id),
    CONSTRAINT fk_course_schedule_lab_room FOREIGN KEY (lab_room_id) REFERENCES lab_room (id),
    CONSTRAINT fk_course_schedule_slot FOREIGN KEY (slot_id) REFERENCES time_slot (id)
);

CREATE UNIQUE INDEX uk_schedule_teacher_time
    ON course_schedule (semester_id, lesson_date, slot_id, teacher_id);

CREATE UNIQUE INDEX uk_schedule_class_time
    ON course_schedule (semester_id, lesson_date, slot_id, class_id);

CREATE INDEX idx_course_schedule_date ON course_schedule (lesson_date);
CREATE INDEX idx_course_schedule_teacher ON course_schedule (teacher_id);
CREATE INDEX idx_course_schedule_class ON course_schedule (class_id);

