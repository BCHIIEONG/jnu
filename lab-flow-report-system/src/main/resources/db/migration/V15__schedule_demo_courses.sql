-- Richer schedule demo data for the current teaching week (2026-03-02 ~ 2026-03-06).
-- Goal: make teacher/student timetable pages show multiple different courses immediately.

INSERT INTO lab_room (name, location, open_hours)
SELECT 'N319', '教学楼 N319', '周一至周五 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'N319');

INSERT INTO lab_room (name, location, open_hours)
SELECT 'N324', '教学楼 N324', '周一至周五 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'N324');

INSERT INTO lab_room (name, location, open_hours)
SELECT 'N327', '教学楼 N327', '周一至周五 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'N327');

INSERT INTO lab_room (name, location, open_hours)
SELECT 'N329', '教学楼 N329', '周一至周五 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'N329');

INSERT INTO lab_room (name, location, open_hours)
SELECT 'N411', '教学楼 N411', '周一至周五 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'N411');

INSERT INTO lab_room (name, location, open_hours)
SELECT 'DF-107', '实验楼 DF-107', '周一至周五 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'DF-107');

INSERT INTO lab_room (name, location, open_hours)
SELECT '体育馆', '体育馆主馆', '周一至周五 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = '体育馆');

INSERT INTO lab_room (name, location, open_hours)
SELECT '外语楼 201', '外语楼 201', '周一至周五 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = '外语楼 201');

-- Software engineering demo week.
INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-02', ts.id, '线性代数引论'
FROM semester s
JOIN org_class c ON c.name = '2022级软件工程1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-03', ts.id, '体育I'
FROM semester s
JOIN org_class c ON c.name = '2022级软件工程1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '体育馆'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-04', ts.id, '大学英语一级'
FROM semester s
JOIN org_class c ON c.name = '2022级软件工程1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '外语楼 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-04', ts.id, '中国传统文化概论'
FROM semester s
JOIN org_class c ON c.name = '2022级软件工程1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-05', ts.id, '高等数学I'
FROM semester s
JOIN org_class c ON c.name = '2022级软件工程1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-06', ts.id, '高级语言程序设计'
FROM semester s
JOIN org_class c ON c.name = '2022级软件工程1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-06' AND x.slot_id = ts.id
  );

-- Chemistry demo week for filtering/teacher schedule variety.
INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-02', ts.id, '分析化学实验'
FROM semester s
JOIN org_class c ON c.name = '2022级化学1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '实验室 B203'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-03', ts.id, '无机化学'
FROM semester s
JOIN org_class c ON c.name = '2022级化学1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '实验室 A101'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-05', ts.id, '仪器分析实验'
FROM semester s
JOIN org_class c ON c.name = '2022级化学1班'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '实验室 B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-05' AND x.slot_id = ts.id
  );
