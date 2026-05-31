ALTER TABLE access_audit_events
    DROP CONSTRAINT IF EXISTS chk_access_audit_events_action_type;

ALTER TABLE access_audit_events
    ADD CONSTRAINT chk_access_audit_events_action_type CHECK (
        action_type IN (
            'CANDIDATE_PROFILE_VIEWED',
            'CANDIDATE_JOB_MATCHES_VIEWED',
            'RECOMMENDATION_JOB_DETAILS_VIEWED',
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
            'PARSE_FAILURE_REVIEW_EVENTS_EXPORTED'
        )
    );