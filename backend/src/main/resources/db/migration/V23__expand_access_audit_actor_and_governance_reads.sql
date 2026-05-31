ALTER TABLE access_audit_events
    DROP CONSTRAINT IF EXISTS chk_access_audit_events_actor_role;

ALTER TABLE access_audit_events
    ADD CONSTRAINT chk_access_audit_events_actor_role CHECK (
        actor_role IN ('HR', 'CANDIDATE', 'ADMIN', 'ANONYMOUS')
    );

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
            'ORGANIZATION_DIRECTORY_VIEWED',
            'ADMIN_OVERVIEW_VIEWED',
            'SKILL_DICTIONARY_VIEWED',
            'PARSE_FAILURE_REVIEW_EVENTS_EXPORTED'
        )
    );

ALTER TABLE access_audit_events
    DROP CONSTRAINT IF EXISTS chk_access_audit_events_target_type;

ALTER TABLE access_audit_events
    ADD CONSTRAINT chk_access_audit_events_target_type CHECK (
        target_type IN ('USER', 'RESUME', 'JOB', 'ORGANIZATION_DIRECTORY', 'SYSTEM_OVERVIEW', 'SKILL_DICTIONARY')
    );