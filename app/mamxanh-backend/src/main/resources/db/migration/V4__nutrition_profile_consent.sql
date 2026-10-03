-- FR-35: record explicit consent before storing self-reported nutrition data.
ALTER TABLE [USER]
    ADD health_data_consent BIT NOT NULL
        CONSTRAINT DF_USER_health_data_consent DEFAULT 0,
        health_data_consent_at DATETIME2(7) NULL;

EXEC(N'ALTER TABLE [USER]
    ADD CONSTRAINT CK_USER_health_data_consent_timestamp CHECK (
        (health_data_consent = 0 AND health_data_consent_at IS NULL)
        OR (health_data_consent = 1 AND health_data_consent_at IS NOT NULL)
    )');
