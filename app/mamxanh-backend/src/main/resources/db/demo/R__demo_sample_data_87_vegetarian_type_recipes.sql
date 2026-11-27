-- Four local-only published recipes to demonstrate the four supported vegetarian types.
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
DECLARE @gram INT;

SELECT @author_id = user_id
FROM [USER]
WHERE email = 'demo-expert@mamxanh.local'
  AND role = 'EXPERT'
  AND account_status = 'ACTIVE';

SELECT @gram = unit_id
FROM [UNIT]
WHERE code = N'g' AND dimension = 'MASS' AND is_active = 1;

IF @author_id IS NULL
    THROW 51087, 'Active demo Expert demo-expert@mamxanh.local is required.', 1;
IF @gram IS NULL
    THROW 51088, 'The active standardized gram unit is required.', 1;

DECLARE @recipes TABLE (
    title NVARCHAR(120) PRIMARY KEY,
    description NVARCHAR(2000) NOT NULL,
    instructions NVARCHAR(MAX) NOT NULL,
    category VARCHAR(20) NOT NULL,
    vegetarian_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    servings INT NOT NULL,
    prep INT NOT NULL,
    cook INT NOT NULL,
    image_url VARCHAR(2048) NOT NULL
);

INSERT INTO @recipes VALUES
 (N'Đậu hũ sốt cà chua nấm', N'Đậu hũ và nấm mỡ om trong sốt cà chua, dùng cùng cơm nóng.', N'Cắt đậu hũ thành miếng vừa ăn, thái nấm và cà chua. Làm nóng dầu, xào hành tây đến thơm rồi thêm nấm và cà chua. Khi cà chua mềm, thêm nước tương và một ít nước, om vài phút. Cho đậu hũ vào đảo nhẹ, nêm vừa ăn và đun đến khi sốt phủ đều.', 'BRAISED', 'VEGAN', 'EASY', 2, 15, 20, '/assets/95427.png'),
 (N'Khoai tây áp chảo phô mai', N'Khoai tây vàng cạnh phủ phô mai mozzarella tan nhẹ.', N'Thái khoai tây thành lát mỏng và hành tây thành sợi. Áp chảo khoai với dầu ở lửa vừa đến khi mềm và vàng hai mặt. Thêm hành tây, nêm muối và tiêu. Rắc phô mai mozzarella, đậy nắp đến khi phô mai tan rồi dùng nóng.', 'FRIED', 'LACTO', 'EASY', 2, 12, 20, '/assets/40053.png'),
 (N'Cơm gạo lứt chiên trứng rau củ', N'Cơm gạo lứt rang tơi với trứng, cà rốt và đậu Hà Lan.', N'Nấu gạo lứt chín, để nguội và tơi hạt. Thái nhỏ cà rốt, xào cùng đậu Hà Lan và hành tây đến mềm. Dồn rau sang một bên chảo, cho trứng đã đánh vào đảo chín rồi trộn với rau. Thêm cơm, nước tương và dầu, rang đến khi nóng đều.', 'STIR_FRY', 'OVO', 'EASY', 2, 15, 30, '/assets/cddd4.png'),
 (N'Trứng cuộn nấm phô mai', N'Trứng cuộn mềm với nấm mỡ, cà chua và phô mai mozzarella.', N'Thái nấm, cà chua và hành tây thành miếng nhỏ. Xào nấm cùng hành tây đến khi mềm, thêm cà chua và để ráo phần nước. Đánh trứng với muối, đổ vào chảo chống dính. Khi trứng se mặt, thêm phần nhân và phô mai mozzarella, cuộn lại rồi nấu đến khi trứng chín.', 'FRIED', 'LACTO_OVO', 'EASY', 2, 12, 15, '/assets/40c2c.png');

DECLARE @recipe_ingredients TABLE (
    title NVARCHAR(120),
    ingredient_name NVARCHAR(200),
    quantity DECIMAL(10,2) NOT NULL
);
INSERT INTO @recipe_ingredients VALUES
 (N'Đậu hũ sốt cà chua nấm', N'Đậu hũ', 250),
 (N'Đậu hũ sốt cà chua nấm', N'Cà chua', 180),
 (N'Đậu hũ sốt cà chua nấm', N'Nấm mỡ', 100),
 (N'Đậu hũ sốt cà chua nấm', N'Hành tây', 40),
 (N'Đậu hũ sốt cà chua nấm', N'Nước tương', 15),
 (N'Đậu hũ sốt cà chua nấm', N'Dầu ăn', 10),
 (N'Khoai tây áp chảo phô mai', N'Khoai tây', 300),
 (N'Khoai tây áp chảo phô mai', N'Phô mai mozzarella', 60),
 (N'Khoai tây áp chảo phô mai', N'Hành tây', 40),
 (N'Khoai tây áp chảo phô mai', N'Dầu ăn', 10),
 (N'Khoai tây áp chảo phô mai', N'Muối ăn', 2),
 (N'Khoai tây áp chảo phô mai', N'Tiêu đen', 1),
 (N'Cơm gạo lứt chiên trứng rau củ', N'Gạo lứt', 160),
 (N'Cơm gạo lứt chiên trứng rau củ', N'Trứng gà', 100),
 (N'Cơm gạo lứt chiên trứng rau củ', N'Cà rốt', 70),
 (N'Cơm gạo lứt chiên trứng rau củ', N'Đậu Hà Lan', 50),
 (N'Cơm gạo lứt chiên trứng rau củ', N'Hành tây', 30),
 (N'Cơm gạo lứt chiên trứng rau củ', N'Nước tương', 15),
 (N'Cơm gạo lứt chiên trứng rau củ', N'Dầu ăn', 10),
 (N'Trứng cuộn nấm phô mai', N'Trứng gà', 150),
 (N'Trứng cuộn nấm phô mai', N'Nấm mỡ', 100),
 (N'Trứng cuộn nấm phô mai', N'Phô mai mozzarella', 50),
 (N'Trứng cuộn nấm phô mai', N'Cà chua', 60),
 (N'Trứng cuộn nấm phô mai', N'Hành tây', 25),
 (N'Trứng cuộn nấm phô mai', N'Dầu ăn', 8),
 (N'Trứng cuộn nấm phô mai', N'Muối ăn', 2);

IF EXISTS (
    SELECT 1
    FROM @recipe_ingredients expected
    LEFT JOIN [INGREDIENT] ingredient
      ON ingredient.name = expected.ingredient_name
     AND ingredient.status = 'ACTIVE'
    WHERE ingredient.ingredient_id IS NULL
)
    THROW 51089, 'All demo recipe ingredients must exist in the active ingredient catalog.', 1;

INSERT INTO [RECIPE_POST] (
    author_id, title, description, instructions, dish_category, vegetarian_type,
    difficulty, servings, prep_time_min, cook_time_min, status, published_at
)
SELECT @author_id, seed.title, seed.description, seed.instructions, seed.category,
       seed.vegetarian_type, seed.difficulty, seed.servings, seed.prep, seed.cook,
       'PUBLISHED', SYSUTCDATETIME()
FROM @recipes seed
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_POST] existing
    WHERE existing.author_id = @author_id AND existing.title = seed.title
);

INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT post.recipe_id, ingredient.ingredient_id, @gram, expected.quantity
FROM @recipe_ingredients expected
JOIN [INGREDIENT] ingredient
  ON ingredient.name = expected.ingredient_name AND ingredient.status = 'ACTIVE'
JOIN [RECIPE_POST] post
  ON post.author_id = @author_id AND post.title = expected.title
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_INGREDIENT] existing
    WHERE existing.recipe_id = post.recipe_id
      AND existing.ingredient_id = ingredient.ingredient_id
);

INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT post.recipe_id, seed.image_url, 'image/png', 1, 1
FROM @recipes seed
JOIN [RECIPE_POST] post
  ON post.author_id = @author_id AND post.title = seed.title
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_MEDIA] existing WHERE existing.recipe_id = post.recipe_id
);

COMMIT TRANSACTION;
