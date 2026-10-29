-- Approximate, ingredient-specific unit choices for the local demo catalog only.
-- Values are representative household amounts; admins can refine them in the catalog UI.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @ingredient_conversions TABLE (
    ingredient_name NVARCHAR(200) NOT NULL,
    unit_code NVARCHAR(50) NOT NULL,
    grams_per_unit DECIMAL(18,9) NOT NULL,
    is_approximate BIT NOT NULL,
    PRIMARY KEY (ingredient_name, unit_code),
    CHECK (grams_per_unit > 0)
);

-- Water is 1 g/ml. Oil, soy milk, and soy sauce use typical densities and are approximate.
INSERT INTO @ingredient_conversions (ingredient_name, unit_code, grams_per_unit, is_approximate) VALUES
    (N'Nước', N'ml', 1.000000000, 0),
    (N'Dầu ăn', N'ml', 0.920000000, 1),
    (N'Sữa đậu nành', N'ml', 1.020000000, 1),
    (N'Nước tương', N'ml', 1.080000000, 1),

    -- Fruit: average edible mass per fruit/berry, marked approximate for size variation.
    (N'Bơ', N'quả', 150.000000000, 1),
    (N'Bưởi', N'quả', 600.000000000, 1),
    (N'Cam', N'quả', 130.000000000, 1),
    (N'Chanh', N'quả', 45.000000000, 1),
    (N'Chanh dây', N'quả', 18.000000000, 1),
    (N'Chuối', N'quả', 118.000000000, 1),
    (N'Chuối sứ', N'quả', 100.000000000, 1),
    (N'Chuối xanh', N'quả', 150.000000000, 1),
    (N'Chà là', N'quả', 7.000000000, 1),
    (N'Chôm chôm', N'quả', 12.000000000, 1),
    (N'Dâu tây', N'quả', 12.000000000, 1),
    (N'Dâu tằm', N'quả', 2.000000000, 1),
    (N'Dưa hấu', N'miếng', 150.000000000, 1),
    (N'Dưa lưới', N'miếng', 150.000000000, 1),
    (N'Dứa', N'miếng', 80.000000000, 1),
    (N'Kiwi', N'quả', 69.000000000, 1),
    (N'Lê', N'quả', 178.000000000, 1),
    (N'Lựu', N'quả', 174.000000000, 1),
    (N'Mít', N'miếng', 30.000000000, 1),
    (N'Mơ', N'quả', 35.000000000, 1),
    (N'Mận hậu', N'quả', 66.000000000, 1),
    (N'Nho', N'quả', 5.000000000, 1),
    (N'Nhãn', N'quả', 10.000000000, 1),
    (N'Quýt', N'quả', 88.000000000, 1),
    (N'Sầu riêng', N'miếng', 40.000000000, 1),
    (N'Thanh long', N'quả', 300.000000000, 1),
    (N'Thanh trà', N'quả', 50.000000000, 1),
    (N'Táo', N'quả', 182.000000000, 1),
    (N'Vải', N'quả', 9.000000000, 1),
    (N'Xoài', N'quả', 250.000000000, 1),
    (N'Đu đủ', N'miếng', 150.000000000, 1),
    (N'Ổi', N'quả', 55.000000000, 1),

    -- Whole vegetables and roots use familiar Vietnamese count units.
    (N'Bí ngòi', N'quả', 196.000000000, 1),
    (N'Bí đao', N'miếng', 200.000000000, 1),
    (N'Bí đỏ', N'miếng', 200.000000000, 1),
    (N'Bầu', N'miếng', 200.000000000, 1),
    (N'Bắp cải', N'miếng', 100.000000000, 1),
    (N'Bắp cải tím', N'miếng', 100.000000000, 1),
    (N'Bắp non', N'quả', 20.000000000, 1),
    (N'Cà chua', N'quả', 123.000000000, 1),
    (N'Cà tím', N'quả', 230.000000000, 1),
    (N'Cải Brussels', N'quả', 20.000000000, 1),
    (N'Củ cải trắng', N'củ', 250.000000000, 1),
    (N'Củ cải vàng', N'củ', 85.000000000, 1),
    (N'Củ cải đỏ', N'củ', 45.000000000, 1),
    (N'Củ dền', N'củ', 82.000000000, 1),
    (N'Củ năng', N'củ', 15.000000000, 1),
    (N'Củ sen', N'miếng', 25.000000000, 1),
    (N'Củ sắn', N'củ', 300.000000000, 1),
    (N'Dưa leo', N'quả', 201.000000000, 1),
    (N'Khoai lang', N'củ', 130.000000000, 1),
    (N'Khoai môn', N'củ', 80.000000000, 1),
    (N'Khoai mỡ', N'củ', 150.000000000, 1),
    (N'Khoai sọ', N'củ', 70.000000000, 1),
    (N'Khoai tây', N'củ', 213.000000000, 1),
    (N'Khổ qua', N'quả', 101.000000000, 1),
    (N'Mướp hương', N'quả', 200.000000000, 1),
    (N'Mướp khía', N'quả', 200.000000000, 1),
    (N'Su su', N'quả', 203.000000000, 1),
    (N'Ớt chuông', N'quả', 119.000000000, 1),
    (N'Đậu bắp', N'quả', 12.000000000, 1),
    (N'Đậu bắp tím', N'quả', 12.000000000, 1),
    (N'Đậu cove vàng', N'miếng', 10.000000000, 1),
    (N'Đậu đũa', N'miếng', 10.000000000, 1),
    (N'Cà rốt', N'củ', 61.000000000, 1),
    (N'Hành tây', N'củ', 110.000000000, 1),
    (N'Hành tím', N'củ', 10.000000000, 1),
    (N'Gừng', N'miếng', 25.000000000, 1),
    (N'Nghệ tươi', N'miếng', 20.000000000, 1),
    (N'Riềng', N'miếng', 25.000000000, 1),
    (N'Tỏi', N'củ', 40.000000000, 1),
    (N'Ớt', N'quả', 5.000000000, 1),
    (N'Ớt đỏ', N'quả', 5.000000000, 1),
    (N'Trứng gà', N'quả', 50.000000000, 1),

    -- A piece is useful for these commonly portioned foods; all are approximate.
    (N'Bánh mì nguyên cám', N'miếng', 30.000000000, 1),
    (N'Đậu hũ', N'miếng', 50.000000000, 1),
    (N'Đậu phụ non', N'miếng', 30.000000000, 1),
    (N'Tempeh', N'miếng', 50.000000000, 1),
    (N'Seitan', N'miếng', 50.000000000, 1),
    (N'Chả giò chay', N'miếng', 25.000000000, 1),
    (N'Phô mai cheddar', N'miếng', 28.000000000, 1),
    (N'Phô mai cottage', N'miếng', 15.000000000, 1),
    (N'Phô mai cream cheese', N'miếng', 15.000000000, 1),
    (N'Phô mai mozzarella', N'miếng', 28.000000000, 1),
    (N'Phô mai parmesan', N'miếng', 5.000000000, 1),
    (N'Phô mai xanh', N'miếng', 28.000000000, 1),
    (N'Phô mai chay đậu nành', N'miếng', 28.000000000, 1),
    (N'Phô mai chay hạt điều', N'miếng', 28.000000000, 1);

IF EXISTS (
    SELECT 1 FROM @ingredient_conversions seed
    WHERE NOT EXISTS (SELECT 1 FROM [INGREDIENT] i WHERE i.name = seed.ingredient_name)
       OR NOT EXISTS (SELECT 1 FROM [UNIT] u WHERE u.code = seed.unit_code AND u.dimension IN (N'VOLUME', N'COUNT'))
)
BEGIN
    THROW 51000, 'Demo ingredient unit fixture references a missing ingredient or non-volume/count unit.', 1;
END;

-- Insert missing pairs only. Existing Admin values and active/inactive choices remain authoritative.
INSERT INTO [INGREDIENT_UNIT_CONVERSION]
    (ingredient_id, unit_id, grams_per_unit, is_approximate, is_active)
SELECT i.ingredient_id, u.unit_id,
       CASE WHEN seed.unit_code = N'ml' THEN seed.grams_per_unit * u.base_factor ELSE seed.grams_per_unit END,
       seed.is_approximate, 1
FROM @ingredient_conversions seed
JOIN [INGREDIENT] i ON i.name = seed.ingredient_name AND i.status = 'ACTIVE'
JOIN [UNIT] u ON u.is_active = 1 AND (
    u.code = seed.unit_code
    OR (seed.unit_code = N'ml' AND u.dimension = N'VOLUME' AND u.code IN (N'L', N'tsp', N'tbsp', N'cup'))
)
WHERE NOT EXISTS (
    SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] existing
    WHERE existing.ingredient_id = i.ingredient_id AND existing.unit_id = u.unit_id
);

COMMIT TRANSACTION;
