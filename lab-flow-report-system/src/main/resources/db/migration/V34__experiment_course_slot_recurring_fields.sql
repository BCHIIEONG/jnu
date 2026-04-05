ALTER TABLE experiment_course_slot ADD COLUMN name VARCHAR(120) NULL;
ALTER TABLE experiment_course_slot ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'SINGLE';
ALTER TABLE experiment_course_slot ADD COLUMN first_lesson_date DATE NULL;
ALTER TABLE experiment_course_slot ADD COLUMN repeat_pattern VARCHAR(20) NULL;
ALTER TABLE experiment_course_slot ADD COLUMN range_mode VARCHAR(20) NULL;
ALTER TABLE experiment_course_slot ADD COLUMN range_start_date DATE NULL;
ALTER TABLE experiment_course_slot ADD COLUMN range_end_date DATE NULL;

UPDATE experiment_course_slot
SET first_lesson_date = lesson_date,
    mode = 'SINGLE',
    range_start_date = lesson_date,
    range_end_date = lesson_date
WHERE first_lesson_date IS NULL;

CREATE INDEX idx_experiment_course_slot_first_date ON experiment_course_slot (first_lesson_date);
CREATE INDEX idx_experiment_course_slot_mode ON experiment_course_slot (mode);
