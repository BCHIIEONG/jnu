-- Seed minimal admin baseline data for demos (departments/classes/labs/devices/semesters).
-- Keep scripts idempotent using WHERE NOT EXISTS patterns (compatible with MySQL + H2 MySQL mode).

INSERT INTO org_department (name)
SELECT '计算机学院'
WHERE NOT EXISTS (SELECT 1 FROM org_department WHERE name = '计算机学院');

INSERT INTO org_department (name)
SELECT '化学学院'
WHERE NOT EXISTS (SELECT 1 FROM org_department WHERE name = '化学学院');

INSERT INTO org_class (department_id, name)
SELECT d.id, '2022级软件工程1班'
FROM org_department d
WHERE d.name = '计算机学院'
  AND NOT EXISTS (
      SELECT 1 FROM org_class c
      WHERE c.department_id = d.id AND c.name = '2022级软件工程1班'
  );

INSERT INTO org_class (department_id, name)
SELECT d.id, '2022级化学1班'
FROM org_department d
WHERE d.name = '化学学院'
  AND NOT EXISTS (
      SELECT 1 FROM org_class c
      WHERE c.department_id = d.id AND c.name = '2022级化学1班'
  );

-- Optionally attach demo users to a department/class (safe to run multiple times).
UPDATE sys_user
SET department_id = (SELECT id FROM org_department WHERE name = '计算机学院'),
    class_id = (SELECT c.id FROM org_class c
                JOIN org_department d ON d.id = c.department_id
                WHERE d.name = '计算机学院' AND c.name = '2022级软件工程1班')
WHERE username = 'student'
  AND (department_id IS NULL OR class_id IS NULL);

UPDATE sys_user
SET department_id = (SELECT id FROM org_department WHERE name = '计算机学院')
WHERE username IN ('teacher', 'admin')
  AND department_id IS NULL;

INSERT INTO lab_room (name, location, open_hours)
SELECT '实验室 A101', '教学楼A区 1层', '周一至周五 08:00-18:00'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = '实验室 A101');

INSERT INTO lab_room (name, location, open_hours)
SELECT '实验室 B203', '教学楼B区 2层', '周一至周五 08:00-18:00'
WHERE NOT EXISTS (SELECT 1 FROM lab_room WHERE name = '实验室 B203');

INSERT INTO device (code, name, status, location, description)
SELECT 'DEV-001', '示波器', 'AVAILABLE', '实验室 A101', '教学演示设备'
WHERE NOT EXISTS (SELECT 1 FROM device WHERE code = 'DEV-001');

INSERT INTO device (code, name, status, location, description)
SELECT 'DEV-002', '万用表', 'AVAILABLE', '实验室 B203', '可借用设备'
WHERE NOT EXISTS (SELECT 1 FROM device WHERE code = 'DEV-002');

INSERT INTO semester (name, start_date, end_date)
SELECT '2025-2026-2', DATE '2026-02-16', DATE '2026-06-30'
WHERE NOT EXISTS (SELECT 1 FROM semester WHERE name = '2025-2026-2');

