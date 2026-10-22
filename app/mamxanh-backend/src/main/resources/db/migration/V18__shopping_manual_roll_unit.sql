-- FR-53 / AC-53.2: allow a manual item such as "Khăn giấy lau bếp: 2 cuộn".
-- This catalog entry does not imply a conversion to MASS or VOLUME.
IF NOT EXISTS (SELECT 1 FROM [UNIT] WHERE code = N'cuộn')
    INSERT INTO [UNIT] (code, name, dimension, base_factor, is_active)
    VALUES (N'cuộn', N'cuộn', 'COUNT', 1.0, 1);
