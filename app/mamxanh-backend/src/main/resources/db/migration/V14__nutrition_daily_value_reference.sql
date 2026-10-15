CREATE TABLE [NUTRITION_DAILY_VALUE] (
    nutrient_key VARCHAR(32) NOT NULL,
    label NVARCHAR(80) NOT NULL,
    reference_value DECIMAL(12,3) NOT NULL,
    unit VARCHAR(12) NOT NULL,
    reference_type VARCHAR(24) NOT NULL,
    source_name NVARCHAR(200) NOT NULL,
    source_url VARCHAR(500) NOT NULL,
    version_label VARCHAR(80) NOT NULL,
    effective_date DATE NOT NULL,
    CONSTRAINT PK_NUTRITION_DAILY_VALUE PRIMARY KEY (nutrient_key),
    CONSTRAINT CK_NUTRITION_DAILY_VALUE_positive CHECK (reference_value > 0)
);

INSERT INTO [NUTRITION_DAILY_VALUE]
    (nutrient_key, label, reference_value, unit, reference_type, source_name, source_url, version_label, effective_date)
VALUES
    ('ENERGY_KCAL', N'Năng lượng', 2000, 'kcal', 'GENERAL_GUIDE', N'FDA — Using the Nutrition Facts Label',
     'https://www.fda.gov/food/nutrition-facts-label/using-nutrition-facts-label-older-adults', 'FDA Daily Values current at implementation', '2026-10-13'),
    ('PROTEIN_G', N'Chất đạm', 50, 'g', 'FDA_DV', N'FDA — Daily Value on the Nutrition and Supplement Facts Labels',
     'https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels', 'FDA Daily Values current at implementation', '2026-10-13'),
    ('CARBOHYDRATE_G', N'Carbohydrate', 275, 'g', 'FDA_DV', N'FDA — Daily Value on the Nutrition and Supplement Facts Labels',
     'https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels', 'FDA Daily Values current at implementation', '2026-10-13'),
    ('TOTAL_FAT_G', N'Tổng chất béo', 78, 'g', 'FDA_DV', N'FDA — Daily Value on the Nutrition and Supplement Facts Labels',
     'https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels', 'FDA Daily Values current at implementation', '2026-10-13'),
    ('FIBER_G', N'Chất xơ', 28, 'g', 'FDA_DV', N'FDA — Daily Value on the Nutrition and Supplement Facts Labels',
     'https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels', 'FDA Daily Values current at implementation', '2026-10-13'),
    ('CALCIUM_MG', N'Canxi', 1300, 'mg', 'FDA_DV', N'FDA — Daily Value on the Nutrition and Supplement Facts Labels',
     'https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels', 'FDA Daily Values current at implementation', '2026-10-13'),
    ('IRON_MG', N'Sắt', 18, 'mg', 'FDA_DV', N'FDA — Daily Value on the Nutrition and Supplement Facts Labels',
     'https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels', 'FDA Daily Values current at implementation', '2026-10-13'),
    ('VITAMIN_B12_MCG', N'Vitamin B12', 2.4, 'mcg', 'FDA_DV', N'FDA — Daily Value on the Nutrition and Supplement Facts Labels',
     'https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels', 'FDA Daily Values current at implementation', '2026-10-13'),
    ('ZINC_MG', N'Kẽm', 11, 'mg', 'FDA_DV', N'FDA — Daily Value on the Nutrition and Supplement Facts Labels',
     'https://www.fda.gov/food/nutrition-facts-label/daily-value-nutrition-and-supplement-facts-labels', 'FDA Daily Values current at implementation', '2026-10-13');
