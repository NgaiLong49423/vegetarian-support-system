-- FR-26/27: align persisted report values with the current requirements baseline.
-- Preserve existing reports while mapping the legacy moderation workflow values.
DROP INDEX UQ_REPORT_open_recipe ON [REPORT];
DROP INDEX UQ_REPORT_open_comment ON [REPORT];
ALTER TABLE [REPORT] DROP CONSTRAINT CK_REPORT_status;
ALTER TABLE [REPORT] DROP CONSTRAINT CK_REPORT_reason_code;
ALTER TABLE [REPORT] DROP CONSTRAINT DF_REPORT_status;

UPDATE [REPORT]
SET status = CASE status
        WHEN 'PENDING' THEN 'OPEN'
        WHEN 'PROCESSING' THEN 'IN_REVIEW'
        WHEN 'REJECTED' THEN 'RESOLVED'
        ELSE status
    END,
    reason_code = CASE reason_code
        WHEN 'NON_VEGAN' THEN 'RECIPE_INFO_OR_DIET_LABEL'
        ELSE reason_code
    END;

ALTER TABLE [REPORT] ADD CONSTRAINT DF_REPORT_status DEFAULT 'OPEN' FOR status;
ALTER TABLE [REPORT] ADD CONSTRAINT CK_REPORT_reason_code CHECK (
    reason_code IN ('RECIPE_INFO_OR_DIET_LABEL', 'FOOD_SAFETY_HAZARD',
                    'SPAM_ADVERTISING', 'INAPPROPRIATE_CONTENT',
                    'COPYRIGHT_VIOLATION', 'OTHER')
);
ALTER TABLE [REPORT] ADD CONSTRAINT CK_REPORT_status CHECK (
    status IN ('OPEN', 'IN_REVIEW', 'RESOLVED')
);

CREATE UNIQUE NONCLUSTERED INDEX UQ_REPORT_open_recipe
    ON [REPORT](reporter_id, recipe_id)
    WHERE recipe_id IS NOT NULL AND status IN ('OPEN', 'IN_REVIEW');

CREATE UNIQUE NONCLUSTERED INDEX UQ_REPORT_open_comment
    ON [REPORT](reporter_id, comment_id)
    WHERE comment_id IS NOT NULL AND status IN ('OPEN', 'IN_REVIEW');
