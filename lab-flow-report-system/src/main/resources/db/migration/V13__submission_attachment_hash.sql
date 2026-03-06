ALTER TABLE report_submission
    ADD COLUMN content_sha256 CHAR(64) NOT NULL DEFAULT '';

ALTER TABLE report_attachment
    ADD COLUMN file_sha256 CHAR(64) NULL;

CREATE INDEX idx_attachment_submission_sha
    ON report_attachment (submission_id, file_sha256);

