-- FR-18: category metadata for the standard ingredient catalog and safe unit factors.
ALTER TABLE [INGREDIENT]
    ADD ingredient_group NVARCHAR(100) NOT NULL
        CONSTRAINT DF_INGREDIENT_ingredient_group DEFAULT N'Khác';
GO

ALTER TABLE [UNIT]
    ADD CONSTRAINT CK_UNIT_base_factor_positive CHECK (base_factor > 0);
GO
