UPDATE org_class
SET grade = 2022,
    name = '软件工程1班',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1
  AND department_id = (SELECT id FROM org_department WHERE name = '计算机学院')
  AND (name = 'c语言' OR name = '2022级软件工程1班');

UPDATE org_class
SET grade = 2022,
    name = '化学1班',
    updated_at = CURRENT_TIMESTAMP
WHERE department_id = (SELECT id FROM org_department WHERE name = '化学学院')
  AND name = '2022级化学1班';

UPDATE org_class
SET grade = 2023,
    name = '软件工程1班',
    updated_at = CURRENT_TIMESTAMP
WHERE department_id = (SELECT id FROM org_department WHERE name = '计算机学院')
  AND name = '2023级软件工程1班';

INSERT INTO org_class (department_id, grade, name, created_at, updated_at)
SELECT d.id, 2022, '软件工程1班', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM org_department d
WHERE d.name = '计算机学院'
  AND NOT EXISTS (
      SELECT 1
      FROM org_class c
      WHERE c.department_id = d.id
        AND c.grade = 2022
        AND c.name = '软件工程1班'
  );

INSERT INTO org_class (department_id, grade, name, created_at, updated_at)
SELECT d.id, 2023, '软件工程1班', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM org_department d
WHERE d.name = '计算机学院'
  AND NOT EXISTS (
      SELECT 1
      FROM org_class c
      WHERE c.department_id = d.id
        AND c.grade = 2023
        AND c.name = '软件工程1班'
  );

INSERT INTO org_class (department_id, grade, name, created_at, updated_at)
SELECT d.id, 2022, '化学1班', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM org_department d
WHERE d.name = '化学学院'
  AND NOT EXISTS (
      SELECT 1
      FROM org_class c
      WHERE c.department_id = d.id
        AND c.grade = 2022
        AND c.name = '化学1班'
  );
