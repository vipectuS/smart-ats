CREATE TABLE IF NOT EXISTS access_audit_events (
    id UUID PRIMARY KEY,
    actor_username VARCHAR(255) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_access_audit_events_actor_role CHECK (
        actor_role IN ('HR', 'CANDIDATE', 'ADMIN')
    ),
    CONSTRAINT chk_access_audit_events_action_type CHECK (
        action_type IN (
            'RESUME_VIEWED',
            'JOB_RECOMMENDATIONS_VIEWED',
            'PARSE_FAILURE_REVIEW_EVENTS_EXPORTED'
        )
    ),
    CONSTRAINT chk_access_audit_events_target_type CHECK (
        target_type IN ('RESUME', 'JOB')
    )
);

CREATE INDEX IF NOT EXISTS idx_access_audit_events_created_at
    ON access_audit_events (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_access_audit_events_action_type
    ON access_audit_events (action_type);

CREATE INDEX IF NOT EXISTS idx_access_audit_events_target_type_target_id
    ON access_audit_events (target_type, target_id);