ALTER TABLE exp_task
    ADD COLUMN experiment_course_id BIGINT NULL;

ALTER TABLE exp_task
    ADD CONSTRAINT fk_exp_task_experiment_course FOREIGN KEY (experiment_course_id) REFERENCES experiment_course (id);

CREATE INDEX idx_exp_task_experiment_course ON exp_task (experiment_course_id);
