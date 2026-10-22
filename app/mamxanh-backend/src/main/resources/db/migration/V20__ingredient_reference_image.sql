-- FR-18: persist the optional image reference managed by an Administrator.
ALTER TABLE [INGREDIENT]
    ADD image_url NVARCHAR(2048) NULL;
