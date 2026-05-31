ALTER TABLE access_audit_events
    DROP CONSTRAINT IF EXISTS chk_access_audit_events_sensitive_field;

ALTER TABLE access_audit_events
    ADD CONSTRAINT chk_access_audit_events_sensitive_field CHECK (
        sensitive_field IS NULL OR sensitive_field IN ('ACCOUNT_EMAIL', 'CONTACT_INFO', 'BASIC_INFO_EMAIL', 'BASIC_INFO_PHONE')
    );