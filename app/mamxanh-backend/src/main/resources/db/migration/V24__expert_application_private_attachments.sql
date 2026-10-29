-- FR-05 / Issue #144: optional certificate document and application-only avatar.
-- Blob content is stored in a private Azure container; SQL stores only server-generated keys.
ALTER TABLE [EXPERT_APPLICATION]
    ADD certificate_blob_key VARCHAR(512) NULL,
        certificate_content_type VARCHAR(100) NULL,
        application_avatar_blob_key VARCHAR(512) NULL,
        application_avatar_content_type VARCHAR(100) NULL;
GO

ALTER TABLE [EXPERT_APPLICATION]
    ADD CONSTRAINT CK_EXPERT_APP_certificate_attachment_pair CHECK (
        (certificate_blob_key IS NULL AND certificate_content_type IS NULL)
        OR (certificate_blob_key IS NOT NULL
            AND certificate_content_type IN ('application/pdf', 'image/jpeg', 'image/png'))
    ),
    CONSTRAINT CK_EXPERT_APP_avatar_attachment_pair CHECK (
        (application_avatar_blob_key IS NULL AND application_avatar_content_type IS NULL)
        OR (application_avatar_blob_key IS NOT NULL
            AND application_avatar_content_type IN ('image/jpeg', 'image/png', 'image/webp'))
    );
GO
