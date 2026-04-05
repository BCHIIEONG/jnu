CREATE TABLE IF NOT EXISTS lab_room_open_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_room_id BIGINT NOT NULL,
    weekday INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_room_open_slot_room FOREIGN KEY (lab_room_id) REFERENCES lab_room (id)
);

CREATE INDEX idx_lab_room_open_slot_room ON lab_room_open_slot (lab_room_id);
