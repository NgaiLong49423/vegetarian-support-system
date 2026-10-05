ALTER TABLE [NOTIFICATION]
    ADD [target_path] NVARCHAR(2048) NULL;

CREATE NONCLUSTERED INDEX [IX_EXPERT_APP_pending_created]
    ON [EXPERT_APPLICATION]([created_at] DESC, [application_id] DESC)
    WHERE [status] = 'PENDING';

CREATE NONCLUSTERED INDEX [IX_EXPERT_APP_user_history]
    ON [EXPERT_APPLICATION]([user_id], [created_at] DESC, [application_id] DESC);
