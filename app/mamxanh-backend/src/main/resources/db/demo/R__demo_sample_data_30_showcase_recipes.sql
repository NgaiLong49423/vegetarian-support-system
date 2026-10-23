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

DECLARE @showcase_recipes TABLE (
    title NVARCHAR(120), description NVARCHAR(2000), instructions NVARCHAR(MAX),
    category VARCHAR(20), difficulty VARCHAR(10), prep INT, cook INT, image_url VARCHAR(2048));
INSERT INTO @showcase_recipes VALUES
 (N'Mì udon xào rau củ kiểu Nhật', N'Mì udon dai mềm xào nấm, cải thìa và đậu Hà Lan trong sốt tương gừng.', N'Luộc mì udon rồi để ráo. Xào thơm gừng, cho nấm và rau củ vào đảo nhanh. Thêm mì cùng nước tương, đảo đều trên lửa lớn rồi rắc hành lá.', 'STIR_FRY', 'EASY', 15, 12, 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=1200'),
 (N'Cơm chiên dứa đậu phộng', N'Cơm chiên vàng thơm với dứa chua ngọt, đậu phộng rang và rau củ.', N'Xào hành tím, thêm đậu Hà Lan và ngô hạt. Cho cơm nguội vào đảo tơi, nêm nước tương. Trộn dứa và đậu phộng rang ở cuối để giữ độ giòn.', 'STIR_FRY', 'EASY', 15, 15, 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Phở cuốn nấm bào ngư', N'Bánh phở cuốn mềm ôm nấm bào ngư áp chảo và rau sống tươi mát.', N'Xé nấm bào ngư, áp chảo với nước tương và gừng. Trải bánh phở, thêm cải thìa, giá đỗ và nấm rồi cuốn chắc tay. Dùng với nước chấm chanh ớt.', 'ROLL', 'MEDIUM', 20, 12, 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200'),
 (N'Bánh xèo nấm và giá đỗ', N'Bánh xèo vỏ mỏng giòn, nhân nấm, giá đỗ và cải bắp.', N'Pha bột gạo với nước và bột nghệ. Xào thơm nấm, đổ bột vào chảo nóng rồi thêm giá đỗ và bắp cải. Đậy nắp đến khi viền bánh giòn rồi gập đôi.', 'FRIED', 'MEDIUM', 20, 20, 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200'),
 (N'Cháo bí đỏ đậu xanh', N'Cháo bí đỏ sánh mịn nấu cùng đậu xanh bùi, thơm nhẹ gừng.', N'Nấu đậu xanh cùng gạo đến khi mềm. Thêm bí đỏ cắt khối và gừng, nấu nhỏ lửa đến khi bí mềm. Khuấy đều, nêm vừa ăn và rắc hành lá.', 'SOUP', 'EASY', 10, 35, 'https://images.unsplash.com/photo-1511690743698-d9d85f2fbf38?w=1200'),
 (N'Đậu hũ sốt nấm mỡ', N'Đậu hũ mềm phủ sốt nấm mỡ sánh nhẹ và hành lá.', N'Xào thơm hành tím, cho nấm và nước tương vào. Hòa bột năng với nước lạnh rồi khuấy vào sốt cho sánh. Áp chảo đậu hũ, rưới sốt nấm và dùng nóng.', 'BRAISED', 'EASY', 12, 12, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=1200'),
 (N'Bánh mì nấm áp chảo', N'Bánh mì nguyên cám kẹp nấm nướng, cà tím và rau xanh.', N'Ướp nấm và cà tím với nước tương, tiêu và ít dầu. Áp chảo đến khi vàng thơm. Nướng nóng bánh mì, kẹp cùng cải thìa và vắt chút chanh trước khi ăn.', 'GRILLED', 'EASY', 12, 12, 'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200'),
 (N'Bún chả giò chay rau thơm', N'Tô bún tươi ăn cùng chả giò chay giòn và rau xanh mát.', N'Chiên hoặc nướng chả giò đến khi vàng giòn. Xếp bánh phở, cải thìa, giá đỗ và rau thơm vào tô. Thêm chả giò cắt khúc, rắc đậu phộng và chan nước chấm chanh.', 'NOODLE_SOUP', 'MEDIUM', 20, 15, 'https://images.unsplash.com/photo-1555126634-323283e090fa?w=1200'),
 (N'Lẩu nấm cải thảo', N'Nồi lẩu thanh ngọt với nấm kim châm, nấm bào ngư và cải thảo.', N'Đun sôi nước dùng rau củ với gừng và riềng. Nêm vừa ăn, lần lượt cho cải thảo, nấm và đậu hũ vào nồi. Dùng nóng cùng bánh phở.', 'HOT_POT', 'EASY', 15, 25, 'https://images.unsplash.com/photo-1570368336224-455084c1fb31?w=1200'),
 (N'Chả giò khoai môn đậu xanh', N'Chả giò vàng giòn với nhân khoai môn, đậu xanh và nấm.', N'Ngâm mềm đậu xanh và nấm. Trộn với khoai môn bào và bắp cải thái sợi. Cuốn nhân trong bánh tráng, áp chảo hoặc chiên ít dầu đến khi vàng đều.', 'FRIED', 'MEDIUM', 25, 20, 'https://images.unsplash.com/photo-1589442305595-62647c1514f9?w=1200'),
 (N'Cơm nắm rong biển mè trắng', N'Cơm nắm nhỏ gọn trộn rong biển, mè trắng và đậu Hà Lan.', N'Nấu chín đậu Hà Lan. Trộn cơm nóng với rong biển vụn, mè rang và chút nước tương. Nắm thành viên nhỏ khi cơm còn ấm, dùng kèm dưa leo.', 'STEAMED', 'EASY', 15, 20, 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=1200'),
 (N'Canh chua nấm đậu bắp', N'Canh chua thanh nhẹ nấu nấm, đậu bắp và me chua.', N'Đun nước với me chua rồi lọc bỏ hạt. Thêm cà chua, nấm và đậu bắp, nấu vừa chín tới. Nêm cân vị, rắc ngò rí trước khi dùng.', 'SOUP', 'EASY', 12, 18, 'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=1200'),
 (N'Mì Ý sốt cà tím nấm', N'Mì Ý phủ sốt cà tím, nấm và cà chua nấu thơm.', N'Luộc mì Ý đến độ dai vừa. Áp chảo cà tím và nấm, thêm cà chua cùng ít nước luộc mì rồi nấu thành sốt. Trộn mì vào chảo, nêm tiêu và nước tương.', 'STIR_FRY', 'MEDIUM', 15, 20, 'https://images.unsplash.com/photo-1758293121435-396ed31ebcf4?w=1200'),
 (N'Pudding hạt chia xoài', N'Món tráng miệng mát lạnh với hạt chia, xoài chín và sữa đậu nành.', N'Khuấy hạt chia vào sữa đậu nành, để lạnh ít nhất 3 giờ cho nở. Cắt xoài thành hạt lựu, xếp lên pudding trước khi dùng.', 'DESSERT', 'EASY', 10, 0, 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=1200'),
 (N'Rau ngót nấu nấm kim châm', N'Canh rau ngót xanh mềm nấu cùng nấm kim châm và đậu hũ.', N'Vò nhẹ rau ngót, cắt bỏ gốc nấm. Đun nước sôi, cho đậu hũ vào trước, thêm rau ngót và nấm. Nấu vừa chín, nêm nước tương và tiêu.', 'SOUP', 'EASY', 10, 12, 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Bí đỏ nướng mè trắng', N'Bí đỏ nướng mềm ngọt phủ mè trắng rang và sốt chanh gừng.', N'Cắt bí đỏ thành miếng vừa ăn, trộn với gừng băm và chút dầu. Nướng đến khi mềm cạnh vàng. Rắc mè rang, thêm nước cốt chanh và dùng nóng.', 'GRILLED', 'EASY', 10, 25, 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200');

INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, published_at)
SELECT @author_id, r.title, r.description, r.instructions, r.category, 'VEGAN', r.difficulty, 2, r.prep, r.cook, SYSUTCDATETIME()
FROM @showcase_recipes r
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_POST] p WHERE p.author_id = @author_id AND p.title = r.title);

DECLARE @showcase_recipe_ingredients TABLE (title NVARCHAR(120), ingredient_name NVARCHAR(200), quantity DECIMAL(10,2));
INSERT INTO @showcase_recipe_ingredients VALUES
 (N'Mì udon xào rau củ kiểu Nhật', N'Mì udon', 200), (N'Mì udon xào rau củ kiểu Nhật', N'Nấm kim châm', 100), (N'Mì udon xào rau củ kiểu Nhật', N'Rau cải thìa', 120), (N'Mì udon xào rau củ kiểu Nhật', N'Đậu Hà Lan', 60),
 (N'Cơm chiên dứa đậu phộng', N'Dứa', 120), (N'Cơm chiên dứa đậu phộng', N'Đậu phộng', 30), (N'Cơm chiên dứa đậu phộng', N'Đậu Hà Lan', 60), (N'Cơm chiên dứa đậu phộng', N'Ngô hạt', 60),
 (N'Phở cuốn nấm bào ngư', N'Bánh phở', 200), (N'Phở cuốn nấm bào ngư', N'Nấm bào ngư', 150), (N'Phở cuốn nấm bào ngư', N'Giá đỗ', 80), (N'Phở cuốn nấm bào ngư', N'Rau cải thìa', 80),
 (N'Bánh xèo nấm và giá đỗ', N'Bột gạo', 150), (N'Bánh xèo nấm và giá đỗ', N'Bột nghệ', 3), (N'Bánh xèo nấm và giá đỗ', N'Nấm mỡ', 100), (N'Bánh xèo nấm và giá đỗ', N'Giá đỗ', 80),
 (N'Cháo bí đỏ đậu xanh', N'Bí đỏ', 200), (N'Cháo bí đỏ đậu xanh', N'Đậu xanh', 80), (N'Cháo bí đỏ đậu xanh', N'Gừng', 5), (N'Cháo bí đỏ đậu xanh', N'Hành lá', 10),
 (N'Đậu hũ sốt nấm mỡ', N'Đậu hũ', 200), (N'Đậu hũ sốt nấm mỡ', N'Nấm mỡ', 120), (N'Đậu hũ sốt nấm mỡ', N'Bột năng', 8), (N'Đậu hũ sốt nấm mỡ', N'Hành tím', 15),
 (N'Bánh mì nấm áp chảo', N'Bánh mì nguyên cám', 120), (N'Bánh mì nấm áp chảo', N'Nấm kim châm', 100), (N'Bánh mì nấm áp chảo', N'Cà tím', 100), (N'Bánh mì nấm áp chảo', N'Chanh', 10),
 (N'Bún chả giò chay rau thơm', N'Bánh phở', 200), (N'Bún chả giò chay rau thơm', N'Chả giò chay', 150), (N'Bún chả giò chay rau thơm', N'Rau cải thìa', 80), (N'Bún chả giò chay rau thơm', N'Đậu phộng', 20),
 (N'Lẩu nấm cải thảo', N'Nấm kim châm', 100), (N'Lẩu nấm cải thảo', N'Nấm bào ngư', 150), (N'Lẩu nấm cải thảo', N'Cải thảo', 150), (N'Lẩu nấm cải thảo', N'Riềng', 10),
 (N'Chả giò khoai môn đậu xanh', N'Khoai môn', 150), (N'Chả giò khoai môn đậu xanh', N'Đậu xanh', 60), (N'Chả giò khoai môn đậu xanh', N'Bắp cải', 80), (N'Chả giò khoai môn đậu xanh', N'Bột gạo', 30),
 (N'Cơm nắm rong biển mè trắng', N'Rong biển', 5), (N'Cơm nắm rong biển mè trắng', N'Mè trắng', 10), (N'Cơm nắm rong biển mè trắng', N'Đậu Hà Lan', 50), (N'Cơm nắm rong biển mè trắng', N'Nước tương', 10),
 (N'Canh chua nấm đậu bắp', N'Đậu bắp', 100), (N'Canh chua nấm đậu bắp', N'Me chua', 15), (N'Canh chua nấm đậu bắp', N'Nấm mỡ', 100), (N'Canh chua nấm đậu bắp', N'Ngò rí', 10),
 (N'Mì Ý sốt cà tím nấm', N'Mì Ý', 180), (N'Mì Ý sốt cà tím nấm', N'Cà tím', 120), (N'Mì Ý sốt cà tím nấm', N'Nấm mỡ', 100), (N'Mì Ý sốt cà tím nấm', N'Cà chua', 150),
 (N'Pudding hạt chia xoài', N'Hạt chia', 25), (N'Pudding hạt chia xoài', N'Xoài', 150), (N'Pudding hạt chia xoài', N'Sữa đậu nành', 200), (N'Pudding hạt chia xoài', N'Chanh', 5),
 (N'Rau ngót nấu nấm kim châm', N'Rau ngót', 150), (N'Rau ngót nấu nấm kim châm', N'Nấm kim châm', 100), (N'Rau ngót nấu nấm kim châm', N'Đậu hũ', 100), (N'Rau ngót nấu nấm kim châm', N'Nước tương', 10),
 (N'Bí đỏ nướng mè trắng', N'Bí đỏ', 250), (N'Bí đỏ nướng mè trắng', N'Mè trắng', 10), (N'Bí đỏ nướng mè trắng', N'Gừng', 5), (N'Bí đỏ nướng mè trắng', N'Chanh', 10);
INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT p.recipe_id, i.ingredient_id, @gram, seed.quantity
FROM @showcase_recipe_ingredients seed
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
JOIN [INGREDIENT] i ON i.name = seed.ingredient_name
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri WHERE ri.recipe_id = p.recipe_id);

-- Add a cover image to every pre-existing and newly added demo recipe.
INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT p.recipe_id, seed.image_url, 'image/jpeg', 1, 1
FROM (VALUES
    (N'Đậu hũ sốt cà chua', 'https://images.unsplash.com/photo-1589442305595-62647c1514f9?w=1200'),
    (N'Rau muống xào tỏi', 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=1200'),
    (N'Nấm đùi gà kho tiêu', 'https://images.unsplash.com/photo-1555126634-323283e090fa?w=1200'),
    (N'Canh nấm hương cà rốt', 'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=1200'),
    (N'Khoai lang hấp', 'https://images.unsplash.com/photo-1570368336224-455084c1fb31?w=1200'),
    (N'Bông cải xanh xào nấm', 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=1200'),
    (N'Súp đậu lăng rau củ', 'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200')
) seed(title, image_url)
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_MEDIA] m WHERE m.recipe_id = p.recipe_id);

INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT p.recipe_id, seed.image_url, 'image/jpeg', 1, 1
FROM @showcase_recipes seed
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_MEDIA] m WHERE m.recipe_id = p.recipe_id);

COMMIT TRANSACTION;
