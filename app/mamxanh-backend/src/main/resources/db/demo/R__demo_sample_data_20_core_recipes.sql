-- Repeatable demo fixtures for local development only.
-- Password hashes are initialized at runtime from MAMXANH_DEMO_PASSWORD and are never stored here.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @author_id BIGINT;
SELECT @author_id = user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local';
DECLARE @gram INT = (SELECT unit_id FROM [UNIT] WHERE name = N'gram');

DECLARE @mushroom BIGINT, @shiitake BIGINT, @carrot BIGINT, @sweet_potato BIGINT, @broccoli BIGINT, @lentil BIGINT;
SELECT @mushroom = ingredient_id FROM [INGREDIENT] WHERE name = N'Nấm đùi gà';
SELECT @shiitake = ingredient_id FROM [INGREDIENT] WHERE name = N'Nấm hương';
SELECT @carrot = ingredient_id FROM [INGREDIENT] WHERE name = N'Cà rốt';
SELECT @sweet_potato = ingredient_id FROM [INGREDIENT] WHERE name = N'Khoai lang';
SELECT @broccoli = ingredient_id FROM [INGREDIENT] WHERE name = N'Bông cải xanh';
SELECT @lentil = ingredient_id FROM [INGREDIENT] WHERE name = N'Đậu lăng';

DECLARE @recipe_seed TABLE (title NVARCHAR(120), description NVARCHAR(2000), instructions NVARCHAR(MAX), category VARCHAR(20), difficulty VARCHAR(10), prep INT, cook INT);
INSERT INTO @recipe_seed VALUES
 (N'Nấm đùi gà kho tiêu', N'Nấm kho đậm vị, dùng cùng cơm nóng.', N'Cắt nấm thành lát. Phi thơm tiêu và gia vị, cho nấm vào kho nhỏ lửa đến khi thấm.', 'BRAISED', 'EASY', 10, 20),
 (N'Canh nấm hương cà rốt', N'Canh rau củ thanh nhẹ cho bữa cơm gia đình.', N'Ngâm nấm hương và cắt cà rốt. Đun mềm rau củ trong nước dùng rau, nêm vừa ăn.', 'SOUP', 'EASY', 10, 20),
 (N'Khoai lang hấp', N'Món phụ đơn giản từ khoai lang.', N'Rửa sạch khoai lang, cắt khúc vừa ăn và hấp đến khi mềm.', 'STEAMED', 'EASY', 5, 20),
 (N'Bông cải xanh xào nấm', N'Rau xanh xào nhanh cùng nấm.', N'Rửa sạch bông cải và nấm. Xào nấm trước, thêm bông cải và đảo đến khi vừa chín.', 'STIR_FRY', 'EASY', 10, 10),
 (N'Súp đậu lăng rau củ', N'Súp đậu lăng ấm bụng với rau củ.', N'Rửa đậu lăng. Nấu cùng cà rốt và gia vị đến khi đậu mềm, khuấy đều trước khi dùng.', 'SOUP', 'MEDIUM', 10, 35);

INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, published_at)
SELECT @author_id, s.title, s.description, s.instructions, s.category, 'VEGAN', s.difficulty, 2, s.prep, s.cook, SYSUTCDATETIME()
FROM @recipe_seed s
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_POST] p WHERE p.author_id = @author_id AND p.title = s.title);

DECLARE @recipe_ingredients TABLE (title NVARCHAR(120), ingredient_id BIGINT, unit_id INT, quantity DECIMAL(10,2));
INSERT INTO @recipe_ingredients VALUES
 (N'Nấm đùi gà kho tiêu', @mushroom, @gram, 250),
 (N'Canh nấm hương cà rốt', @shiitake, @gram, 30), (N'Canh nấm hương cà rốt', @carrot, @gram, 100),
 (N'Khoai lang hấp', @sweet_potato, @gram, 300),
 (N'Bông cải xanh xào nấm', @broccoli, @gram, 200), (N'Bông cải xanh xào nấm', @mushroom, @gram, 100),
 (N'Súp đậu lăng rau củ', @lentil, @gram, 150), (N'Súp đậu lăng rau củ', @carrot, @gram, 80);
INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT p.recipe_id, i.ingredient_id, i.unit_id, i.quantity
FROM @recipe_ingredients i JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = i.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri WHERE ri.recipe_id = p.recipe_id);

-- Local-only cover images use the Frontend's public demo assets so the recipe
-- cards can render without depending on a developer-specific Azure account.
DECLARE @recipe_media_seed TABLE (title NVARCHAR(120), blob_url VARCHAR(2048));
INSERT INTO @recipe_media_seed VALUES
 (N'Đậu hũ sốt cà chua', '/assets/95427.png'),
 (N'Rau muống xào tỏi', '/assets/cddd4.png'),
 (N'Nấm đùi gà kho tiêu', '/assets/40c2c.png'),
 (N'Canh nấm hương cà rốt', '/assets/40053.png'),
 (N'Khoai lang hấp', '/assets/8ef48.png'),
 (N'Bông cải xanh xào nấm', '/assets/09723.png'),
 (N'Súp đậu lăng rau củ', '/assets/5a53c.png');

INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT p.recipe_id, s.blob_url, 'image/png', 1, 1
FROM @recipe_media_seed s
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = s.title
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_MEDIA] rm WHERE rm.recipe_id = p.recipe_id
);

COMMIT TRANSACTION;
