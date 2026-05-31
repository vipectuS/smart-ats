ALTER TABLE access_audit_events
    ADD COLUMN IF NOT EXISTS sensitive_field VARCHAR(64);

ALTER TABLE access_audit_events
    DROP CONSTRAINT IF EXISTS chk_access_audit_events_action_type;

ALTER TABLE access_audit_events
    ADD CONSTRAINT chk_access_audit_events_action_type CHECK (
        action_type IN (
            'CANDIDATE_PROFILE_VIEWED',
            'CANDIDATE_JOB_MATCHES_VIEWED',
            'RESUME_VIEWED',
            'RESUME_STATUS_VIEWED',
            'JOB_RECOMMENDATIONS_VIEWED',
            'RECOMMENDATION_CANDIDATE_DETAILS_VIEWED',
            'SENSITIVE_FIELD_VIEWED',
            'ORGANIZATION_DIRECTORY_VIEWED',
            'ADMIN_OVERVIEW_VIEWED',
            'SKILL_DICTIONARY_VIEWED',
            'PARSE_FAILURE_REVIEW_EVENTS_EXPORTED'
        )
    );

ALTER TABLE access_audit_events
    DROP CONSTRAINT IF EXISTS chk_access_audit_events_sensitive_field;

ALTER TABLE access_audit_events
    ADD CONSTRAINT chk_access_audit_events_sensitive_field CHECK (
        sensitive_field IS NULL OR sensitive_field IN ('CONTACT_INFO', 'BASIC_INFO_EMAIL', 'BASIC_INFO_PHONE')
    );