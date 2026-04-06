CREATE TABLE IF NOT EXISTS report_review_issue_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    tag_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_issue_tag_review FOREIGN KEY (review_id) REFERENCES report_review(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_review_issue_tag_review_code ON report_review_issue_tag (review_id, tag_code);
CREATE INDEX idx_review_issue_tag_code ON report_review_issue_tag (tag_code);
