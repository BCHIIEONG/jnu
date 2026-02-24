-- Seed minimal schedule + attendance baseline data for demos.
-- Keep idempotent using WHERE NOT EXISTS patterns (compatible with MySQL + H2 MySQL mode).

INSERT INTO time_slot (code, name, start_time, end_time)
SELECT 'S1', '第1-2节', TIME '08:00:00', TIME '09:40:00'
WHERE NOT EXISTS (SELECT 1 FROM time_slot WHERE code = 'S1');

INSERT INTO time_slot (code, name, start_time, end_time)
SELECT 'S2', '第3-4节', TIME '10:00:00', TIME '11:40:00'
WHERE NOT EXISTS (SELECT 1 FROM time_slot WHERE code = 'S2');

INSERT INTO time_slot (code, name, start_time, end_time)
SELECT 'S3', '第5-6节', TIME '14:00:00', TIME '15:40:00'
WHERE NOT EXISTS (SELECT 1 FROM time_slot WHERE code = 'S3');

-- Demo schedules in the same week as 2026-02-24 (fixed dates to be deterministic).
INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id,
       c.id,
       t.id,
       r.id,
       DATE '2026-02-24',
       ts.id,
       '实验课（示例）'
FROM semester s
JOIN org_class c ON c.name = '2022级软件工程1班'
JOIN sys_user t ON t.username = 'teacher'
LEFT JOIN lab_room r ON r.name = '实验室 A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.lesson_date = DATE '2026-02-24' AND x.slot_id = ts.id AND x.class_id = c.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id,
       c.id,
       t.id,
       r.id,
       DATE '2026-02-26',
       ts.id,
       '实验课（示例）'
FROM semester s
JOIN org_class c ON c.name = '2022级软件工程1班'
JOIN sys_user t ON t.username = 'teacher'
LEFT JOIN lab_room r ON r.name = '实验室 A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.lesson_date = DATE '2026-02-26' AND x.slot_id = ts.id AND x.class_id = c.id
  );

