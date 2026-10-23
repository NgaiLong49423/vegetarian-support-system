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

DECLARE @extra_recipes TABLE (
    title NVARCHAR(120) PRIMARY KEY, description NVARCHAR(2000), instructions NVARCHAR(MAX),
    category VARCHAR(20), difficulty VARCHAR(10), prep INT, cook INT, image_url VARCHAR(2048));
INSERT INTO @extra_recipes VALUES
 (N'Cơm gạo lứt đậu gà rau củ',N'Cơm gạo lứt ăn cùng đậu gà và rau củ áp chảo.',N'Nấu chín gạo lứt và đậu gà đã ngâm mềm. Áp chảo ớt chuông cùng bí ngòi, nêm nước tương. Xếp cơm, đậu và rau củ ra đĩa, rắc hạt hướng dương.','STIR_FRY','EASY',15,30,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Cháo yến mạch nấm mộc nhĩ',N'Cháo yến mạch nóng với nấm mộc nhĩ và cà rốt.',N'Ngâm nấm mộc nhĩ rồi thái sợi. Đun yến mạch với nước đến khi sánh, thêm nấm và cà rốt thái nhỏ. Nấu chín mềm, nêm vừa ăn và dùng nóng.','SOUP','EASY',10,15,'https://images.unsplash.com/photo-1511690743698-d9d85f2fbf38?w=1200'),
 (N'Salad quinoa bơ lựu',N'Salad quinoa trộn bơ, lựu và xà lách romaine.',N'Nấu chín quinoa rồi để nguội. Cắt bơ và dưa leo, tách hạt lựu. Trộn cùng xà lách romaine, nước cốt cam và chút muối ngay trước khi dùng.','SALAD','EASY',15,20,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Đậu hũ áp chảo sốt hạt điều',N'Đậu hũ vàng nhẹ phủ sốt hạt điều béo mịn.',N'Ngâm hạt điều rồi xay với nước thành sốt. Áp chảo đậu hũ đến vàng các mặt, đun sốt với tỏi và nước tương. Rưới sốt lên đậu hũ và rắc hạnh nhân lát.','BRAISED','MEDIUM',15,15,'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=1200'),
 (N'Bún tempeh rau sống',N'Bánh phở ăn kèm tempeh áp chảo và rau sống tươi.',N'Cắt tempeh thành lát, ướp nước tương rồi áp chảo vàng. Xếp bánh phở, xà lách romaine, dưa leo và tempeh vào tô. Rắc đậu phộng và dùng cùng nước chấm chanh.','NOODLE_SOUP','EASY',15,12,'https://images.unsplash.com/photo-1555126634-323283e090fa?w=1200'),
 (N'Khoai tây nướng hạt bí',N'Khoai tây nướng thơm với hạt bí rang.',N'Cắt khoai tây thành miếng vừa, trộn với dầu và tiêu. Nướng đến khi mềm ruột và vàng cạnh. Rang hạt bí riêng rồi rắc lên khoai trước khi dùng.','GRILLED','EASY',10,30,'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200'),
 (N'Súp đậu trắng cải xoăn',N'Súp đậu trắng nấu cùng cải xoăn và cà chua.',N'Ngâm và nấu mềm đậu trắng. Thêm cà chua cùng nước dùng rau củ, nấu liu riu. Cho cải xoăn vào cuối, nấu vừa mềm rồi nêm gia vị.','SOUP','MEDIUM',15,35,'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200'),
 (N'Bánh quinoa chuối dâu tây',N'Bánh áp chảo từ quinoa, chuối và dâu tây.',N'Nghiền chuối, trộn với quinoa đã nấu và yến mạch cán. Áp chảo từng phần nhỏ đến khi hai mặt vàng. Dùng cùng dâu tây cắt lát.','FRIED','MEDIUM',15,15,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200'),
 (N'Ớt chuông nhồi gạo lứt',N'Ớt chuông nướng với nhân gạo lứt và đậu đỏ.',N'Nấu chín gạo lứt và đậu đỏ. Trộn cùng cà chua thái hạt lựu, cho vào ớt chuông bỏ hạt. Nướng đến khi ớt mềm và phần nhân nóng đều.','GRILLED','MEDIUM',20,30,'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200'),
 (N'Mì seitan xào măng tây',N'Mì xào seitan và măng tây giòn nhẹ.',N'Luộc mì vừa chín. Cắt seitan và măng tây thành miếng, xào nhanh với tỏi. Thêm mì, nước tương và chút nước luộc mì, đảo đều rồi dùng nóng.','STIR_FRY','MEDIUM',15,15,'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=1200'),
 (N'Canh kombu đậu hũ',N'Canh thanh vị nấu từ kombu, đậu hũ và nấm.',N'Ngâm kombu trong nước lạnh rồi đun nhỏ lửa để lấy nước dùng, vớt kombu ra. Thêm nấm và đậu hũ, nấu sôi nhẹ, nêm nước tương trước khi dùng.','SOUP','EASY',10,20,'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=1200'),
 (N'Bí ngòi cuộn hạt điều',N'Bí ngòi cuộn sốt hạt điều và cà chua.',N'Thái bí ngòi thành lát mỏng và áp chảo nhanh. Xay hạt điều đã ngâm cùng nước và tỏi thành sốt. Phết sốt lên lát bí, cuộn lại và dùng với cà chua.','ROLL','MEDIUM',20,10,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200'),
 (N'Súp lơ trắng nghiền hạnh nhân',N'Súp lơ trắng hấp mềm nghiền cùng hạnh nhân.',N'Hấp súp lơ trắng đến khi mềm. Xay hoặc nghiền cùng hạnh nhân ngâm và nước ấm đến độ mịn mong muốn. Nêm tiêu, dùng nóng với hạt bí rang.','STEAMED','EASY',10,15,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Đậu đỏ hầm cà chua',N'Đậu đỏ hầm mềm với cà chua và khoai tây.',N'Ngâm đậu đỏ qua đêm rồi nấu mềm. Thêm khoai tây và cà chua cắt miếng, hầm đến khi rau củ chín. Nêm gia vị và rắc hành lá.','BRAISED','MEDIUM',15,45,'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200'),
 (N'Yến mạch qua đêm táo',N'Yến mạch ngâm lạnh với táo và hạnh nhân.',N'Trộn yến mạch với sữa đậu nành, đậy kín và để lạnh qua đêm. Cắt táo nhỏ, rắc hạnh nhân lên trên ngay trước khi ăn.','DESSERT','EASY',10,5,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200'),
 (N'Gỏi romaine dưa leo cam',N'Gỏi rau giòn với dưa leo và cam tươi.',N'Rửa và xé xà lách romaine. Cắt dưa leo, tách múi cam rồi trộn nhẹ. Rắc hạt hướng dương và dùng ngay.', 'SALAD','EASY',15,5,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Đậu nành hạt sốt cà tím',N'Đậu nành hạt nấu mềm trong sốt cà tím.',N'Ngâm và nấu mềm đậu nành hạt. Áp chảo cà tím với tỏi, thêm cà chua và đun thành sốt. Trộn đậu vào, nấu thêm vài phút rồi nêm vừa ăn.','BRAISED','MEDIUM',15,35,'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=1200'),
 (N'Bánh khoai lang hạt óc chó',N'Bánh khoai lang nghiền trộn óc chó và yến mạch.',N'Hấp khoai lang rồi nghiền nhuyễn. Trộn với yến mạch và óc chó băm, tạo miếng dẹt. Áp chảo lửa nhỏ đến khi hai mặt vàng và bánh nóng đều.','FRIED','MEDIUM',15,20,'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200'),
 (N'Đậu gà nghiền bánh mì',N'Bánh mì nguyên cám kẹp đậu gà nghiền và rau xanh.',N'Nghiền đậu gà đã nấu chín với nước cốt chanh và tiêu. Phết lên bánh mì, thêm xà lách romaine, dưa leo và cà chua. Dùng ngay sau khi kẹp.','ROLL','EASY',15,5,'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200'),
 (N'Salad trái cây hạt chia',N'Trái cây tươi trộn hạt chia và hạnh nhân.',N'Cắt chuối, táo, dâu tây và cam thành miếng vừa ăn. Trộn nhẹ với hạt chia đã ngâm nở, rắc hạnh nhân lát và dùng lạnh.','DESSERT','EASY',15,5,'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=1200'),
 (N'Canh rau dền mướp hương nấm rơm',N'Canh rau dền nấu cùng mướp hương, nấm rơm, hạt sen và bí đao.',N'Ngâm hạt sen đến mềm. Đun hạt sen và bí đao trong nước đến khi gần chín, thêm mướp hương và nấm rơm. Cho rau dền vào sau cùng, nấu vừa mềm rồi nêm nhẹ và dùng nóng.','SOUP','EASY',15,25,'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=1200');

INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, published_at)
SELECT @author_id, seed.title, seed.description, seed.instructions, seed.category, 'VEGAN', seed.difficulty, 2, seed.prep, seed.cook, SYSUTCDATETIME()
FROM @extra_recipes seed
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_POST] p WHERE p.author_id = @author_id AND p.title = seed.title);

DECLARE @extra_recipe_ingredients TABLE (title NVARCHAR(120), ingredient_name NVARCHAR(200), quantity DECIMAL(10,2));
INSERT INTO @extra_recipe_ingredients VALUES
 (N'Cơm gạo lứt đậu gà rau củ',N'Gạo lứt',180),(N'Cơm gạo lứt đậu gà rau củ',N'Đậu gà',100),(N'Cơm gạo lứt đậu gà rau củ',N'Ớt chuông',80),(N'Cơm gạo lứt đậu gà rau củ',N'Bí ngòi',80),(N'Cơm gạo lứt đậu gà rau củ',N'Hạt hướng dương',10),
 (N'Cháo yến mạch nấm mộc nhĩ',N'Yến mạch cán',80),(N'Cháo yến mạch nấm mộc nhĩ',N'Nấm mộc nhĩ',25),(N'Cháo yến mạch nấm mộc nhĩ',N'Cà rốt',60),
 (N'Salad quinoa bơ lựu',N'Hạt quinoa',100),(N'Salad quinoa bơ lựu',N'Bơ',80),(N'Salad quinoa bơ lựu',N'Lựu',50),(N'Salad quinoa bơ lựu',N'Xà lách romaine',80),(N'Salad quinoa bơ lựu',N'Dưa leo',50),(N'Salad quinoa bơ lựu',N'Cam',20),
 (N'Đậu hũ áp chảo sốt hạt điều',N'Đậu hũ',200),(N'Đậu hũ áp chảo sốt hạt điều',N'Hạt điều',35),(N'Đậu hũ áp chảo sốt hạt điều',N'Hạnh nhân',10),
 (N'Bún tempeh rau sống',N'Tempeh',150),(N'Bún tempeh rau sống',N'Bánh phở',150),(N'Bún tempeh rau sống',N'Dưa leo',80),(N'Bún tempeh rau sống',N'Xà lách romaine',70),(N'Bún tempeh rau sống',N'Đậu phộng',15),
 (N'Khoai tây nướng hạt bí',N'Khoai tây',250),(N'Khoai tây nướng hạt bí',N'Hạt bí',15),
 (N'Súp đậu trắng cải xoăn',N'Đậu trắng',100),(N'Súp đậu trắng cải xoăn',N'Cải xoăn',80),(N'Súp đậu trắng cải xoăn',N'Cà chua',100),
 (N'Bánh quinoa chuối dâu tây',N'Hạt quinoa',80),(N'Bánh quinoa chuối dâu tây',N'Chuối',100),(N'Bánh quinoa chuối dâu tây',N'Dâu tây',80),(N'Bánh quinoa chuối dâu tây',N'Yến mạch cán',40),
 (N'Ớt chuông nhồi gạo lứt',N'Ớt chuông',160),(N'Ớt chuông nhồi gạo lứt',N'Gạo lứt',100),(N'Ớt chuông nhồi gạo lứt',N'Đậu đỏ',80),(N'Ớt chuông nhồi gạo lứt',N'Cà chua',60),
 (N'Mì seitan xào măng tây',N'Seitan',120),(N'Mì seitan xào măng tây',N'Măng tây',100),(N'Mì seitan xào măng tây',N'Mì Ý',120),
 (N'Canh kombu đậu hũ',N'Tảo bẹ kombu',5),(N'Canh kombu đậu hũ',N'Đậu hũ',120),(N'Canh kombu đậu hũ',N'Nấm hương',20),
 (N'Bí ngòi cuộn hạt điều',N'Bí ngòi',160),(N'Bí ngòi cuộn hạt điều',N'Hạt điều',30),(N'Bí ngòi cuộn hạt điều',N'Cà chua',80),
 (N'Súp lơ trắng nghiền hạnh nhân',N'Súp lơ trắng',200),(N'Súp lơ trắng nghiền hạnh nhân',N'Hạnh nhân',20),(N'Súp lơ trắng nghiền hạnh nhân',N'Hạt bí',10),
 (N'Đậu đỏ hầm cà chua',N'Đậu đỏ',100),(N'Đậu đỏ hầm cà chua',N'Khoai tây',100),(N'Đậu đỏ hầm cà chua',N'Cà chua',120),
 (N'Yến mạch qua đêm táo',N'Yến mạch cán',60),(N'Yến mạch qua đêm táo',N'Táo',100),(N'Yến mạch qua đêm táo',N'Hạnh nhân',10),(N'Yến mạch qua đêm táo',N'Sữa đậu nành',150),
 (N'Gỏi romaine dưa leo cam',N'Xà lách romaine',100),(N'Gỏi romaine dưa leo cam',N'Dưa leo',80),(N'Gỏi romaine dưa leo cam',N'Cam',80),(N'Gỏi romaine dưa leo cam',N'Hạt hướng dương',10),
 (N'Đậu nành hạt sốt cà tím',N'Đậu nành hạt',100),(N'Đậu nành hạt sốt cà tím',N'Cà tím',120),(N'Đậu nành hạt sốt cà tím',N'Cà chua',100),
 (N'Bánh khoai lang hạt óc chó',N'Khoai lang',180),(N'Bánh khoai lang hạt óc chó',N'Óc chó',20),(N'Bánh khoai lang hạt óc chó',N'Yến mạch cán',30),
 (N'Đậu gà nghiền bánh mì',N'Đậu gà',100),(N'Đậu gà nghiền bánh mì',N'Bánh mì nguyên cám',100),(N'Đậu gà nghiền bánh mì',N'Xà lách romaine',50),(N'Đậu gà nghiền bánh mì',N'Dưa leo',50),(N'Đậu gà nghiền bánh mì',N'Cà chua',30),
 (N'Salad trái cây hạt chia',N'Chuối',60),(N'Salad trái cây hạt chia',N'Táo',60),(N'Salad trái cây hạt chia',N'Dâu tây',60),(N'Salad trái cây hạt chia',N'Cam',60),(N'Salad trái cây hạt chia',N'Hạt chia',10),(N'Salad trái cây hạt chia',N'Hạnh nhân',10),
 (N'Canh rau dền mướp hương nấm rơm',N'Rau dền',100),(N'Canh rau dền mướp hương nấm rơm',N'Mướp hương',120),
 (N'Canh rau dền mướp hương nấm rơm',N'Nấm rơm',100),(N'Canh rau dền mướp hương nấm rơm',N'Hạt sen',40),
 (N'Canh rau dền mướp hương nấm rơm',N'Bí đao',100);

INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT p.recipe_id, i.ingredient_id, @gram, seed.quantity
FROM @extra_recipe_ingredients seed
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
JOIN [INGREDIENT] i ON i.name = seed.ingredient_name
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri WHERE ri.recipe_id = p.recipe_id AND ri.ingredient_id = i.ingredient_id);

INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT p.recipe_id, seed.image_url, 'image/jpeg', 1, 1
FROM @extra_recipes seed JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_MEDIA] m WHERE m.recipe_id = p.recipe_id);


COMMIT TRANSACTION;
