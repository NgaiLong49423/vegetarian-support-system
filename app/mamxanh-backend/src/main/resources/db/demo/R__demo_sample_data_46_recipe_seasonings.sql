-- Local demo only: curated quantities and seasonings for exactly 54 fixture recipes.
-- Ingredient quantities describe edible ingredient weights for the full recipe.
-- Dry grains/legumes are weighed before cooking; tofu/seitan/noodles follow their catalog form.
-- Water in this list is retained recipe water; discard soaking and boiling process water.
-- Seasonings are illustrative cooking quantities, not personalized nutrition advice.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @author_id BIGINT = (SELECT user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local');
DECLARE @gram INT = (SELECT unit_id FROM [UNIT] WHERE code = N'g' AND dimension = 'MASS');
IF @author_id IS NULL OR @gram IS NULL
    THROW 51000, 'Demo ingredient enrichment requires the demo author and gram unit.', 1;

DECLARE @recipes TABLE (title NVARCHAR(120) PRIMARY KEY, instructions NVARCHAR(MAX) NOT NULL);
DECLARE @recipe_json NVARCHAR(MAX) = N'[
  {
    "title": "Nấm đùi gà kho tiêu",
    "instructions": "Cắt nấm thành lát dày. Phi tỏi trong dầu, cho nấm vào đảo săn. Thêm nước tương, đường, tiêu và nước, kho nhỏ lửa đến khi sốt bám nấm. Rắc hành lá trước khi dùng.\n\nĐịnh lượng cho 2 phần: nấm đùi gà 250 g, dầu ăn 8 g, tỏi 5 g, nước tương 15 g, đường ăn 5 g, tiêu đen 1 g, hành lá 8 g, nước 100 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Canh nấm hương cà rốt",
    "instructions": "Ngâm nấm hương, bỏ nước ngâm rồi rửa sạch; cắt cà rốt và gừng. Đun lượng nước đã cân với gừng, thêm cà rốt và nấm, nấu mềm. Nêm muối, rắc hành lá và tiêu trước khi dùng.\n\nĐịnh lượng cho 2 phần: nấm hương 30 g, cà rốt 100 g, nước 650 g, muối ăn 2 g, gừng 5 g, hành lá 8 g, tiêu đen 0.3 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Khoai lang hấp",
    "instructions": "Rửa sạch khoai lang, cắt khúc vừa ăn. Hấp cách thủy đến khi mềm; không thêm gia vị để giữ vị ngọt tự nhiên. Nước ở đáy nồi hấp không tính vào phần ăn.\n\nĐịnh lượng cho 2 phần: khoai lang 300 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bông cải xanh xào nấm",
    "instructions": "Rửa sạch bông cải, cắt nấm và băm tỏi. Phi tỏi trong dầu, xào nấm săn rồi thêm bông cải và nước. Đảo nhanh đến khi rau vừa chín, thêm nước tương và tiêu, dùng nóng.\n\nĐịnh lượng cho 2 phần: bông cải xanh 200 g, nấm đùi gà 100 g, dầu ăn 10 g, tỏi 6 g, nước tương 12 g, tiêu đen 0.5 g, nước 30 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Súp đậu lăng rau củ",
    "instructions": "Rửa đậu lăng, cắt cà rốt và hành tây. Xào hành tây cùng dầu và hạt thì là. Thêm đậu lăng, cà rốt và nước, nấu đến khi đậu mềm. Nêm muối và tiêu, khuấy đều trước khi dùng.\n\nĐịnh lượng cho 2 phần: đậu lăng 150 g, cà rốt 80 g, nước 750 g, hành tây 40 g, dầu ăn 8 g, muối ăn 2 g, tiêu đen 0.5 g, hạt thì là 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Mì udon xào rau củ kiểu Nhật",
    "instructions": "Luộc mì udon rồi để ráo. Phi gừng trong dầu, xào nấm và rau củ. Hòa nước tương, đường với lượng nước đã cân, cho vào chảo cùng mì. Đảo đều trên lửa lớn, rắc hành lá.\n\nĐịnh lượng cho 2 phần: mì udon 200 g, nấm kim châm 100 g, rau cải thìa 120 g, đậu hà lan 60 g, dầu ăn 10 g, nước tương 18 g, gừng 6 g, hành lá 10 g, đường ăn 3 g, nước 40 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Cơm chiên dứa đậu phộng",
    "instructions": "Nấu gạo với lượng nước đã cân, để cơm nguội và tơi. Phi hành tím trong dầu, thêm đậu Hà Lan và ngô. Cho cơm vào chiên, nêm nước tương và tiêu. Trộn dứa cùng đậu phộng rang vào cuối để giữ độ giòn.\n\nĐịnh lượng cho 2 phần: dứa 120 g, đậu phộng 30 g, đậu hà lan 60 g, ngô hạt 60 g, gạo tẻ 140 g, nước 280 g, dầu ăn 12 g, nước tương 16 g, hành tím 15 g, tiêu đen 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Phở cuốn nấm bào ngư",
    "instructions": "Xé nấm, áp chảo trong dầu với gừng và một nửa nước tương. Chần cải thìa và giá đỗ, để ráo. Cuốn bánh phở cùng rau và nấm. Hòa phần nước tương còn lại với nước, đường, nước cốt chanh và ớt làm nước chấm.\n\nĐịnh lượng cho 2 phần: bánh phở 200 g, nấm bào ngư 150 g, giá đỗ 80 g, rau cải thìa 80 g, dầu ăn 8 g, nước tương 20 g, gừng 5 g, chanh 15 g, ớt 2 g, đường ăn 6 g, nước 35 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bánh xèo nấm và giá đỗ",
    "instructions": "Pha bột gạo, bột nghệ, muối với 250 g nước. Xào nấm, giá đỗ và bắp cải với một phần dầu. Tráng bột mỏng trên chảo có dầu, thêm nhân, đậy nắp đến khi bánh chín giòn rồi gập đôi. Pha nước tương, 50 g nước còn lại, đường, nước cốt chanh và ớt làm nước chấm.\n\nĐịnh lượng cho 2 phần: bột gạo 150 g, bột nghệ 3 g, nấm mỡ 100 g, giá đỗ 80 g, nước 300 g, dầu ăn 18 g, muối ăn 1.5 g, bắp cải 80 g, nước tương 15 g, chanh 12 g, đường ăn 5 g, ớt 2 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Cháo bí đỏ đậu xanh",
    "instructions": "Vo gạo, rửa đậu xanh. Nấu cùng lượng nước đã cân đến khi mềm. Thêm bí đỏ và gừng, nấu nhỏ lửa cho bí chín rồi nêm muối. Rắc hành lá và tiêu khi dùng.\n\nĐịnh lượng cho 2 phần: bí đỏ 200 g, đậu xanh 80 g, gừng 5 g, hành lá 10 g, gạo tẻ 60 g, nước 850 g, muối ăn 2 g, tiêu đen 0.3 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Đậu hũ sốt nấm mỡ",
    "instructions": "Áp chảo đậu hũ với một nửa dầu. Phi hành tím trong dầu còn lại, xào nấm rồi thêm nước tương, đường và 130 g nước. Hòa bột năng với 20 g nước còn lại, khuấy vào sốt cho sánh. Rưới sốt lên đậu hũ, rắc tiêu.\n\nĐịnh lượng cho 2 phần: đậu hũ 200 g, nấm mỡ 120 g, bột năng 8 g, hành tím 15 g, dầu ăn 10 g, nước tương 16 g, nước 150 g, đường ăn 3 g, tiêu đen 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bánh mì nấm áp chảo",
    "instructions": "Ướp nấm và cà tím với nước tương, tiêu và dầu. Áp chảo đến khi vàng thơm. Chần cải thìa, để ráo. Làm nóng bánh mì, kẹp cùng nấm, cà tím và cải thìa, thêm nước cốt chanh rồi dùng ngay.\n\nĐịnh lượng cho 2 phần: bánh mì nguyên cám 120 g, nấm kim châm 100 g, cà tím 100 g, chanh 10 g, dầu ăn 10 g, nước tương 12 g, tiêu đen 0.5 g, rau cải thìa 60 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bún chả giò chay rau thơm",
    "instructions": "Nướng chả giò chay đến khi vỏ giòn. Chần bánh phở theo loại đang dùng, để ráo; rửa rau và giá đỗ. Hòa nước tương, nước, đường, nước cốt chanh và ớt làm nước chấm. Xếp bánh phở, rau và chả giò vào tô, rắc đậu phộng và chan nước chấm.\n\nĐịnh lượng cho 2 phần: bánh phở 200 g, chả giò chay 150 g, rau cải thìa 80 g, đậu phộng 20 g, giá đỗ 60 g, húng quế 5 g, nước tương 18 g, chanh 15 g, đường ăn 7 g, ớt 2 g, nước 50 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Lẩu nấm cải thảo",
    "instructions": "Đun nước với gừng và riềng, thêm muối và nước tương. Lần lượt cho cải thảo, nấm và đậu hũ vào nấu vừa chín. Chần bánh phở riêng, chia ra hai tô và dùng cùng lẩu nóng.\n\nĐịnh lượng cho 2 phần: nấm kim châm 100 g, nấm bào ngư 150 g, cải thảo 150 g, riềng 10 g, nước 1000 g, gừng 8 g, đậu hũ 200 g, bánh phở 150 g, muối ăn 2 g, nước tương 15 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Chả giò khoai môn đậu xanh",
    "instructions": "Ngâm đậu xanh và nấm, bỏ nước ngâm; nấu đậu mềm rồi để ráo. Trộn khoai môn bào, bắp cải, nấm, hành tím, muối và tiêu thành nhân. Pha bột gạo, bột mì với nước, tráng thành các lá vỏ mỏng trên chảo chống dính. Cuốn nhân vào vỏ, quét dầu và nướng hoặc áp chảo đến khi nhân chín và vỏ vàng giòn.\n\nĐịnh lượng cho 2 phần: khoai môn 150 g, đậu xanh 60 g, bắp cải 80 g, bột gạo 30 g, nấm hương 15 g, bột mì 50 g, nước 110 g, dầu ăn 20 g, muối ăn 1.5 g, tiêu đen 0.5 g, hành tím 15 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Cơm nắm rong biển mè trắng",
    "instructions": "Nấu gạo với lượng nước đã cân. Luộc đậu Hà Lan rồi để ráo. Trộn cơm còn ấm với rong biển vụn, mè trắng rang, đậu Hà Lan và nước tương. Nắm thành viên nhỏ, dùng với dưa leo cắt lát.\n\nĐịnh lượng cho 2 phần: rong biển 5 g, mè trắng 10 g, đậu hà lan 50 g, nước tương 10 g, gạo tẻ 140 g, nước 280 g, dưa leo 80 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Canh chua nấm đậu bắp",
    "instructions": "Đun me với 100 g nước, dầm và lọc bỏ hạt, giữ nước cốt. Cho nước cốt me và 650 g nước còn lại vào nồi, thêm cà chua và nấm. Khi sôi, cho đậu bắp vào nấu vừa chín, nêm muối và đường. Rắc ngò rí và ớt trước khi dùng.\n\nĐịnh lượng cho 2 phần: đậu bắp 100 g, me chua 15 g, nấm mỡ 100 g, ngò rí 10 g, nước 750 g, cà chua 120 g, muối ăn 2 g, đường ăn 8 g, ớt 2 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Mì Ý sốt cà tím nấm",
    "instructions": "Luộc mì Ý đến khi vừa dai rồi để ráo. Phi tỏi trong dầu, áp chảo cà tím và nấm. Thêm cà chua cùng lượng nước đã cân, nấu thành sốt. Nêm nước tương và tiêu, trộn mì vào sốt, thêm húng quế trước khi dùng.\n\nĐịnh lượng cho 2 phần: mì ý 180 g, cà tím 120 g, nấm mỡ 100 g, cà chua 150 g, dầu ăn 12 g, tỏi 6 g, nước tương 12 g, tiêu đen 0.5 g, nước 100 g, húng quế 5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Pudding hạt chia xoài",
    "instructions": "Khuấy hạt chia vào sữa đậu nành, đậy kín và để lạnh ít nhất 3 giờ cho nở. Cắt xoài thành hạt lựu, trộn với nước cốt chanh rồi xếp lên pudding. Không thêm đường để giữ vị ngọt tự nhiên của xoài.\n\nĐịnh lượng cho 2 phần: hạt chia 25 g, xoài 150 g, sữa đậu nành 200 g, chanh 5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Rau ngót nấu nấm kim châm",
    "instructions": "Vò nhẹ rau ngót, cắt bỏ gốc nấm và cắt đậu hũ. Đun lượng nước đã cân, cho đậu hũ vào trước rồi thêm rau ngót và nấm. Nấu vừa chín, nêm nước tương, muối và tiêu, dùng nóng.\n\nĐịnh lượng cho 2 phần: rau ngót 150 g, nấm kim châm 100 g, đậu hũ 100 g, nước tương 10 g, nước 650 g, tiêu đen 0.3 g, muối ăn 1 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bí đỏ nướng mè trắng",
    "instructions": "Cắt bí đỏ, trộn với gừng băm, dầu, muối và tiêu. Nướng đến khi bí mềm, cạnh vàng. Rắc mè trắng rang và thêm nước cốt chanh trước khi dùng.\n\nĐịnh lượng cho 2 phần: bí đỏ 250 g, mè trắng 10 g, gừng 5 g, chanh 10 g, dầu ăn 10 g, muối ăn 1 g, tiêu đen 0.3 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Cơm gạo lứt đậu gà rau củ",
    "instructions": "Ngâm gạo lứt, để ráo rồi nấu với lượng nước đã cân. Ngâm đậu gà, bỏ nước ngâm rồi nấu chín và để ráo. Phi tỏi trong dầu, xào ớt chuông và bí ngòi, nêm nước tương và tiêu. Xếp cùng cơm và đậu gà, rắc hạt hướng dương rang.\n\nĐịnh lượng cho 2 phần: gạo lứt 180 g, đậu gà 100 g, ớt chuông 80 g, bí ngòi 80 g, hạt hướng dương 10 g, nước 360 g, dầu ăn 10 g, nước tương 15 g, tỏi 5 g, tiêu đen 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Cháo yến mạch nấm mộc nhĩ",
    "instructions": "Ngâm nấm mộc nhĩ, bỏ nước ngâm, rửa sạch rồi thái sợi. Đun yến mạch với nước và gừng, thêm nấm cùng cà rốt. Nấu chín mềm, nêm muối và rắc hành lá, tiêu trước khi dùng.\n\nĐịnh lượng cho 2 phần: yến mạch cán 80 g, nấm mộc nhĩ 25 g, cà rốt 60 g, nước 650 g, muối ăn 2 g, gừng 5 g, hành lá 8 g, tiêu đen 0.3 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Salad quinoa bơ lựu",
    "instructions": "Rửa quinoa, nấu với lượng nước đã cân rồi để nguội. Cắt bơ, dưa leo, tách hạt lựu. Hòa nước cốt cam với muối và dầu, trộn nhẹ cùng quinoa, xà lách và trái cây ngay trước khi dùng.\n\nĐịnh lượng cho 2 phần: hạt quinoa 100 g, bơ 80 g, lựu 50 g, xà lách romaine 80 g, dưa leo 50 g, cam 20 g, nước 180 g, muối ăn 1 g, dầu ăn 6 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Đậu hũ áp chảo sốt hạt điều",
    "instructions": "Ngâm hạt điều, bỏ nước ngâm rồi xay với lượng nước đã cân thành sốt. Áp chảo đậu hũ trong dầu đến vàng. Phi tỏi ở góc chảo, thêm sốt hạt điều, nước tương và tiêu, đun sánh. Rưới sốt lên đậu hũ, rắc hạnh nhân lát.\n\nĐịnh lượng cho 2 phần: đậu hũ 200 g, hạt điều 35 g, hạnh nhân 10 g, nước 120 g, dầu ăn 10 g, tỏi 5 g, nước tương 12 g, tiêu đen 0.4 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bún tempeh rau sống",
    "instructions": "Ướp tempeh với 10 g nước tương rồi áp chảo trong dầu. Hòa 12 g nước tương còn lại với nước, đường, nước cốt chanh và ớt làm nước chấm. Chần bánh phở rồi để ráo. Xếp cùng rau sống, tempeh và đậu phộng rang, dùng với nước chấm.\n\nĐịnh lượng cho 2 phần: tempeh 150 g, bánh phở 150 g, dưa leo 80 g, xà lách romaine 70 g, đậu phộng 15 g, dầu ăn 10 g, nước tương 22 g, chanh 15 g, đường ăn 6 g, ớt 2 g, nước 45 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Khoai tây nướng hạt bí",
    "instructions": "Cắt khoai tây thành miếng, trộn với dầu, tỏi băm, muối và tiêu. Nướng đến khi mềm ruột và vàng cạnh. Rang hạt bí riêng rồi rắc lên khoai trước khi dùng.\n\nĐịnh lượng cho 2 phần: khoai tây 250 g, hạt bí 15 g, dầu ăn 12 g, muối ăn 1.5 g, tiêu đen 0.5 g, tỏi 5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Súp đậu trắng cải xoăn",
    "instructions": "Ngâm đậu trắng, bỏ nước ngâm rồi luộc mềm và để ráo. Phi hành tây, tỏi trong dầu, thêm cà chua và nước. Cho đậu vào nấu liu riu, thêm cải xoăn ở cuối. Nêm muối và tiêu khi rau vừa mềm.\n\nĐịnh lượng cho 2 phần: đậu trắng 100 g, cải xoăn 80 g, cà chua 100 g, nước 750 g, hành tây 40 g, tỏi 5 g, dầu ăn 8 g, muối ăn 2 g, tiêu đen 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bánh quinoa chuối dâu tây",
    "instructions": "Rửa quinoa, nấu với lượng nước đã cân rồi để nguội. Nghiền chuối, trộn quinoa, yến mạch và muối, để hỗn hợp nghỉ. Quét dầu mỏng lên chảo, áp chảo từng bánh ở lửa vừa đến chín hai mặt, dùng cùng dâu tây.\n\nĐịnh lượng cho 2 phần: hạt quinoa 80 g, chuối 100 g, dâu tây 80 g, yến mạch cán 40 g, nước 150 g, dầu ăn 6 g, muối ăn 0.3 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Ớt chuông nhồi gạo lứt",
    "instructions": "Nấu gạo lứt với lượng nước đã cân. Ngâm đậu đỏ, bỏ nước ngâm rồi nấu chín và để ráo. Xào hành tây với dầu, thêm cà chua, cơm và đậu, nêm muối, tiêu. Nhồi vào ớt chuông đã bỏ hạt, nướng đến khi ớt mềm và nhân nóng đều.\n\nĐịnh lượng cho 2 phần: ớt chuông 160 g, gạo lứt 100 g, đậu đỏ 80 g, cà chua 60 g, nước 220 g, dầu ăn 8 g, muối ăn 1.5 g, tiêu đen 0.5 g, hành tây 30 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Mì seitan xào măng tây",
    "instructions": "Luộc mì vừa chín rồi để ráo. Phi tỏi trong dầu, xào seitan và măng tây. Thêm mì, nước tương cùng lượng nước đã cân, đảo đều cho sốt bám mì. Rắc tiêu, dùng nóng.\n\nĐịnh lượng cho 2 phần: seitan 120 g, măng tây 100 g, mì ý 120 g, dầu ăn 10 g, tỏi 6 g, nước tương 15 g, tiêu đen 0.5 g, nước 40 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Canh kombu đậu hũ",
    "instructions": "Ngâm kombu trong lượng nước đã cân rồi đun nhỏ lửa. Vớt kombu ra, thái sợi và cho lại vào canh để dùng hết phần rong biển. Thêm nấm đã ngâm rửa, đậu hũ và gừng, nấu sôi nhẹ. Nêm nước tương trước khi dùng.\n\nĐịnh lượng cho 2 phần: tảo bẹ kombu 5 g, đậu hũ 120 g, nấm hương 20 g, nước 700 g, nước tương 12 g, gừng 5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bí ngòi cuộn hạt điều",
    "instructions": "Ngâm hạt điều rồi bỏ nước ngâm. Xay hạt điều với nước, tỏi, muối, tiêu và nước cốt chanh thành sốt. Thái bí ngòi thành lát mỏng, áp chảo nhanh với dầu. Phết sốt lên bí, cuộn lại và dùng với cà chua.\n\nĐịnh lượng cho 2 phần: bí ngòi 160 g, hạt điều 30 g, cà chua 80 g, nước 60 g, dầu ăn 8 g, tỏi 4 g, muối ăn 1 g, tiêu đen 0.4 g, chanh 8 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Súp lơ trắng nghiền hạnh nhân",
    "instructions": "Hấp súp lơ trắng đến mềm, ngâm hạnh nhân rồi bỏ nước ngâm. Xay súp lơ với hạnh nhân, lượng nước ấm đã cân, dầu, muối và tiêu đến mịn. Rắc hạt bí rang, dùng nóng.\n\nĐịnh lượng cho 2 phần: súp lơ trắng 200 g, hạnh nhân 20 g, hạt bí 10 g, nước 80 g, muối ăn 1.5 g, tiêu đen 0.4 g, dầu ăn 5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Đậu đỏ hầm cà chua",
    "instructions": "Ngâm đậu đỏ, bỏ nước ngâm rồi nấu mềm và để ráo. Phi hành tây trong dầu, thêm cà chua, khoai tây, đậu đỏ và lượng nước đã cân. Hầm đến khi khoai mềm, nêm muối, tiêu và rắc hành lá.\n\nĐịnh lượng cho 2 phần: đậu đỏ 100 g, khoai tây 100 g, cà chua 120 g, nước 500 g, dầu ăn 8 g, hành tây 30 g, muối ăn 2 g, tiêu đen 0.5 g, hành lá 8 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Yến mạch qua đêm táo",
    "instructions": "Trộn yến mạch với sữa đậu nành và quế, đậy kín rồi để lạnh qua đêm. Cắt táo nhỏ, rắc hạnh nhân lên trên ngay trước khi ăn. Không thêm đường vì táo đã có vị ngọt tự nhiên.\n\nĐịnh lượng cho 2 phần: yến mạch cán 60 g, táo 100 g, hạnh nhân 10 g, sữa đậu nành 150 g, quế 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Gỏi romaine dưa leo cam",
    "instructions": "Rửa và xé xà lách romaine, cắt dưa leo và tách múi cam. Hòa nước cốt chanh với dầu và muối làm sốt. Trộn nhẹ rau quả với sốt, rắc hạt hướng dương rang và dùng ngay.\n\nĐịnh lượng cho 2 phần: xà lách romaine 100 g, dưa leo 80 g, cam 80 g, hạt hướng dương 10 g, muối ăn 0.8 g, dầu ăn 5 g, chanh 8 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Đậu nành hạt sốt cà tím",
    "instructions": "Ngâm đậu nành hạt, bỏ nước ngâm rồi nấu chín kỹ và để ráo. Phi tỏi trong dầu, áp chảo cà tím, thêm cà chua và lượng nước đã cân. Cho đậu vào, nêm nước tương và tiêu, nấu thêm vài phút cho thấm.\n\nĐịnh lượng cho 2 phần: đậu nành hạt 100 g, cà tím 120 g, cà chua 100 g, nước 150 g, dầu ăn 10 g, tỏi 6 g, nước tương 12 g, tiêu đen 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bánh khoai lang hạt óc chó",
    "instructions": "Hấp khoai lang đến mềm rồi nghiền nhuyễn. Trộn với yến mạch, óc chó băm, muối và quế, tạo miếng dẹt. Quét dầu lên chảo, áp chảo lửa nhỏ đến khi hai mặt vàng và bánh chín đều.\n\nĐịnh lượng cho 2 phần: khoai lang 180 g, óc chó 20 g, yến mạch cán 30 g, dầu ăn 6 g, muối ăn 0.3 g, quế 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Đậu gà nghiền bánh mì",
    "instructions": "Ngâm đậu gà, bỏ nước ngâm, nấu chín và để ráo. Nghiền đậu với nước cốt chanh, nước, dầu, muối và tiêu. Phết lên bánh mì, kẹp cùng xà lách, dưa leo và cà chua, dùng ngay.\n\nĐịnh lượng cho 2 phần: đậu gà 100 g, bánh mì nguyên cám 100 g, xà lách romaine 50 g, dưa leo 50 g, cà chua 30 g, chanh 15 g, muối ăn 1 g, tiêu đen 0.4 g, dầu ăn 6 g, nước 25 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Salad trái cây hạt chia",
    "instructions": "Ngâm hạt chia trong lượng nước đã cân đến khi nở. Cắt chuối, táo, dâu tây và cam thành miếng vừa ăn. Trộn cùng hạt chia, rắc hạnh nhân lát và dùng lạnh. Không thêm đường hoặc muối.\n\nĐịnh lượng cho 2 phần: chuối 60 g, táo 60 g, dâu tây 60 g, cam 60 g, hạt chia 10 g, hạnh nhân 10 g, nước 80 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Canh rau dền mướp hương nấm rơm",
    "instructions": "Ngâm hạt sen, bỏ nước ngâm. Phi hành tím với dầu, cho hạt sen, bí đao và lượng nước đã cân vào nấu gần mềm. Thêm mướp hương, nấm rơm rồi rau dền, nấu vừa chín. Nêm muối và tiêu, dùng nóng.\n\nĐịnh lượng cho 2 phần: rau dền 100 g, mướp hương 120 g, nấm rơm 100 g, hạt sen 40 g, bí đao 100 g, nước 800 g, muối ăn 2 g, hành tím 12 g, dầu ăn 5 g, tiêu đen 0.3 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Cơm gạo lứt đậu nành non",
    "instructions": "Nấu gạo lứt với lượng nước đã cân. Chần đậu nành non và cải bó xôi, để ráo. Xào hành tây trong dầu, cho đậu nành non vào đảo, nêm nước tương và tiêu. Xếp cùng cơm, cải bó xôi và dưa leo cắt lát.\n\nĐịnh lượng cho 2 phần: gạo lứt 160 g, đậu nành non 100 g, cải bó xôi 80 g, dưa leo 60 g, hành tây 20 g, nước 320 g, dầu ăn 8 g, nước tương 12 g, tiêu đen 0.4 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Súp đậu trắng bí đỏ",
    "instructions": "Ngâm đậu trắng, bỏ nước ngâm rồi nấu mềm và để ráo. Phi hành tây, tỏi trong dầu, thêm bí đỏ, cà chua, đậu và nước. Nấu đến khi bí mềm, nghiền nhẹ một phần cho sánh. Nêm muối và tiêu, dùng nóng.\n\nĐịnh lượng cho 2 phần: đậu trắng 80 g, bí đỏ 180 g, cà chua 80 g, hành tây 30 g, nước 700 g, dầu ăn 8 g, muối ăn 2 g, tiêu đen 0.4 g, tỏi 5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Mì soba nấm mộc nhĩ sốt miso",
    "instructions": "Ngâm nấm mộc nhĩ, bỏ nước ngâm và thái sợi; luộc mì soba rồi để ráo. Xào nấm với dầu và gừng. Hòa miso cùng lượng nước nóng đã cân, cho vào chảo cùng mì, đảo nhanh ở lửa nhỏ. Rắc hành lá và mè trắng rang.\n\nĐịnh lượng cho 2 phần: mì soba 160 g, nấm mộc nhĩ 20 g, tương miso 15 g, gừng 5 g, hành lá 10 g, dầu ăn 8 g, nước 70 g, mè trắng 6 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Đậu phụ non sốt miso nấm",
    "instructions": "Ngâm nấm hương rồi bỏ nước ngâm, rửa sạch. Áp chảo nhẹ đậu phụ non trong dầu. Đun nấm với gừng và lượng nước đã cân, hạ lửa rồi hòa miso vào. Rưới sốt lên đậu phụ, rắc hành lá và tiêu.\n\nĐịnh lượng cho 2 phần: đậu phụ non 220 g, tương miso 15 g, nấm hương 40 g, gừng 5 g, dầu ăn 8 g, nước 150 g, hành lá 8 g, tiêu đen 0.3 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Salad cải bó xôi bơ lựu",
    "instructions": "Rửa cải bó xôi, để ráo; cắt bơ và tách hạt lựu. Hòa nước cốt chanh với dầu, muối và tiêu. Trộn nhẹ cùng rau, bơ, lựu và óc chó ngay trước khi dùng.\n\nĐịnh lượng cho 2 phần: cải bó xôi 100 g, bơ 80 g, lựu 50 g, óc chó 15 g, chanh 10 g, dầu ăn 6 g, muối ăn 0.8 g, tiêu đen 0.2 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bánh mì kẹp seitan rau thơm",
    "instructions": "Cắt seitan, ướp với nước tương, tiêu và tỏi rồi áp chảo trong dầu. Làm nóng bánh mì, kẹp seitan với xà lách romaine, dưa leo, cà chua và húng quế. Dùng ngay khi bánh còn ấm.\n\nĐịnh lượng cho 2 phần: bánh mì nguyên cám 100 g, seitan 100 g, xà lách romaine 40 g, dưa leo 40 g, cà chua 40 g, húng quế 5 g, dầu ăn 8 g, nước tương 12 g, tiêu đen 0.4 g, tỏi 4 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Khoai sọ nướng mè đen",
    "instructions": "Gọt khoai sọ, cắt miếng và hấp sơ. Trộn khoai với dầu, gừng băm, muối và tiêu, nướng đến khi chín mềm và vàng cạnh. Rắc mè đen rang trước khi dùng.\n\nĐịnh lượng cho 2 phần: khoai sọ 220 g, mè đen 8 g, gừng 5 g, dầu ăn 10 g, muối ăn 1 g, tiêu đen 0.3 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Bánh yến mạch trứng chuối",
    "instructions": "Nghiền chuối, trộn với trứng đã bỏ vỏ, bột yến mạch, muối và quế. Để hỗn hợp nghỉ vài phút. Quét dầu lên chảo, áp chảo từng bánh nhỏ ở lửa vừa đến khi trứng chín kỹ hai mặt. Dùng cùng dâu tây.\n\nĐịnh lượng cho 2 phần: bột yến mạch 60 g, trứng gà 100 g, chuối 80 g, dâu tây 50 g, dầu ăn 6 g, muối ăn 0.3 g, quế 0.5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Chè kê vàng chà là dừa",
    "instructions": "Vo kê vàng, nấu với lượng nước đã cân và lá dứa buộc gọn đến khi kê mềm. Thêm chà là, dừa nạo và muối, đun thêm vài phút. Vớt lá dứa ra trước khi dùng; phần lá chỉ tạo hương, không ăn. Không thêm đường vì chà là đã ngọt.\n\nĐịnh lượng cho 2 phần: kê vàng 60 g, chà là 25 g, dừa nạo 20 g, nước 550 g, muối ăn 0.3 g, lá dứa 5 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Khổ qua nhồi đậu phụ non",
    "instructions": "Ngâm nấm mộc nhĩ, bỏ nước ngâm rồi băm nhỏ. Trộn đậu phụ non, nấm, cà rốt, gừng, một nửa muối và tiêu thành nhân. Nhồi vào khổ qua đã bỏ ruột. Đun lượng nước đã cân, nêm nước tương và muối còn lại, thả khổ qua vào nấu chín mềm, rắc hành lá.\n\nĐịnh lượng cho 2 phần: khổ qua 200 g, đậu phụ non 120 g, nấm mộc nhĩ 15 g, cà rốt 40 g, gừng 5 g, nước 800 g, muối ăn 2 g, nước tương 10 g, tiêu đen 0.5 g, hành lá 8 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Đậu hũ sốt cà chua",
    "instructions": "Cắt đậu hũ, băm tỏi và cà chua. Phi tỏi trong dầu, cho cà chua vào xào mềm. Thêm nước, nước tương và đường, cho đậu hũ vào rim nhỏ lửa 10 phút. Rắc tiêu và hành lá, dùng nóng.\n\nĐịnh lượng cho 2 phần: đậu hũ 300 g, cà chua 200 g, dầu ăn 10 g, tỏi 5 g, nước tương 12 g, đường ăn 4 g, tiêu đen 0.5 g, hành lá 8 g, nước 100 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  },
  {
    "title": "Rau muống xào tỏi",
    "instructions": "Nhặt rau muống và rửa sạch, để ráo. Đập dập tỏi, phi thơm trong dầu. Cho rau vào xào lửa lớn cùng nước và muối đến khi vừa chín, rắc tiêu rồi dùng ngay.\n\nĐịnh lượng cho 2 phần: rau muống 300 g, tỏi 9 g, dầu ăn 10 g, muối ăn 1.5 g, tiêu đen 0.3 g, nước 20 g. Các lượng nước ngâm/rửa và nước luộc bỏ đi không nằm trong danh sách nguyên liệu."
  }
]';
INSERT INTO @recipes (title, instructions)
SELECT title, instructions
FROM OPENJSON(@recipe_json)
WITH (title NVARCHAR(120), instructions NVARCHAR(MAX));

DECLARE @ingredients TABLE (
    title NVARCHAR(120) NOT NULL, ingredient_name NVARCHAR(200) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (title, ingredient_name)
);
INSERT INTO @ingredients (title, ingredient_name, quantity) VALUES
    (N'Mì udon xào rau củ kiểu Nhật', N'Mì udon', 200),
    (N'Mì udon xào rau củ kiểu Nhật', N'Nấm kim châm', 100),
    (N'Mì udon xào rau củ kiểu Nhật', N'Rau cải thìa', 120),
    (N'Mì udon xào rau củ kiểu Nhật', N'Đậu Hà Lan', 60),
    (N'Cơm chiên dứa đậu phộng', N'Dứa', 120),
    (N'Cơm chiên dứa đậu phộng', N'Đậu phộng', 30),
    (N'Cơm chiên dứa đậu phộng', N'Đậu Hà Lan', 60),
    (N'Cơm chiên dứa đậu phộng', N'Ngô hạt', 60),
    (N'Phở cuốn nấm bào ngư', N'Bánh phở', 200),
    (N'Phở cuốn nấm bào ngư', N'Nấm bào ngư', 150),
    (N'Phở cuốn nấm bào ngư', N'Giá đỗ', 80),
    (N'Phở cuốn nấm bào ngư', N'Rau cải thìa', 80),
    (N'Bánh xèo nấm và giá đỗ', N'Bột gạo', 150),
    (N'Bánh xèo nấm và giá đỗ', N'Bột nghệ', 3),
    (N'Bánh xèo nấm và giá đỗ', N'Nấm mỡ', 100),
    (N'Bánh xèo nấm và giá đỗ', N'Giá đỗ', 80),
    (N'Cháo bí đỏ đậu xanh', N'Bí đỏ', 200),
    (N'Cháo bí đỏ đậu xanh', N'Đậu xanh', 80),
    (N'Cháo bí đỏ đậu xanh', N'Gừng', 5),
    (N'Cháo bí đỏ đậu xanh', N'Hành lá', 10),
    (N'Đậu hũ sốt nấm mỡ', N'Đậu hũ', 200),
    (N'Đậu hũ sốt nấm mỡ', N'Nấm mỡ', 120),
    (N'Đậu hũ sốt nấm mỡ', N'Bột năng', 8),
    (N'Đậu hũ sốt nấm mỡ', N'Hành tím', 15),
    (N'Bánh mì nấm áp chảo', N'Bánh mì nguyên cám', 120),
    (N'Bánh mì nấm áp chảo', N'Nấm kim châm', 100),
    (N'Bánh mì nấm áp chảo', N'Cà tím', 100),
    (N'Bánh mì nấm áp chảo', N'Chanh', 10),
    (N'Bún chả giò chay rau thơm', N'Bánh phở', 200),
    (N'Bún chả giò chay rau thơm', N'Chả giò chay', 150),
    (N'Bún chả giò chay rau thơm', N'Rau cải thìa', 80),
    (N'Bún chả giò chay rau thơm', N'Đậu phộng', 20),
    (N'Lẩu nấm cải thảo', N'Nấm kim châm', 100),
    (N'Lẩu nấm cải thảo', N'Nấm bào ngư', 150),
    (N'Lẩu nấm cải thảo', N'Cải thảo', 150),
    (N'Lẩu nấm cải thảo', N'Riềng', 10),
    (N'Chả giò khoai môn đậu xanh', N'Khoai môn', 150),
    (N'Chả giò khoai môn đậu xanh', N'Đậu xanh', 60),
    (N'Chả giò khoai môn đậu xanh', N'Bắp cải', 80),
    (N'Chả giò khoai môn đậu xanh', N'Bột gạo', 30),
    (N'Cơm nắm rong biển mè trắng', N'Rong biển', 5),
    (N'Cơm nắm rong biển mè trắng', N'Mè trắng', 10),
    (N'Cơm nắm rong biển mè trắng', N'Đậu Hà Lan', 50),
    (N'Cơm nắm rong biển mè trắng', N'Nước tương', 10),
    (N'Canh chua nấm đậu bắp', N'Đậu bắp', 100),
    (N'Canh chua nấm đậu bắp', N'Me chua', 15),
    (N'Canh chua nấm đậu bắp', N'Nấm mỡ', 100),
    (N'Canh chua nấm đậu bắp', N'Ngò rí', 10),
    (N'Mì Ý sốt cà tím nấm', N'Mì Ý', 180),
    (N'Mì Ý sốt cà tím nấm', N'Cà tím', 120),
    (N'Mì Ý sốt cà tím nấm', N'Nấm mỡ', 100),
    (N'Mì Ý sốt cà tím nấm', N'Cà chua', 150),
    (N'Pudding hạt chia xoài', N'Hạt chia', 25),
    (N'Pudding hạt chia xoài', N'Xoài', 150),
    (N'Pudding hạt chia xoài', N'Sữa đậu nành', 200),
    (N'Pudding hạt chia xoài', N'Chanh', 5),
    (N'Rau ngót nấu nấm kim châm', N'Rau ngót', 150),
    (N'Rau ngót nấu nấm kim châm', N'Nấm kim châm', 100),
    (N'Rau ngót nấu nấm kim châm', N'Đậu hũ', 100),
    (N'Rau ngót nấu nấm kim châm', N'Nước tương', 10),
    (N'Bí đỏ nướng mè trắng', N'Bí đỏ', 250),
    (N'Bí đỏ nướng mè trắng', N'Mè trắng', 10),
    (N'Bí đỏ nướng mè trắng', N'Gừng', 5),
    (N'Bí đỏ nướng mè trắng', N'Chanh', 10),
    (N'Cơm gạo lứt đậu gà rau củ', N'Gạo lứt', 180),
    (N'Cơm gạo lứt đậu gà rau củ', N'Đậu gà', 100),
    (N'Cơm gạo lứt đậu gà rau củ', N'Ớt chuông', 80),
    (N'Cơm gạo lứt đậu gà rau củ', N'Bí ngòi', 80),
    (N'Cơm gạo lứt đậu gà rau củ', N'Hạt hướng dương', 10),
    (N'Cháo yến mạch nấm mộc nhĩ', N'Yến mạch cán', 80),
    (N'Cháo yến mạch nấm mộc nhĩ', N'Nấm mộc nhĩ', 25),
    (N'Cháo yến mạch nấm mộc nhĩ', N'Cà rốt', 60),
    (N'Salad quinoa bơ lựu', N'Hạt quinoa', 100),
    (N'Salad quinoa bơ lựu', N'Bơ', 80),
    (N'Salad quinoa bơ lựu', N'Lựu', 50),
    (N'Salad quinoa bơ lựu', N'Xà lách romaine', 80),
    (N'Salad quinoa bơ lựu', N'Dưa leo', 50),
    (N'Salad quinoa bơ lựu', N'Cam', 20),
    (N'Đậu hũ áp chảo sốt hạt điều', N'Đậu hũ', 200),
    (N'Đậu hũ áp chảo sốt hạt điều', N'Hạt điều', 35),
    (N'Đậu hũ áp chảo sốt hạt điều', N'Hạnh nhân', 10),
    (N'Bún tempeh rau sống', N'Tempeh', 150),
    (N'Bún tempeh rau sống', N'Bánh phở', 150),
    (N'Bún tempeh rau sống', N'Dưa leo', 80),
    (N'Bún tempeh rau sống', N'Xà lách romaine', 70),
    (N'Bún tempeh rau sống', N'Đậu phộng', 15),
    (N'Khoai tây nướng hạt bí', N'Khoai tây', 250),
    (N'Khoai tây nướng hạt bí', N'Hạt bí', 15),
    (N'Súp đậu trắng cải xoăn', N'Đậu trắng', 100),
    (N'Súp đậu trắng cải xoăn', N'Cải xoăn', 80),
    (N'Súp đậu trắng cải xoăn', N'Cà chua', 100),
    (N'Bánh quinoa chuối dâu tây', N'Hạt quinoa', 80),
    (N'Bánh quinoa chuối dâu tây', N'Chuối', 100),
    (N'Bánh quinoa chuối dâu tây', N'Dâu tây', 80),
    (N'Bánh quinoa chuối dâu tây', N'Yến mạch cán', 40),
    (N'Ớt chuông nhồi gạo lứt', N'Ớt chuông', 160),
    (N'Ớt chuông nhồi gạo lứt', N'Gạo lứt', 100),
    (N'Ớt chuông nhồi gạo lứt', N'Đậu đỏ', 80),
    (N'Ớt chuông nhồi gạo lứt', N'Cà chua', 60),
    (N'Mì seitan xào măng tây', N'Seitan', 120),
    (N'Mì seitan xào măng tây', N'Măng tây', 100),
    (N'Mì seitan xào măng tây', N'Mì Ý', 120),
    (N'Canh kombu đậu hũ', N'Tảo bẹ kombu', 5),
    (N'Canh kombu đậu hũ', N'Đậu hũ', 120),
    (N'Canh kombu đậu hũ', N'Nấm hương', 20),
    (N'Bí ngòi cuộn hạt điều', N'Bí ngòi', 160),
    (N'Bí ngòi cuộn hạt điều', N'Hạt điều', 30),
    (N'Bí ngòi cuộn hạt điều', N'Cà chua', 80),
    (N'Súp lơ trắng nghiền hạnh nhân', N'Súp lơ trắng', 200),
    (N'Súp lơ trắng nghiền hạnh nhân', N'Hạnh nhân', 20),
    (N'Súp lơ trắng nghiền hạnh nhân', N'Hạt bí', 10),
    (N'Đậu đỏ hầm cà chua', N'Đậu đỏ', 100),
    (N'Đậu đỏ hầm cà chua', N'Khoai tây', 100),
    (N'Đậu đỏ hầm cà chua', N'Cà chua', 120),
    (N'Yến mạch qua đêm táo', N'Yến mạch cán', 60),
    (N'Yến mạch qua đêm táo', N'Táo', 100),
    (N'Yến mạch qua đêm táo', N'Hạnh nhân', 10),
    (N'Yến mạch qua đêm táo', N'Sữa đậu nành', 150),
    (N'Gỏi romaine dưa leo cam', N'Xà lách romaine', 100),
    (N'Gỏi romaine dưa leo cam', N'Dưa leo', 80),
    (N'Gỏi romaine dưa leo cam', N'Cam', 80),
    (N'Gỏi romaine dưa leo cam', N'Hạt hướng dương', 10),
    (N'Đậu nành hạt sốt cà tím', N'Đậu nành hạt', 100),
    (N'Đậu nành hạt sốt cà tím', N'Cà tím', 120),
    (N'Đậu nành hạt sốt cà tím', N'Cà chua', 100),
    (N'Bánh khoai lang hạt óc chó', N'Khoai lang', 180),
    (N'Bánh khoai lang hạt óc chó', N'Óc chó', 20),
    (N'Bánh khoai lang hạt óc chó', N'Yến mạch cán', 30),
    (N'Đậu gà nghiền bánh mì', N'Đậu gà', 100),
    (N'Đậu gà nghiền bánh mì', N'Bánh mì nguyên cám', 100),
    (N'Đậu gà nghiền bánh mì', N'Xà lách romaine', 50),
    (N'Đậu gà nghiền bánh mì', N'Dưa leo', 50),
    (N'Đậu gà nghiền bánh mì', N'Cà chua', 30),
    (N'Salad trái cây hạt chia', N'Chuối', 60),
    (N'Salad trái cây hạt chia', N'Táo', 60),
    (N'Salad trái cây hạt chia', N'Dâu tây', 60),
    (N'Salad trái cây hạt chia', N'Cam', 60),
    (N'Salad trái cây hạt chia', N'Hạt chia', 10),
    (N'Salad trái cây hạt chia', N'Hạnh nhân', 10),
    (N'Canh rau dền mướp hương nấm rơm', N'Rau dền', 100),
    (N'Canh rau dền mướp hương nấm rơm', N'Mướp hương', 120),
    (N'Canh rau dền mướp hương nấm rơm', N'Nấm rơm', 100),
    (N'Canh rau dền mướp hương nấm rơm', N'Hạt sen', 40),
    (N'Canh rau dền mướp hương nấm rơm', N'Bí đao', 100),
    (N'Cơm gạo lứt đậu nành non', N'Gạo lứt', 160),
    (N'Cơm gạo lứt đậu nành non', N'Đậu nành non', 100),
    (N'Cơm gạo lứt đậu nành non', N'Cải bó xôi', 80),
    (N'Cơm gạo lứt đậu nành non', N'Dưa leo', 60),
    (N'Cơm gạo lứt đậu nành non', N'Hành tây', 20),
    (N'Súp đậu trắng bí đỏ', N'Đậu trắng', 80),
    (N'Súp đậu trắng bí đỏ', N'Bí đỏ', 180),
    (N'Súp đậu trắng bí đỏ', N'Cà chua', 80),
    (N'Súp đậu trắng bí đỏ', N'Hành tây', 30),
    (N'Mì soba nấm mộc nhĩ sốt miso', N'Mì soba', 160),
    (N'Mì soba nấm mộc nhĩ sốt miso', N'Nấm mộc nhĩ', 20),
    (N'Mì soba nấm mộc nhĩ sốt miso', N'Tương miso', 15),
    (N'Mì soba nấm mộc nhĩ sốt miso', N'Gừng', 5),
    (N'Mì soba nấm mộc nhĩ sốt miso', N'Hành lá', 10),
    (N'Đậu phụ non sốt miso nấm', N'Đậu phụ non', 220),
    (N'Đậu phụ non sốt miso nấm', N'Tương miso', 15),
    (N'Đậu phụ non sốt miso nấm', N'Nấm hương', 40),
    (N'Đậu phụ non sốt miso nấm', N'Gừng', 5),
    (N'Salad cải bó xôi bơ lựu', N'Cải bó xôi', 100),
    (N'Salad cải bó xôi bơ lựu', N'Bơ', 80),
    (N'Salad cải bó xôi bơ lựu', N'Lựu', 50),
    (N'Salad cải bó xôi bơ lựu', N'Óc chó', 15),
    (N'Salad cải bó xôi bơ lựu', N'Chanh', 10),
    (N'Bánh mì kẹp seitan rau thơm', N'Bánh mì nguyên cám', 100),
    (N'Bánh mì kẹp seitan rau thơm', N'Seitan', 100),
    (N'Bánh mì kẹp seitan rau thơm', N'Xà lách romaine', 40),
    (N'Bánh mì kẹp seitan rau thơm', N'Dưa leo', 40),
    (N'Bánh mì kẹp seitan rau thơm', N'Cà chua', 40),
    (N'Bánh mì kẹp seitan rau thơm', N'Húng quế', 5),
    (N'Khoai sọ nướng mè đen', N'Khoai sọ', 220),
    (N'Khoai sọ nướng mè đen', N'Mè đen', 8),
    (N'Khoai sọ nướng mè đen', N'Gừng', 5),
    (N'Bánh yến mạch trứng chuối', N'Bột yến mạch', 60),
    (N'Bánh yến mạch trứng chuối', N'Trứng gà', 100),
    (N'Bánh yến mạch trứng chuối', N'Chuối', 80),
    (N'Bánh yến mạch trứng chuối', N'Dâu tây', 50),
    (N'Chè kê vàng chà là dừa', N'Kê vàng', 60),
    (N'Chè kê vàng chà là dừa', N'Chà là', 25),
    (N'Chè kê vàng chà là dừa', N'Dừa nạo', 20),
    (N'Khổ qua nhồi đậu phụ non', N'Khổ qua', 200),
    (N'Khổ qua nhồi đậu phụ non', N'Đậu phụ non', 120),
    (N'Khổ qua nhồi đậu phụ non', N'Nấm mộc nhĩ', 15),
    (N'Khổ qua nhồi đậu phụ non', N'Cà rốt', 40),
    (N'Khổ qua nhồi đậu phụ non', N'Gừng', 5),
    (N'Nấm đùi gà kho tiêu', N'Nấm đùi gà', 250),
    (N'Canh nấm hương cà rốt', N'Nấm hương', 30),
    (N'Canh nấm hương cà rốt', N'Cà rốt', 100),
    (N'Khoai lang hấp', N'Khoai lang', 300),
    (N'Bông cải xanh xào nấm', N'Bông cải xanh', 200),
    (N'Bông cải xanh xào nấm', N'Nấm đùi gà', 100),
    (N'Súp đậu lăng rau củ', N'Đậu lăng', 150),
    (N'Súp đậu lăng rau củ', N'Cà rốt', 80),
    (N'Đậu hũ sốt cà chua', N'Đậu hũ', 300),
    (N'Đậu hũ sốt cà chua', N'Cà chua', 200),
    (N'Rau muống xào tỏi', N'Rau muống', 300),
    (N'Rau muống xào tỏi', N'Tỏi', 9),
    (N'Đậu hũ sốt cà chua', N'Dầu ăn', 10),
    (N'Đậu hũ sốt cà chua', N'Tỏi', 5),
    (N'Đậu hũ sốt cà chua', N'Nước tương', 12),
    (N'Đậu hũ sốt cà chua', N'Đường ăn', 4),
    (N'Đậu hũ sốt cà chua', N'Tiêu đen', 0.5),
    (N'Đậu hũ sốt cà chua', N'Hành lá', 8),
    (N'Đậu hũ sốt cà chua', N'Nước', 100),
    (N'Rau muống xào tỏi', N'Dầu ăn', 10),
    (N'Rau muống xào tỏi', N'Muối ăn', 1.5),
    (N'Rau muống xào tỏi', N'Tiêu đen', 0.3),
    (N'Rau muống xào tỏi', N'Nước', 20),
    (N'Nấm đùi gà kho tiêu', N'Dầu ăn', 8),
    (N'Nấm đùi gà kho tiêu', N'Tỏi', 5),
    (N'Nấm đùi gà kho tiêu', N'Nước tương', 15),
    (N'Nấm đùi gà kho tiêu', N'Đường ăn', 5),
    (N'Nấm đùi gà kho tiêu', N'Tiêu đen', 1),
    (N'Nấm đùi gà kho tiêu', N'Hành lá', 8),
    (N'Nấm đùi gà kho tiêu', N'Nước', 100),
    (N'Canh nấm hương cà rốt', N'Nước', 650),
    (N'Canh nấm hương cà rốt', N'Muối ăn', 2),
    (N'Canh nấm hương cà rốt', N'Gừng', 5),
    (N'Canh nấm hương cà rốt', N'Hành lá', 8),
    (N'Canh nấm hương cà rốt', N'Tiêu đen', 0.3),
    (N'Bông cải xanh xào nấm', N'Dầu ăn', 10),
    (N'Bông cải xanh xào nấm', N'Tỏi', 6),
    (N'Bông cải xanh xào nấm', N'Nước tương', 12),
    (N'Bông cải xanh xào nấm', N'Tiêu đen', 0.5),
    (N'Bông cải xanh xào nấm', N'Nước', 30),
    (N'Súp đậu lăng rau củ', N'Nước', 750),
    (N'Súp đậu lăng rau củ', N'Hành tây', 40),
    (N'Súp đậu lăng rau củ', N'Dầu ăn', 8),
    (N'Súp đậu lăng rau củ', N'Muối ăn', 2),
    (N'Súp đậu lăng rau củ', N'Tiêu đen', 0.5),
    (N'Súp đậu lăng rau củ', N'Hạt thì là', 0.5),
    (N'Mì udon xào rau củ kiểu Nhật', N'Dầu ăn', 10),
    (N'Mì udon xào rau củ kiểu Nhật', N'Nước tương', 18),
    (N'Mì udon xào rau củ kiểu Nhật', N'Gừng', 6),
    (N'Mì udon xào rau củ kiểu Nhật', N'Hành lá', 10),
    (N'Mì udon xào rau củ kiểu Nhật', N'Đường ăn', 3),
    (N'Mì udon xào rau củ kiểu Nhật', N'Nước', 40),
    (N'Cơm chiên dứa đậu phộng', N'Gạo tẻ', 140),
    (N'Cơm chiên dứa đậu phộng', N'Nước', 280),
    (N'Cơm chiên dứa đậu phộng', N'Dầu ăn', 12),
    (N'Cơm chiên dứa đậu phộng', N'Nước tương', 16),
    (N'Cơm chiên dứa đậu phộng', N'Hành tím', 15),
    (N'Cơm chiên dứa đậu phộng', N'Tiêu đen', 0.5),
    (N'Phở cuốn nấm bào ngư', N'Dầu ăn', 8),
    (N'Phở cuốn nấm bào ngư', N'Nước tương', 20),
    (N'Phở cuốn nấm bào ngư', N'Gừng', 5),
    (N'Phở cuốn nấm bào ngư', N'Chanh', 15),
    (N'Phở cuốn nấm bào ngư', N'Ớt', 2),
    (N'Phở cuốn nấm bào ngư', N'Đường ăn', 6),
    (N'Phở cuốn nấm bào ngư', N'Nước', 35),
    (N'Bánh xèo nấm và giá đỗ', N'Nước', 300),
    (N'Bánh xèo nấm và giá đỗ', N'Dầu ăn', 18),
    (N'Bánh xèo nấm và giá đỗ', N'Muối ăn', 1.5),
    (N'Bánh xèo nấm và giá đỗ', N'Bắp cải', 80),
    (N'Bánh xèo nấm và giá đỗ', N'Nước tương', 15),
    (N'Bánh xèo nấm và giá đỗ', N'Chanh', 12),
    (N'Bánh xèo nấm và giá đỗ', N'Đường ăn', 5),
    (N'Bánh xèo nấm và giá đỗ', N'Ớt', 2),
    (N'Cháo bí đỏ đậu xanh', N'Gạo tẻ', 60),
    (N'Cháo bí đỏ đậu xanh', N'Nước', 850),
    (N'Cháo bí đỏ đậu xanh', N'Muối ăn', 2),
    (N'Cháo bí đỏ đậu xanh', N'Tiêu đen', 0.3),
    (N'Đậu hũ sốt nấm mỡ', N'Dầu ăn', 10),
    (N'Đậu hũ sốt nấm mỡ', N'Nước tương', 16),
    (N'Đậu hũ sốt nấm mỡ', N'Nước', 150),
    (N'Đậu hũ sốt nấm mỡ', N'Đường ăn', 3),
    (N'Đậu hũ sốt nấm mỡ', N'Tiêu đen', 0.5),
    (N'Bánh mì nấm áp chảo', N'Dầu ăn', 10),
    (N'Bánh mì nấm áp chảo', N'Nước tương', 12),
    (N'Bánh mì nấm áp chảo', N'Tiêu đen', 0.5),
    (N'Bánh mì nấm áp chảo', N'Rau cải thìa', 60),
    (N'Bún chả giò chay rau thơm', N'Giá đỗ', 60),
    (N'Bún chả giò chay rau thơm', N'Húng quế', 5),
    (N'Bún chả giò chay rau thơm', N'Nước tương', 18),
    (N'Bún chả giò chay rau thơm', N'Chanh', 15),
    (N'Bún chả giò chay rau thơm', N'Đường ăn', 7),
    (N'Bún chả giò chay rau thơm', N'Ớt', 2),
    (N'Bún chả giò chay rau thơm', N'Nước', 50),
    (N'Lẩu nấm cải thảo', N'Nước', 1000),
    (N'Lẩu nấm cải thảo', N'Gừng', 8),
    (N'Lẩu nấm cải thảo', N'Đậu hũ', 200),
    (N'Lẩu nấm cải thảo', N'Bánh phở', 150),
    (N'Lẩu nấm cải thảo', N'Muối ăn', 2),
    (N'Lẩu nấm cải thảo', N'Nước tương', 15),
    (N'Chả giò khoai môn đậu xanh', N'Nấm hương', 15),
    (N'Chả giò khoai môn đậu xanh', N'Bột mì', 50),
    (N'Chả giò khoai môn đậu xanh', N'Nước', 110),
    (N'Chả giò khoai môn đậu xanh', N'Dầu ăn', 20),
    (N'Chả giò khoai môn đậu xanh', N'Muối ăn', 1.5),
    (N'Chả giò khoai môn đậu xanh', N'Tiêu đen', 0.5),
    (N'Chả giò khoai môn đậu xanh', N'Hành tím', 15),
    (N'Cơm nắm rong biển mè trắng', N'Gạo tẻ', 140),
    (N'Cơm nắm rong biển mè trắng', N'Nước', 280),
    (N'Cơm nắm rong biển mè trắng', N'Dưa leo', 80),
    (N'Canh chua nấm đậu bắp', N'Nước', 750),
    (N'Canh chua nấm đậu bắp', N'Cà chua', 120),
    (N'Canh chua nấm đậu bắp', N'Muối ăn', 2),
    (N'Canh chua nấm đậu bắp', N'Đường ăn', 8),
    (N'Canh chua nấm đậu bắp', N'Ớt', 2),
    (N'Mì Ý sốt cà tím nấm', N'Dầu ăn', 12),
    (N'Mì Ý sốt cà tím nấm', N'Tỏi', 6),
    (N'Mì Ý sốt cà tím nấm', N'Nước tương', 12),
    (N'Mì Ý sốt cà tím nấm', N'Tiêu đen', 0.5),
    (N'Mì Ý sốt cà tím nấm', N'Nước', 100),
    (N'Mì Ý sốt cà tím nấm', N'Húng quế', 5),
    (N'Rau ngót nấu nấm kim châm', N'Nước', 650),
    (N'Rau ngót nấu nấm kim châm', N'Tiêu đen', 0.3),
    (N'Rau ngót nấu nấm kim châm', N'Muối ăn', 1),
    (N'Bí đỏ nướng mè trắng', N'Dầu ăn', 10),
    (N'Bí đỏ nướng mè trắng', N'Muối ăn', 1),
    (N'Bí đỏ nướng mè trắng', N'Tiêu đen', 0.3),
    (N'Cơm gạo lứt đậu gà rau củ', N'Nước', 360),
    (N'Cơm gạo lứt đậu gà rau củ', N'Dầu ăn', 10),
    (N'Cơm gạo lứt đậu gà rau củ', N'Nước tương', 15),
    (N'Cơm gạo lứt đậu gà rau củ', N'Tỏi', 5),
    (N'Cơm gạo lứt đậu gà rau củ', N'Tiêu đen', 0.5),
    (N'Cháo yến mạch nấm mộc nhĩ', N'Nước', 650),
    (N'Cháo yến mạch nấm mộc nhĩ', N'Muối ăn', 2),
    (N'Cháo yến mạch nấm mộc nhĩ', N'Gừng', 5),
    (N'Cháo yến mạch nấm mộc nhĩ', N'Hành lá', 8),
    (N'Cháo yến mạch nấm mộc nhĩ', N'Tiêu đen', 0.3),
    (N'Salad quinoa bơ lựu', N'Nước', 180),
    (N'Salad quinoa bơ lựu', N'Muối ăn', 1),
    (N'Salad quinoa bơ lựu', N'Dầu ăn', 6),
    (N'Đậu hũ áp chảo sốt hạt điều', N'Nước', 120),
    (N'Đậu hũ áp chảo sốt hạt điều', N'Dầu ăn', 10),
    (N'Đậu hũ áp chảo sốt hạt điều', N'Tỏi', 5),
    (N'Đậu hũ áp chảo sốt hạt điều', N'Nước tương', 12),
    (N'Đậu hũ áp chảo sốt hạt điều', N'Tiêu đen', 0.4),
    (N'Bún tempeh rau sống', N'Dầu ăn', 10),
    (N'Bún tempeh rau sống', N'Nước tương', 22),
    (N'Bún tempeh rau sống', N'Chanh', 15),
    (N'Bún tempeh rau sống', N'Đường ăn', 6),
    (N'Bún tempeh rau sống', N'Ớt', 2),
    (N'Bún tempeh rau sống', N'Nước', 45),
    (N'Khoai tây nướng hạt bí', N'Dầu ăn', 12),
    (N'Khoai tây nướng hạt bí', N'Muối ăn', 1.5),
    (N'Khoai tây nướng hạt bí', N'Tiêu đen', 0.5),
    (N'Khoai tây nướng hạt bí', N'Tỏi', 5),
    (N'Súp đậu trắng cải xoăn', N'Nước', 750),
    (N'Súp đậu trắng cải xoăn', N'Hành tây', 40),
    (N'Súp đậu trắng cải xoăn', N'Tỏi', 5),
    (N'Súp đậu trắng cải xoăn', N'Dầu ăn', 8),
    (N'Súp đậu trắng cải xoăn', N'Muối ăn', 2),
    (N'Súp đậu trắng cải xoăn', N'Tiêu đen', 0.5),
    (N'Bánh quinoa chuối dâu tây', N'Nước', 150),
    (N'Bánh quinoa chuối dâu tây', N'Dầu ăn', 6),
    (N'Bánh quinoa chuối dâu tây', N'Muối ăn', 0.3),
    (N'Ớt chuông nhồi gạo lứt', N'Nước', 220),
    (N'Ớt chuông nhồi gạo lứt', N'Dầu ăn', 8),
    (N'Ớt chuông nhồi gạo lứt', N'Muối ăn', 1.5),
    (N'Ớt chuông nhồi gạo lứt', N'Tiêu đen', 0.5),
    (N'Ớt chuông nhồi gạo lứt', N'Hành tây', 30),
    (N'Mì seitan xào măng tây', N'Dầu ăn', 10),
    (N'Mì seitan xào măng tây', N'Tỏi', 6),
    (N'Mì seitan xào măng tây', N'Nước tương', 15),
    (N'Mì seitan xào măng tây', N'Tiêu đen', 0.5),
    (N'Mì seitan xào măng tây', N'Nước', 40),
    (N'Canh kombu đậu hũ', N'Nước', 700),
    (N'Canh kombu đậu hũ', N'Nước tương', 12),
    (N'Canh kombu đậu hũ', N'Gừng', 5),
    (N'Bí ngòi cuộn hạt điều', N'Nước', 60),
    (N'Bí ngòi cuộn hạt điều', N'Dầu ăn', 8),
    (N'Bí ngòi cuộn hạt điều', N'Tỏi', 4),
    (N'Bí ngòi cuộn hạt điều', N'Muối ăn', 1),
    (N'Bí ngòi cuộn hạt điều', N'Tiêu đen', 0.4),
    (N'Bí ngòi cuộn hạt điều', N'Chanh', 8),
    (N'Súp lơ trắng nghiền hạnh nhân', N'Nước', 80),
    (N'Súp lơ trắng nghiền hạnh nhân', N'Muối ăn', 1.5),
    (N'Súp lơ trắng nghiền hạnh nhân', N'Tiêu đen', 0.4),
    (N'Súp lơ trắng nghiền hạnh nhân', N'Dầu ăn', 5),
    (N'Đậu đỏ hầm cà chua', N'Nước', 500),
    (N'Đậu đỏ hầm cà chua', N'Dầu ăn', 8),
    (N'Đậu đỏ hầm cà chua', N'Hành tây', 30),
    (N'Đậu đỏ hầm cà chua', N'Muối ăn', 2),
    (N'Đậu đỏ hầm cà chua', N'Tiêu đen', 0.5),
    (N'Đậu đỏ hầm cà chua', N'Hành lá', 8),
    (N'Yến mạch qua đêm táo', N'Quế', 0.5),
    (N'Gỏi romaine dưa leo cam', N'Muối ăn', 0.8),
    (N'Gỏi romaine dưa leo cam', N'Dầu ăn', 5),
    (N'Gỏi romaine dưa leo cam', N'Chanh', 8),
    (N'Đậu nành hạt sốt cà tím', N'Nước', 150),
    (N'Đậu nành hạt sốt cà tím', N'Dầu ăn', 10),
    (N'Đậu nành hạt sốt cà tím', N'Tỏi', 6),
    (N'Đậu nành hạt sốt cà tím', N'Nước tương', 12),
    (N'Đậu nành hạt sốt cà tím', N'Tiêu đen', 0.5),
    (N'Bánh khoai lang hạt óc chó', N'Dầu ăn', 6),
    (N'Bánh khoai lang hạt óc chó', N'Muối ăn', 0.3),
    (N'Bánh khoai lang hạt óc chó', N'Quế', 0.5),
    (N'Đậu gà nghiền bánh mì', N'Chanh', 15),
    (N'Đậu gà nghiền bánh mì', N'Muối ăn', 1),
    (N'Đậu gà nghiền bánh mì', N'Tiêu đen', 0.4),
    (N'Đậu gà nghiền bánh mì', N'Dầu ăn', 6),
    (N'Đậu gà nghiền bánh mì', N'Nước', 25),
    (N'Salad trái cây hạt chia', N'Nước', 80),
    (N'Canh rau dền mướp hương nấm rơm', N'Nước', 800),
    (N'Canh rau dền mướp hương nấm rơm', N'Muối ăn', 2),
    (N'Canh rau dền mướp hương nấm rơm', N'Hành tím', 12),
    (N'Canh rau dền mướp hương nấm rơm', N'Dầu ăn', 5),
    (N'Canh rau dền mướp hương nấm rơm', N'Tiêu đen', 0.3),
    (N'Cơm gạo lứt đậu nành non', N'Nước', 320),
    (N'Cơm gạo lứt đậu nành non', N'Dầu ăn', 8),
    (N'Cơm gạo lứt đậu nành non', N'Nước tương', 12),
    (N'Cơm gạo lứt đậu nành non', N'Tiêu đen', 0.4),
    (N'Súp đậu trắng bí đỏ', N'Nước', 700),
    (N'Súp đậu trắng bí đỏ', N'Dầu ăn', 8),
    (N'Súp đậu trắng bí đỏ', N'Muối ăn', 2),
    (N'Súp đậu trắng bí đỏ', N'Tiêu đen', 0.4),
    (N'Súp đậu trắng bí đỏ', N'Tỏi', 5),
    (N'Mì soba nấm mộc nhĩ sốt miso', N'Dầu ăn', 8),
    (N'Mì soba nấm mộc nhĩ sốt miso', N'Nước', 70),
    (N'Mì soba nấm mộc nhĩ sốt miso', N'Mè trắng', 6),
    (N'Đậu phụ non sốt miso nấm', N'Dầu ăn', 8),
    (N'Đậu phụ non sốt miso nấm', N'Nước', 150),
    (N'Đậu phụ non sốt miso nấm', N'Hành lá', 8),
    (N'Đậu phụ non sốt miso nấm', N'Tiêu đen', 0.3),
    (N'Salad cải bó xôi bơ lựu', N'Dầu ăn', 6),
    (N'Salad cải bó xôi bơ lựu', N'Muối ăn', 0.8),
    (N'Salad cải bó xôi bơ lựu', N'Tiêu đen', 0.2),
    (N'Bánh mì kẹp seitan rau thơm', N'Dầu ăn', 8),
    (N'Bánh mì kẹp seitan rau thơm', N'Nước tương', 12),
    (N'Bánh mì kẹp seitan rau thơm', N'Tiêu đen', 0.4),
    (N'Bánh mì kẹp seitan rau thơm', N'Tỏi', 4),
    (N'Khoai sọ nướng mè đen', N'Dầu ăn', 10),
    (N'Khoai sọ nướng mè đen', N'Muối ăn', 1),
    (N'Khoai sọ nướng mè đen', N'Tiêu đen', 0.3),
    (N'Bánh yến mạch trứng chuối', N'Dầu ăn', 6),
    (N'Bánh yến mạch trứng chuối', N'Muối ăn', 0.3),
    (N'Bánh yến mạch trứng chuối', N'Quế', 0.5),
    (N'Chè kê vàng chà là dừa', N'Nước', 550),
    (N'Chè kê vàng chà là dừa', N'Muối ăn', 0.3),
    (N'Chè kê vàng chà là dừa', N'Lá dứa', 5),
    (N'Khổ qua nhồi đậu phụ non', N'Nước', 800),
    (N'Khổ qua nhồi đậu phụ non', N'Muối ăn', 2),
    (N'Khổ qua nhồi đậu phụ non', N'Nước tương', 10),
    (N'Khổ qua nhồi đậu phụ non', N'Tiêu đen', 0.5),
    (N'Khổ qua nhồi đậu phụ non', N'Hành lá', 8);

-- Fail atomically if a prerequisite is missing; do not silently omit an ingredient.
IF EXISTS (SELECT 1 FROM @recipes seed WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_POST] post WHERE post.author_id = @author_id AND post.title = seed.title))
    THROW 51001, 'A named demo recipe is missing before ingredient enrichment.', 1;
IF EXISTS (SELECT 1 FROM @ingredients seed WHERE NOT EXISTS (
    SELECT 1 FROM [INGREDIENT] ingredient WHERE ingredient.name = seed.ingredient_name AND ingredient.status = 'ACTIVE'))
    THROW 51002, 'A required active demo ingredient is missing.', 1;

DECLARE @targets TABLE (recipe_id BIGINT PRIMARY KEY, title NVARCHAR(120) NOT NULL);
INSERT INTO @targets (recipe_id, title)
SELECT post.recipe_id, post.title FROM [RECIPE_POST] post
JOIN @recipes seed ON seed.title = post.title WHERE post.author_id = @author_id;

IF (SELECT COUNT(*) FROM @targets) <> 54
    THROW 51004, 'Named demo recipe identities must be unambiguous.', 1;

-- Preserve additional ingredients on these named fixtures, converting their units only
-- when there is a real mass factor or a per-ingredient conversion to grams.
IF EXISTS (
    SELECT 1 FROM [RECIPE_INGREDIENT] ri JOIN @targets target ON target.recipe_id = ri.recipe_id
    JOIN [UNIT] unit ON unit.unit_id = ri.unit_id
    LEFT JOIN [INGREDIENT_UNIT_CONVERSION] conversion
      ON conversion.ingredient_id = ri.ingredient_id AND conversion.unit_id = ri.unit_id
    WHERE ri.unit_id <> @gram AND unit.dimension <> 'MASS' AND conversion.grams_per_unit IS NULL
)
    THROW 51003, 'A demo ingredient has no reliable conversion to grams.', 1;

UPDATE ri SET quantity = CONVERT(DECIMAL(10,2), ri.quantity *
        CASE WHEN unit.dimension = 'MASS' THEN unit.base_factor ELSE conversion.grams_per_unit END),
    unit_id = @gram
FROM [RECIPE_INGREDIENT] ri JOIN @targets target ON target.recipe_id = ri.recipe_id
JOIN [UNIT] unit ON unit.unit_id = ri.unit_id
LEFT JOIN [INGREDIENT_UNIT_CONVERSION] conversion
  ON conversion.ingredient_id = ri.ingredient_id AND conversion.unit_id = ri.unit_id
WHERE ri.unit_id <> @gram;

UPDATE ri SET quantity = seed.quantity, unit_id = @gram
FROM [RECIPE_INGREDIENT] ri
JOIN @targets target ON target.recipe_id = ri.recipe_id
JOIN [INGREDIENT] ingredient ON ingredient.ingredient_id = ri.ingredient_id
JOIN @ingredients seed ON seed.title = target.title AND seed.ingredient_name = ingredient.name
WHERE ri.quantity <> seed.quantity OR ri.unit_id <> @gram;

INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT target.recipe_id, ingredient.ingredient_id, @gram, seed.quantity
FROM @ingredients seed JOIN @targets target ON target.title = seed.title
JOIN [INGREDIENT] ingredient ON ingredient.name = seed.ingredient_name AND ingredient.status = 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri
    WHERE ri.recipe_id = target.recipe_id AND ri.ingredient_id = ingredient.ingredient_id);

UPDATE post SET instructions = seed.instructions, servings = 2, updated_at = SYSUTCDATETIME()
FROM [RECIPE_POST] post JOIN @targets target ON target.recipe_id = post.recipe_id
JOIN @recipes seed ON seed.title = target.title
WHERE post.instructions <> seed.instructions OR post.servings <> 2;

COMMIT TRANSACTION;
