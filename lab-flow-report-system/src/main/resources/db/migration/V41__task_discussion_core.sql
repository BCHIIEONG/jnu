CREATE TABLE IF NOT EXISTS task_discussion_thread (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    creator_id BIGINT NOT NULL,
    latest_message_id BIGINT NULL,
    latest_message_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_discussion_thread_task FOREIGN KEY (task_id) REFERENCES exp_task (id),
    CONSTRAINT fk_task_discussion_thread_creator FOREIGN KEY (creator_id) REFERENCES sys_user (id)
);

CREATE TABLE IF NOT EXISTS task_discussion_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thread_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    author_role VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_discussion_message_thread FOREIGN KEY (thread_id) REFERENCES task_discussion_thread (id),
    CONSTRAINT fk_task_discussion_message_author FOREIGN KEY (author_id) REFERENCES sys_user (id)
);

CREATE TABLE IF NOT EXISTS task_discussion_read_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thread_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT NULL,
    last_read_at TIMESTAMP NULL,
    CONSTRAINT fk_task_discussion_read_state_thread FOREIGN KEY (thread_id) REFERENCES task_discussion_thread (id),
    CONSTRAINT fk_task_discussion_read_state_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

CREATE UNIQUE INDEX uk_task_discussion_read_state_thread_user ON task_discussion_read_state (thread_id, user_id);
CREATE INDEX idx_task_discussion_thread_task_type ON task_discussion_thread (task_id, type);
CREATE INDEX idx_task_discussion_thread_latest_message_at ON task_discussion_thread (latest_message_at);
CREATE INDEX idx_task_discussion_message_thread_id ON task_discussion_message (thread_id, id);
CREATE INDEX idx_task_discussion_read_state_user_id ON task_discussion_read_state (user_id);
