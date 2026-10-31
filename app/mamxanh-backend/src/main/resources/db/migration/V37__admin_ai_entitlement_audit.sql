ALTER TABLE [USER_ACCOUNT_AUDIT] ADD old_tier VARCHAR(10) NULL;
ALTER TABLE [USER_ACCOUNT_AUDIT] ADD new_tier VARCHAR(10) NULL;
ALTER TABLE [USER_ACCOUNT_AUDIT] ADD new_tier_ends_at DATETIME2(7) NULL;
GO

ALTER TABLE [USER_ACCOUNT_AUDIT] DROP CONSTRAINT CK_USER_ACCOUNT_AUDIT_ACTION;
ALTER TABLE [USER_ACCOUNT_AUDIT] ADD CONSTRAINT CK_USER_ACCOUNT_AUDIT_ACTION
    CHECK (action IN ('STATUS_CHANGED','ROLE_CHANGED','AI_PACKAGE_GIFTED'));

ALTER TABLE [USER_ACCOUNT_AUDIT] DROP CONSTRAINT CK_USER_ACCOUNT_AUDIT_VALUES;
ALTER TABLE [USER_ACCOUNT_AUDIT] ADD CONSTRAINT CK_USER_ACCOUNT_AUDIT_VALUES CHECK (
    (action='STATUS_CHANGED' AND old_account_status IS NOT NULL
        AND old_account_status IN ('ACTIVE','LOCKED') AND new_account_status IS NOT NULL
        AND new_account_status IN ('ACTIVE','LOCKED')
        AND old_role IS NULL AND new_role IS NULL AND old_tier IS NULL AND new_tier IS NULL
        AND expert_application_id IS NULL)
    OR
    (action='ROLE_CHANGED' AND old_role IS NOT NULL AND old_role IN ('CUSTOMER','EXPERT')
        AND new_role IS NOT NULL AND new_role IN ('CUSTOMER','EXPERT')
        AND old_account_status IS NULL AND new_account_status IS NULL
        AND old_tier IS NULL AND new_tier IS NULL)
    OR
    (action='AI_PACKAGE_GIFTED' AND old_tier IS NOT NULL AND old_tier IN ('FREE','PLUS','PRO')
        AND new_tier IS NOT NULL AND new_tier IN ('FREE','PLUS','PRO')
        AND ((new_tier='FREE' AND new_tier_ends_at IS NULL) OR (new_tier IN ('PLUS','PRO') AND new_tier_ends_at IS NOT NULL))
        AND old_role IS NULL AND new_role IS NULL
        AND old_account_status IS NULL AND new_account_status IS NULL
        AND expert_application_id IS NULL)
);
