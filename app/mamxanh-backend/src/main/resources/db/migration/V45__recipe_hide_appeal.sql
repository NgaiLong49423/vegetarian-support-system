-- FR-28 / BR-27: an author may appeal a HIDE_RECIPE decision once; the Administrator restores the post,
-- keeps it hidden, or lets the author edit it while hidden and then reviews the resubmitted post.
ALTER TABLE [REPORT] ADD
    appeal_status          VARCHAR(20)     NULL,
    appeal_message         NVARCHAR(1000)  NULL,
    appealed_at            DATETIME2(7)    NULL,
    appeal_resubmitted_at  DATETIME2(7)    NULL,
    appeal_decision_reason NVARCHAR(1000)  NULL,
    appeal_handled_at      DATETIME2(7)    NULL,
    appeal_handled_by      BIGINT          NULL;
GO

ALTER TABLE [REPORT] ADD CONSTRAINT FK_REPORT_APPEAL_HANDLED_BY FOREIGN KEY (appeal_handled_by)
    REFERENCES [USER](user_id) ON DELETE NO ACTION;
ALTER TABLE [REPORT] ADD CONSTRAINT CK_REPORT_appeal_status CHECK (
    appeal_status IS NULL
    OR appeal_status IN ('PENDING', 'EDIT_ALLOWED', 'RESUBMITTED', 'RESTORED', 'KEPT_HIDDEN')
);
ALTER TABLE [REPORT] ADD CONSTRAINT CK_REPORT_appeal_hide_only CHECK (
    appeal_status IS NULL OR decision = 'HIDE_RECIPE'
);

CREATE NONCLUSTERED INDEX IX_REPORT_appeal_status
    ON [REPORT](appeal_status, recipe_id)
    WHERE appeal_status IS NOT NULL;
