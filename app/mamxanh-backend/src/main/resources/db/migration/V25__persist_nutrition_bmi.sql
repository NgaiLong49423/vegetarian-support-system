-- FR-35: persist the BMI calculated when a Member saves their nutrition profile.
ALTER TABLE [USER]
    ADD nutrition_bmi DECIMAL(18, 12) NULL;
GO

-- Backfill existing consented profiles once so opening the page can reuse the saved BMI.
UPDATE [USER]
SET nutrition_bmi = CONVERT(DECIMAL(18, 12), (weight_kg * 10000.0) / NULLIF(height_cm * height_cm, 0))
WHERE health_data_consent = 1
  AND height_cm IS NOT NULL
  AND weight_kg IS NOT NULL;
GO
