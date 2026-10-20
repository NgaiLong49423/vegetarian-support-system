-- Additional local-only recipe fixtures using the expanded nutrition catalog.
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

DECLARE @recipes TABLE (
    title NVARCHAR(120) PRIMARY KEY, description NVARCHAR(2000), instructions NVARCHAR(MAX),
    category VARCHAR(20), vegetarian_type VARCHAR(20), difficulty VARCHAR(10),
    prep INT, cook INT, image_url VARCHAR(2048)
);
INSERT INTO @recipes VALUES
 (N'Cơm gạo lứt đậu nành non', N'Cơm gạo lứt dẻo bùi dùng cùng đậu nành non và rau xanh.', N'Nấu chín gạo lứt. Chần đậu nành non, áp chảo nhanh với hành tây và nước tương. Xếp cùng cải bó xôi đã chần và dưa leo cắt lát.', 'STIR_FRY', 'VEGAN', 'EASY', 15, 30, '/assets/09723.png'),
 (N'Súp đậu trắng bí đỏ', N'Súp bí đỏ sánh nhẹ với đậu trắng và cà chua.', N'Nấu đậu trắng đã ngâm đến mềm. Thêm bí đỏ, cà chua và hành tây, nấu đến khi bí mềm rồi nghiền nhẹ một phần để tạo độ sánh.', 'SOUP', 'VEGAN', 'EASY', 15, 35, '/assets/40053.png'),
 (N'Mì soba nấm mộc nhĩ sốt miso', N'Mì soba trộn nấm mộc nhĩ và sốt miso gừng.', N'Luộc mì soba theo hướng dẫn rồi để ráo. Xào nấm mộc nhĩ với gừng, hòa miso cùng ít nước nóng, trộn sốt với mì và rắc hành lá.', 'STIR_FRY', 'VEGAN', 'MEDIUM', 15, 15, '/assets/40c2c.png'),
 (N'Đậu phụ non sốt miso nấm', N'Đậu phụ non mềm dùng cùng sốt miso và nấm hương.', N'Áp chảo nhẹ đậu phụ non. Đun miso với nấm hương, gừng và một ít nước đến khi thành sốt sánh; rưới lên đậu và dùng nóng.', 'BRAISED', 'VEGAN', 'EASY', 10, 15, '/assets/5a53c.png'),
 (N'Salad cải bó xôi bơ lựu', N'Cải bó xôi non trộn bơ, lựu và óc chó.', N'Rửa sạch cải bó xôi và để ráo. Cắt bơ, tách hạt lựu, trộn nhẹ cùng óc chó đập dập và nước cốt chanh ngay trước khi dùng.', 'SALAD', 'VEGAN', 'EASY', 15, 5, '/assets/1a85b.png'),
 (N'Bánh mì kẹp seitan rau thơm', N'Bánh mì nguyên cám kẹp seitan áp chảo, rau romaine và húng quế.', N'Cắt seitan thành lát và áp chảo cùng nước tương. Làm nóng bánh mì, kẹp seitan với romaine, dưa leo, cà chua và húng quế.', 'ROLL', 'VEGAN', 'EASY', 12, 10, '/assets/95427.png'),
 (N'Khoai sọ nướng mè đen', N'Khoai sọ nướng mềm phủ mè đen rang và gừng.', N'Cắt khoai sọ thành miếng vừa ăn, hấp sơ rồi nướng đến khi vàng cạnh. Rang mè đen, trộn với gừng băm và rắc lên khoai trước khi dùng.', 'GRILLED', 'VEGAN', 'EASY', 12, 25, '/assets/8ef48.png'),
 (N'Bánh yến mạch trứng chuối', N'Bánh áp chảo mềm từ bột yến mạch, trứng gà và chuối.', N'Nghiền chuối, trộn với trứng và bột yến mạch. Để hỗn hợp nghỉ vài phút rồi áp chảo từng bánh nhỏ ở lửa vừa đến khi chín hai mặt; dùng cùng dâu tây.', 'FRIED', 'OVO', 'EASY', 10, 12, '/assets/62df0.png'),
 (N'Chè kê vàng chà là dừa', N'Chè kê nấu mềm cùng chà là và dừa nạo.', N'Vo kê vàng rồi nấu với nước đến khi hạt mềm. Thêm chà là cắt nhỏ và dừa nạo, đun thêm vài phút; dùng ấm hoặc để nguội.', 'DESSERT', 'VEGAN', 'EASY', 10, 25, '/assets/e5dc6.png'),
 (N'Khổ qua nhồi đậu phụ non', N'Khổ qua nhồi nhân đậu phụ non, nấm mộc nhĩ và cà rốt.', N'Băm đậu phụ non cùng nấm mộc nhĩ, cà rốt và gừng. Nhồi nhân vào khổ qua đã bỏ ruột, xếp vào nồi với nước dùng rau củ và nấu đến khi mềm.', 'SOUP', 'VEGAN', 'MEDIUM', 20, 30, '/assets/40053.png');

INSERT INTO [RECIPE_POST] (
    author_id, title, description, instructions, dish_category, vegetarian_type,
    difficulty, servings, prep_time_min, cook_time_min, published_at
)
SELECT @author_id, seed.title, seed.description, seed.instructions, seed.category,
    seed.vegetarian_type, seed.difficulty, 2, seed.prep, seed.cook, SYSUTCDATETIME()
FROM @recipes seed
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_POST] existing
    WHERE existing.author_id = @author_id AND existing.title = seed.title
);

DECLARE @recipe_ingredients TABLE (
    title NVARCHAR(120), ingredient_name NVARCHAR(200), quantity DECIMAL(10,2)
);
INSERT INTO @recipe_ingredients VALUES
 (N'Cơm gạo lứt đậu nành non', N'Gạo lứt', 160), (N'Cơm gạo lứt đậu nành non', N'Đậu nành non', 100),
 (N'Cơm gạo lứt đậu nành non', N'Cải bó xôi', 80), (N'Cơm gạo lứt đậu nành non', N'Dưa leo', 60),
 (N'Cơm gạo lứt đậu nành non', N'Hành tây', 20), (N'Súp đậu trắng bí đỏ', N'Đậu trắng', 80),
 (N'Súp đậu trắng bí đỏ', N'Bí đỏ', 180), (N'Súp đậu trắng bí đỏ', N'Cà chua', 80),
 (N'Súp đậu trắng bí đỏ', N'Hành tây', 30), (N'Mì soba nấm mộc nhĩ sốt miso', N'Mì soba', 160),
 (N'Mì soba nấm mộc nhĩ sốt miso', N'Nấm mộc nhĩ', 20), (N'Mì soba nấm mộc nhĩ sốt miso', N'Tương miso', 15),
 (N'Mì soba nấm mộc nhĩ sốt miso', N'Gừng', 5), (N'Mì soba nấm mộc nhĩ sốt miso', N'Hành lá', 10),
 (N'Đậu phụ non sốt miso nấm', N'Đậu phụ non', 220), (N'Đậu phụ non sốt miso nấm', N'Tương miso', 15),
 (N'Đậu phụ non sốt miso nấm', N'Nấm hương', 40), (N'Đậu phụ non sốt miso nấm', N'Gừng', 5),
 (N'Salad cải bó xôi bơ lựu', N'Cải bó xôi', 100), (N'Salad cải bó xôi bơ lựu', N'Bơ', 80),
 (N'Salad cải bó xôi bơ lựu', N'Lựu', 50), (N'Salad cải bó xôi bơ lựu', N'Óc chó', 15),
 (N'Salad cải bó xôi bơ lựu', N'Chanh', 10), (N'Bánh mì kẹp seitan rau thơm', N'Bánh mì nguyên cám', 100),
 (N'Bánh mì kẹp seitan rau thơm', N'Seitan', 100), (N'Bánh mì kẹp seitan rau thơm', N'Xà lách romaine', 40),
 (N'Bánh mì kẹp seitan rau thơm', N'Dưa leo', 40), (N'Bánh mì kẹp seitan rau thơm', N'Cà chua', 40),
 (N'Bánh mì kẹp seitan rau thơm', N'Húng quế', 5), (N'Khoai sọ nướng mè đen', N'Khoai sọ', 220),
 (N'Khoai sọ nướng mè đen', N'Mè đen', 8), (N'Khoai sọ nướng mè đen', N'Gừng', 5),
 (N'Bánh yến mạch trứng chuối', N'Bột yến mạch', 60), (N'Bánh yến mạch trứng chuối', N'Trứng gà', 100),
 (N'Bánh yến mạch trứng chuối', N'Chuối', 80), (N'Bánh yến mạch trứng chuối', N'Dâu tây', 50),
 (N'Chè kê vàng chà là dừa', N'Kê vàng', 60), (N'Chè kê vàng chà là dừa', N'Chà là', 25),
 (N'Chè kê vàng chà là dừa', N'Dừa nạo', 20), (N'Khổ qua nhồi đậu phụ non', N'Khổ qua', 200),
 (N'Khổ qua nhồi đậu phụ non', N'Đậu phụ non', 120), (N'Khổ qua nhồi đậu phụ non', N'Nấm mộc nhĩ', 15),
 (N'Khổ qua nhồi đậu phụ non', N'Cà rốt', 40), (N'Khổ qua nhồi đậu phụ non', N'Gừng', 5);

INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT post.recipe_id, ingredient.ingredient_id, @gram, seed.quantity
FROM @recipe_ingredients seed
JOIN [RECIPE_POST] post ON post.author_id = @author_id AND post.title = seed.title
JOIN [INGREDIENT] ingredient ON ingredient.name = seed.ingredient_name
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_INGREDIENT] existing
    WHERE existing.recipe_id = post.recipe_id AND existing.ingredient_id = ingredient.ingredient_id
);

INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT post.recipe_id, seed.image_url, 'image/png', 1, 1
FROM @recipes seed
JOIN [RECIPE_POST] post ON post.author_id = @author_id AND post.title = seed.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_MEDIA] media WHERE media.recipe_id = post.recipe_id);

COMMIT TRANSACTION;
