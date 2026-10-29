CREATE TABLE [USER_ACCOUNT_AUDIT] (
    user_account_audit_id BIGINT IDENTITY(1,1) NOT NULL,
    target_user_id BIGINT NOT NULL,
    administrator_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_role VARCHAR(20) NULL,
    new_role VARCHAR(20) NULL,
    old_account_status VARCHAR(20) NULL,
    new_account_status VARCHAR(20) NULL,
    reason NVARCHAR(500) NOT NULL,
    expert_application_id BIGINT NULL,
    created_at DATETIME2(7) NOT NULL,
    CONSTRAINT PK_USER_ACCOUNT_AUDIT PRIMARY KEY (user_account_audit_id),
    CONSTRAINT FK_USER_ACCOUNT_AUDIT_TARGET FOREIGN KEY (target_user_id) REFERENCES [USER](user_id),
    CONSTRAINT FK_USER_ACCOUNT_AUDIT_ADMIN FOREIGN KEY (administrator_id) REFERENCES [USER](user_id),
    CONSTRAINT FK_USER_ACCOUNT_AUDIT_APPLICATION FOREIGN KEY (expert_application_id) REFERENCES [EXPERT_APPLICATION](application_id),
    CONSTRAINT CK_USER_ACCOUNT_AUDIT_ACTION CHECK (action IN ('STATUS_CHANGED','ROLE_CHANGED')),
    CONSTRAINT CK_USER_ACCOUNT_AUDIT_REASON CHECK (LEN(LTRIM(RTRIM(reason))) BETWEEN 10 AND 500),
    CONSTRAINT CK_USER_ACCOUNT_AUDIT_VALUES CHECK (
        (action='STATUS_CHANGED' AND old_account_status IS NOT NULL
            AND old_account_status IN ('ACTIVE','LOCKED') AND new_account_status IS NOT NULL
            AND new_account_status IN ('ACTIVE','LOCKED')
            AND old_role IS NULL AND new_role IS NULL AND expert_application_id IS NULL)
        OR
        (action='ROLE_CHANGED' AND old_role IS NOT NULL AND old_role IN ('CUSTOMER','EXPERT')
            AND new_role IS NOT NULL AND new_role IN ('CUSTOMER','EXPERT')
            AND old_account_status IS NULL AND new_account_status IS NULL)
    )
);

CREATE INDEX IX_USER_ACCOUNT_AUDIT_TARGET_CREATED
    ON [USER_ACCOUNT_AUDIT](target_user_id, created_at DESC, user_account_audit_id DESC);

ALTER TABLE [EXPERT_APPLICATION] DROP CONSTRAINT CK_EXPERT_APP_status;
ALTER TABLE [EXPERT_APPLICATION] ADD CONSTRAINT CK_EXPERT_APP_status
    CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'));
