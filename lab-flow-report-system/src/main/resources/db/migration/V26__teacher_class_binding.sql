CREATE TABLE IF NOT EXISTS sys_user_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_user_class_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_class_class FOREIGN KEY (class_id) REFERENCES org_class (id)
);

CREATE UNIQUE INDEX uk_sys_user_class_user_class ON sys_user_class (user_id, class_id);
CREATE INDEX idx_sys_user_class_user ON sys_user_class (user_id);
CREATE INDEX idx_sys_user_class_class ON sys_user_class (class_id);

INSERT INTO sys_user_class (user_id, class_id, created_at)
SELECT DISTINCT su.id, su.class_id, CURRENT_TIMESTAMP
FROM sys_user su
JOIN sys_user_role ur ON ur.user_id = su.id
JOIN sys_role r ON r.id = ur.role_id
LEFT JOIN sys_user_class suc ON suc.user_id = su.id AND suc.class_id = su.class_id
WHERE r.code = 'ROLE_TEACHER'
  AND su.class_id IS NOT NULL
  AND suc.id IS NULL;
