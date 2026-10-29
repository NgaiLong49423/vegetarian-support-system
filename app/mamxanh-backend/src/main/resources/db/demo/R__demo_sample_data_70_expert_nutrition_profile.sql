-- Fictional nutrition-profile fixture for local demonstration only.
-- Never loaded outside the local Flyway profile; values do not describe a real person.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

UPDATE [USER]
SET date_of_birth = DATEADD(YEAR, -29, CONVERT(DATE, GETDATE())),
    biological_sex = 'FEMALE',
    height_cm = 165.0,
    weight_kg = 60.0,
    nutrition_bmi = CAST(60.0 / POWER(165.0 / 100.0, 2) AS DECIMAL(18,12)),
    activity_level = 'MODERATELY_ACTIVE',
    nutrition_goal = 'IMPROVE_HEALTH',
    pregnant = 0,
    breastfeeding = 0,
    therapeutic_diet_required = 0,
    health_data_consent = 1,
    health_data_consent_at = COALESCE(health_data_consent_at, SYSUTCDATETIME()),
    nutrition_eligibility_status = 'ELIGIBLE',
    nutrition_eligibility_confirmed_at = COALESCE(nutrition_eligibility_confirmed_at, SYSUTCDATETIME()),
    nutrition_scope_confirmed = 1,
    updated_at = SYSUTCDATETIME()
WHERE email = 'demo-expert@mamxanh.local'
  AND role = 'EXPERT'
  AND account_status = 'ACTIVE'
  AND date_of_birth IS NULL
  AND biological_sex IS NULL
  AND height_cm IS NULL
  AND weight_kg IS NULL
  AND activity_level IS NULL
  AND nutrition_goal IS NULL
  AND health_data_consent = 0
  AND nutrition_eligibility_status = 'NOT_CONFIRMED';

COMMIT TRANSACTION;
