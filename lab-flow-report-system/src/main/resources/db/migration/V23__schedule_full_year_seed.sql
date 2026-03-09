-- Full academic year recurring demo schedules for timetable testing.
-- Strategy: materialize concrete course_schedule rows across two semesters so existing timetable/sign-in pages work unchanged.
-- Includes weekly repeats, odd-only/even-only classes, and odd/even room-time differences.

INSERT INTO semester (name, start_date, end_date)
SELECT '2025-2026-1', DATE '2025-09-01', DATE '2026-01-16'
WHERE NOT EXISTS (SELECT 1 FROM semester WHERE name = '2025-2026-1');

INSERT INTO time_slot (code, name, start_time, end_time)
SELECT 'S4', '?7-8?', TIME '15:50:00', TIME '17:30:00'
WHERE NOT EXISTS (SELECT 1 FROM time_slot WHERE code = 'S4');

INSERT INTO time_slot (code, name, start_time, end_time)
SELECT 'S5', '?10-12?', TIME '18:30:00', TIME '21:05:00'
WHERE NOT EXISTS (SELECT 1 FROM time_slot WHERE code = 'S5');

INSERT INTO lab_room (name, location, open_hours)
SELECT 'N325', '??? N325', '????? 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'N325');

INSERT INTO lab_room (name, location, open_hours)
SELECT 'N326', '??? N326', '????? 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'N326');

INSERT INTO lab_room (name, location, open_hours)
SELECT 'N328', '??? N328', '????? 08:00-21:30'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = 'N328');

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-01', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-02', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-03', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-04', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-05', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-01', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-02', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-04', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-05', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-08', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-09', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-10', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-11', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-12', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-08', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-09', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-11', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-12', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-15', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-16', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-17', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-18', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-19', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-15', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-16', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-18', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-19', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-22', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-23', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-24', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-25', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-26', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-22', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-23', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-25', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-26', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-29', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-30', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-01', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-02', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-03', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-29', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-09-30', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-09-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-02', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-03', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-06', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-07', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-07' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-08', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-09', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-10', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-06', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-07', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-07' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-09', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-10', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-13', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-14', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-14' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-15', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-16', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-17', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-13', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-14', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-14' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-16', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-17', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-20', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-21', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-21' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-22', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-23', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-24', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-20', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-21', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-21' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-23', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-24', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-27', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-28', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-28' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-29', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-30', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-31', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-31' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-27', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-28', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-28' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-30', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-10-31', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-10-31' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-03', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-04', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-05', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-06', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-07', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-07' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-03', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-04', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-06', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-07', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-07' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-10', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-11', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-12', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-13', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-14', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-14' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-10', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-11', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-13', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-14', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-14' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-17', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-18', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-19', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-20', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-21', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-21' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-17', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-18', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-20', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-21', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-21' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-24', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-25', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-26', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-27', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-28', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-28' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-24', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-25', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-27', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-11-28', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-11-28' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-01', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-02', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-03', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-04', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-05', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-01', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-02', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-04', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-05', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-08', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-09', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-10', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-11', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-12', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-08', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-09', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-11', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-12', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-15', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-16', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-17', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-18', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-19', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-15', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-16', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-18', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-19', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-22', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-23', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-24', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-25', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-26', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-22', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-23', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-25', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-26', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-29', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-30', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-31', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-31' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-01', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-02', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-29', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2025-12-30', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2025-12-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-01', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-02', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-05', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-06', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-07', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-07' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-08', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-09', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-05', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-06', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-08', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-09', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-12', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-13', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-14', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-14' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-15', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N328'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-16', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-12', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-13', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-15', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-01-16', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-1'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-01-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-16', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-17', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-18', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-18', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-19', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-20', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-20', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-16', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-18', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-19', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-20', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-23', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-24', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-25', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-25', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-26', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-27', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-27', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-23', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-25', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-26', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-02-27', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-02-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-02', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-03', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-04', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-04', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-05', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-06', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-06', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-02', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-04', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-05', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-06', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-09', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-10', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-11', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-11', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-12', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-13', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-13', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-09', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-11', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-12', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-13', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-16', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-17', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-18', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-18', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-19', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-20', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-20', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-16', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-18', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-19', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-20', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-23', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-24', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-25', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-25', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-26', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-27', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-27', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-23', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-25', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-26', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-27', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-30', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-31', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-31' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-01', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-01', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-02', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-03', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-03', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-03-30', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-03-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-01', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-02', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-03', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-06', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-07', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-07' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-08', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-08', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-09', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-10', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-10', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-06', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-08', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-09', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-10', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-13', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-14', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-14' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-15', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-15', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-16', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-17', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-17', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-13', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-15', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-16', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-17', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-20', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-21', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-21' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-22', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-22', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-23', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-24', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-24', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-20', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-22', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-23', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-24', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-27', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-28', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-28' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-29', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-29', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-30', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-01', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-01', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-27', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-29', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-04-30', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-04-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-01', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-04', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-05', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-06', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-06', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-07', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-07' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-08', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-08', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-04', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-06', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-06' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-07', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-07' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-08', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-11', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-12', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-13', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-13', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-14', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-14' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-15', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-15', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-11', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-13', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-13' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-14', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-14' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-15', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-18', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-19', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-20', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-20', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-21', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-21' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-22', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-22', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-18', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-20', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-20' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-21', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-21' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-22', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-25', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-26', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-27', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-27', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-28', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-28' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-29', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-29', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-25', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-27', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-27' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-28', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-28' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-05-29', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-05-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-01', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-02', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-02' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-03', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-03', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-04', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-05', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-05', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-01', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-01' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-03', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-03' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-04', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-04' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-05', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-05' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-08', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-09', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-09' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-10', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-10', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-11', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-12', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-12', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-08', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-08' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-10', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-10' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-11', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-11' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-12', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-12' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-15', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-16', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-16' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-17', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-17', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-18', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-19', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-19', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'DF-107'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-15', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-15' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-17', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S4'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-17' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-18', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-18' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-19', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-19' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-22', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-23', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-23' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-24', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? 201'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-24', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N327'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-25', ts.id, '????I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N319'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-26', ts.id, '????????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N324'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-26', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N326'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-22', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-22' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-24', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-24' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-25', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-25' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-26', ts.id, '?????????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? B203'
JOIN time_slot ts ON ts.code = 'S5'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-26' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-29', ts.id, '??????'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = 'N329'
JOIN time_slot ts ON ts.code = 'S3'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-29' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-30', ts.id, '??I'
FROM semester s
JOIN org_class c ON c.name = '2022?????1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '???'
JOIN time_slot ts ON ts.code = 'S2'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-30' AND x.slot_id = ts.id
  );

INSERT INTO course_schedule (semester_id, class_id, teacher_id, lab_room_id, lesson_date, slot_id, course_name)
SELECT s.id, c.id, t.id, r.id, DATE '2026-06-29', ts.id, '????'
FROM semester s
JOIN org_class c ON c.name = '2022???1?'
JOIN sys_user t ON t.username = 'teacher'
JOIN lab_room r ON r.name = '??? A101'
JOIN time_slot ts ON ts.code = 'S1'
WHERE s.name = '2025-2026-2'
  AND NOT EXISTS (
      SELECT 1 FROM course_schedule x
      WHERE x.semester_id = s.id AND x.class_id = c.id AND x.lesson_date = DATE '2026-06-29' AND x.slot_id = ts.id
  );
