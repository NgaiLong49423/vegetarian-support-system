IF COL_LENGTH('dbo.USER', 'preferred_ai_model_id') IS NULL
BEGIN
    ALTER TABLE dbo.[USER] ADD preferred_ai_model_id NVARCHAR(128) NULL;
END;
