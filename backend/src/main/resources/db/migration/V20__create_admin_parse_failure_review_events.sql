CREATE TABLE IF NOT EXISTS admin_parse_failure_review_events (
    id UUID PRIMARY KEY,
    resume_id UUID NOT NULL,
    admin_username VARCHAR(255) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    note TEXT,
    previous_review_status VARCHAR(64) NOT NULL,
    next_review_status VARCHAR(64) NOT NULL,
    resume_status_after_action VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_parse_failure_review_events_resume
        FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE,
    CONSTRAINT chk_admin_parse_failure_review_events_action_type CHECK (
        action_type IN ('REVIEW_SAVED', 'RETRY_QUEUED')
    ),
    CONSTRAINT chk_admin_parse_failure_review_events_previous_review_status CHECK (
        previous_review_status IN (
            'UNREVIEWED',
            'NEEDS_CANDIDATE_UPDATE',
            'APPROVED_FOR_RETRY',
            'NO_FURTHER_ACTION'
        )
    ),
    CONSTRAINT chk_admin_parse_failure_review_events_next_review_status CHECK (
        next_review_status IN (
            'UNREVIEWED',
            'NEEDS_CANDIDATE_UPDATE',
            'APPROVED_FOR_RETRY',
            'NO_FURTHER_ACTION'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_admin_parse_failure_review_events_resume_created_at
    ON admin_parse_failure_review_events (resume_id, created_at DESC);