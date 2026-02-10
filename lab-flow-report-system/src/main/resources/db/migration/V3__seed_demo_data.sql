INSERT INTO sys_role (code, name)
SELECT 'ROLE_ADMIN', '管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'ROLE_ADMIN');

INSERT INTO sys_role (code, name)
SELECT 'ROLE_TEACHER', '教师'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'ROLE_TEACHER');

INSERT INTO sys_role (code, name)
SELECT 'ROLE_STUDENT', '学生'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'ROLE_STUDENT');

INSERT INTO sys_user (username, password_hash, display_name, enabled)
SELECT 'admin', '$2a$10$g6M3miCBuB7ronYtkM/5euqQ.SAAevz3bCYfErd.Xh9Q6jzrEVCYy', '系统管理员', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_user (username, password_hash, display_name, enabled)
SELECT 'teacher', '$2a$10$6MINTo4n3xKWzVvv7.jfOO1qdmcG7/QvX0NtbJk4.ENhXli5GMtH.', '示例教师', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'teacher');

INSERT INTO sys_user (username, password_hash, display_name, enabled)
SELECT 'student', '$2a$10$WPyA9ysvzi8WNJfHRq.Kne4BqS6IT/x2uXODzJSCT53z4zvUYNBSG', '示例学生', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'student');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'ROLE_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'ROLE_TEACHER'
WHERE u.username = 'teacher'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'ROLE_STUDENT'
WHERE u.username = 'student'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

