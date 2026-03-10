CREATE UNIQUE INDEX uk_org_class_department_grade_name
    ON org_class (department_id, grade, name);

DROP INDEX uk_org_class_department_name ON org_class;
