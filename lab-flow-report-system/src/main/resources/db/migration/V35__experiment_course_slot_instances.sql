CREATE TABLE IF NOT EXISTS experiment_course_slot_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    slot_group_id BIGINT NOT NULL,
    lesson_date DATE NOT NULL,
    teaching_week INT NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    slot_id BIGINT NOT NULL,
    lab_room_id BIGINT NOT NULL,
    capacity INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_experiment_course_slot_instance_course FOREIGN KEY (course_id) REFERENCES experiment_course (id),
    CONSTRAINT fk_experiment_course_slot_instance_group FOREIGN KEY (slot_group_id) REFERENCES experiment_course_slot (id) ON DELETE CASCADE,
    CONSTRAINT fk_experiment_course_slot_instance_slot FOREIGN KEY (slot_id) REFERENCES time_slot (id),
    CONSTRAINT fk_experiment_course_slot_instance_room FOREIGN KEY (lab_room_id) REFERENCES lab_room (id)
);

CREATE INDEX idx_experiment_course_slot_instance_course ON experiment_course_slot_instance (course_id);
CREATE INDEX idx_experiment_course_slot_instance_group ON experiment_course_slot_instance (slot_group_id);
CREATE INDEX idx_experiment_course_slot_instance_date ON experiment_course_slot_instance (lesson_date);
CREATE UNIQUE INDEX uk_experiment_course_slot_instance_group_date ON experiment_course_slot_instance (slot_group_id, lesson_date);

INSERT INTO experiment_course_slot_instance (
    course_id,
    slot_group_id,
    lesson_date,
    teaching_week,
    display_name,
    slot_id,
    lab_room_id,
    capacity,
    created_at
)
SELECT ecs.course_id,
       ecs.id,
       COALESCE(ecs.first_lesson_date, ecs.lesson_date),
       1,
       CASE
           WHEN ecs.name IS NOT NULL AND TRIM(ecs.name) <> '' THEN CONCAT(ecs.name, ' 第1周')
           ELSE CONCAT('场次', ecs.id, ' 第1周')
       END,
       ecs.slot_id,
       ecs.lab_room_id,
       ecs.capacity,
       COALESCE(ecs.created_at, CURRENT_TIMESTAMP)
FROM experiment_course_slot ecs;
