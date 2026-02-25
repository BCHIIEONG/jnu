-- Add a per-session static QR code for attendance check-in.
-- This allows teachers to use a stable QR during an OPEN session without relying on short-lived dynamic tokens.

ALTER TABLE attendance_session
    ADD COLUMN static_code VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_att_session_static_code
    ON attendance_session (static_code);

