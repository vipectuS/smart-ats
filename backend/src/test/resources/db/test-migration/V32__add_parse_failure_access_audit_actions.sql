ALTER TABLE access_audit_events
    DROP CONSTRAINT IF EXISTS chk_access_audit_events_action_type;

ALTER TABLE access_audit_events
    ADD CONSTRAINT chk_access_audit_events_action_type CHECK (
        action_type IN (
            'USER_ACCOUNT_VIEWED',
            'CANDIDATE_PROFILE_VIEWED',
            'CANDIDATE_JOB_MATCHES_VIEWED',
            'RECOMMENDATION_JOB_DETAILS_VIEWED',
            'JOB_APPLICATIONS_VIEWED',
            'ORGANIZATION_CREATED',
            'ORGANIZATION_UPDATED',
            'ORGANIZATION_TOKEN_REGENERATED',
            'SKILL_CREATED',
            'SKILL_UPDATED',
            'RESUME_VIEWED',
            'RESUME_STATUS_VIEWED',
            'JOB_RECOMMENDATIONS_VIEWED',
            'RECOMMENDATION_CANDIDATE_DETAILS_VIEWED',
            'SENSITIVE_FIELD_VIEWED',
            'ORGANIZATION_DIRECTORY_VIEWED',
            'ADMIN_OVERVIEW_VIEWED',
            'SKILL_DICTIONARY_VIEWED',
            'ACCESS_AUDIT_EVENTS_VIEWED',
            'ACCESS_AUDIT_SUMMARY_VIEWED',
            'ACCESS_AUDIT_SUMMARY_EXPORTED',
            'PARSE_FAILURES_VIEWED',
            'PARSE_FAILURE_REVIEW_UPDATED',
            'PARSE_FAILURE_RETRY_QUEUED',
            'PARSE_FAILURE_REVIEW_EVENTS_VIEWED',
            'PARSE_FAILURE_REVIEW_EVENTS_EXPORTED'
        )
    );