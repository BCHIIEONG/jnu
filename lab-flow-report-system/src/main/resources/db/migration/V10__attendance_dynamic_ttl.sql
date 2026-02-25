-- Add per-session dynamic token TTL for attendance QR code.
-- Default remains 6 seconds unless configured by teacher.

ALTER TABLE attendance_session
    ADD COLUMN token_ttl_seconds INT NOT NULL DEFAULT 6;

