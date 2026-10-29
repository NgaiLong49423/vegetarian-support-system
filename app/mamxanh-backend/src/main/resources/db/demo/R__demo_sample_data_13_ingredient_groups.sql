-- Curated groups for local demo catalog ingredients; this does not alter nutrition or image data.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @ingredient_groups TABLE (
    ingredient_name NVARCHAR(200) PRIMARY KEY,
    ingredient_group NVARCHAR(100) NOT NULL
);

INSERT INTO @ingredient_groups (ingredient_name, ingredient_group) VALUES
    (N'Bí ngòi', N'Rau củ'), (N'Bí đao', N'Rau củ'), (N'Bí đỏ', N'Rau củ'),
    (N'Bông cải xanh', N'Rau củ'), (N'Bông thiên lý', N'Rau củ'), (N'Bầu', N'Rau củ'),
    (N'Bắp chuối', N'Rau củ'), (N'Bắp cải', N'Rau củ'), (N'Bắp cải tím', N'Rau củ'),
    (N'Bắp non', N'Rau củ'), (N'Cà chua', N'Rau củ'), (N'Cà rốt', N'Rau củ'),
    (N'Cà tím', N'Rau củ'), (N'Cải Brussels', N'Rau củ'), (N'Cải bó xôi', N'Rau củ'),
    (N'Cải bẹ xanh', N'Rau củ'), (N'Cải cầu vồng', N'Rau củ'), (N'Cải ngọt', N'Rau củ'),
    (N'Cải thảo', N'Rau củ'), (N'Cải xoong', N'Rau củ'), (N'Cải xoăn', N'Rau củ'),
    (N'Cần ta', N'Rau củ'), (N'Cần tây', N'Rau củ'), (N'Củ cải trắng', N'Rau củ'),
    (N'Củ cải vàng', N'Rau củ'), (N'Củ cải đỏ', N'Rau củ'), (N'Củ dền', N'Rau củ'),
    (N'Củ năng', N'Rau củ'), (N'Củ sen', N'Rau củ'), (N'Củ sắn', N'Rau củ'),
    (N'Dưa leo', N'Rau củ'), (N'Dọc mùng', N'Rau củ'), (N'Giá đỗ', N'Rau củ'),
    (N'Hành boa rô', N'Rau củ'), (N'Hành lá', N'Rau củ'), (N'Hành tây', N'Rau củ'),
    (N'Hành tím', N'Rau củ'), (N'Khoai lang', N'Rau củ'), (N'Khoai môn', N'Rau củ'),
    (N'Khoai mỡ', N'Rau củ'), (N'Khoai sọ', N'Rau củ'), (N'Khoai tây', N'Rau củ'),
    (N'Khổ qua', N'Rau củ'), (N'Măng tre', N'Rau củ'), (N'Măng tây', N'Rau củ'),
    (N'Mướp hương', N'Rau củ'), (N'Mướp khía', N'Rau củ'), (N'Rau cải thìa', N'Rau củ'),
    (N'Rau diếp cá', N'Rau củ'), (N'Rau dền', N'Rau củ'), (N'Rau kinh giới', N'Rau củ'),
    (N'Rau lang', N'Rau củ'), (N'Rau muống', N'Rau củ'), (N'Rau má', N'Rau củ'),
    (N'Rau mồng tơi', N'Rau củ'), (N'Rau ngót', N'Rau củ'), (N'Rau ngổ', N'Rau củ'),
    (N'Rau thì là', N'Rau củ'), (N'Rau đay', N'Rau củ'), (N'Su su', N'Rau củ'),
    (N'Súp lơ trắng', N'Rau củ'), (N'Tần ô', N'Rau củ'), (N'Xà lách romaine', N'Rau củ'),
    (N'Ớt chuông', N'Rau củ'), (N'Đậu bắp', N'Rau củ'), (N'Đậu bắp tím', N'Rau củ'),
    (N'Đậu cove vàng', N'Rau củ'), (N'Đậu đũa', N'Rau củ'),

    (N'Nấm bào ngư', N'Nấm'), (N'Nấm hương', N'Nấm'), (N'Nấm kim châm', N'Nấm'),
    (N'Nấm mộc nhĩ', N'Nấm'), (N'Nấm mỡ', N'Nấm'), (N'Nấm rơm', N'Nấm'),
    (N'Nấm đùi gà', N'Nấm'),

    (N'Bơ', N'Trái cây'), (N'Bưởi', N'Trái cây'), (N'Cam', N'Trái cây'),
    (N'Chanh', N'Trái cây'), (N'Chanh dây', N'Trái cây'), (N'Chuối', N'Trái cây'),
    (N'Chuối sứ', N'Trái cây'), (N'Chuối xanh', N'Trái cây'), (N'Chà là', N'Trái cây'),
    (N'Chôm chôm', N'Trái cây'), (N'Dâu tây', N'Trái cây'), (N'Dâu tằm', N'Trái cây'),
    (N'Dưa hấu', N'Trái cây'), (N'Dưa lưới', N'Trái cây'), (N'Dứa', N'Trái cây'),
    (N'Dừa nạo', N'Trái cây'), (N'Kiwi', N'Trái cây'), (N'Lê', N'Trái cây'),
    (N'Lựu', N'Trái cây'), (N'Me chua', N'Trái cây'), (N'Mít', N'Trái cây'),
    (N'Mơ', N'Trái cây'), (N'Mận hậu', N'Trái cây'), (N'Nho', N'Trái cây'),
    (N'Nhãn', N'Trái cây'), (N'Quýt', N'Trái cây'), (N'Sầu riêng', N'Trái cây'),
    (N'Thanh long', N'Trái cây'), (N'Thanh trà', N'Trái cây'), (N'Táo', N'Trái cây'),
    (N'Vải', N'Trái cây'), (N'Xoài', N'Trái cây'), (N'Đu đủ', N'Trái cây'),
    (N'Ổi', N'Trái cây'),

    (N'Bánh mì nguyên cám', N'Ngũ cốc'), (N'Bánh phở', N'Ngũ cốc'),
    (N'Bột bắp', N'Ngũ cốc'), (N'Bột gạo', N'Ngũ cốc'), (N'Bột mì', N'Ngũ cốc'),
    (N'Bột năng', N'Ngũ cốc'), (N'Bột yến mạch', N'Ngũ cốc'),
    (N'Gạo basmati', N'Ngũ cốc'), (N'Gạo lứt', N'Ngũ cốc'),
    (N'Gạo nếp', N'Ngũ cốc'), (N'Gạo tẻ', N'Ngũ cốc'), (N'Hạt dền', N'Ngũ cốc'),
    (N'Hạt quinoa', N'Ngũ cốc'), (N'Kê vàng', N'Ngũ cốc'), (N'Kiều mạch', N'Ngũ cốc'),
    (N'Lúa mạch', N'Ngũ cốc'), (N'Mì soba', N'Ngũ cốc'), (N'Mì udon', N'Ngũ cốc'),
    (N'Mì Ý', N'Ngũ cốc'), (N'Ngô hạt', N'Ngũ cốc'), (N'Yến mạch cán', N'Ngũ cốc'),

    (N'Chao', N'Đậu & Chế phẩm'), (N'Đậu Hà Lan', N'Đậu & Chế phẩm'),
    (N'Đậu gà', N'Đậu & Chế phẩm'), (N'Đậu hũ', N'Đậu & Chế phẩm'),
    (N'Đậu hũ ky', N'Đậu & Chế phẩm'), (N'Đậu lăng', N'Đậu & Chế phẩm'),
    (N'Đậu lăng đỏ', N'Đậu & Chế phẩm'), (N'Đậu ngự', N'Đậu & Chế phẩm'),
    (N'Đậu nành hạt', N'Đậu & Chế phẩm'), (N'Đậu nành non', N'Đậu & Chế phẩm'),
    (N'Đậu phộng', N'Đậu & Chế phẩm'), (N'Đậu phụ non', N'Đậu & Chế phẩm'),
    (N'Đậu trắng', N'Đậu & Chế phẩm'), (N'Đậu ván', N'Đậu & Chế phẩm'),
    (N'Đậu xanh', N'Đậu & Chế phẩm'), (N'Đậu đen', N'Đậu & Chế phẩm'),
    (N'Đậu đỏ', N'Đậu & Chế phẩm'), (N'Tempeh', N'Đậu & Chế phẩm'),
    (N'Sữa đậu nành', N'Đậu & Chế phẩm'),

    (N'Bột hạnh nhân', N'Các loại hạt'), (N'Hạnh nhân', N'Các loại hạt'),
    (N'Hạt bí', N'Các loại hạt'), (N'Hạt chia', N'Các loại hạt'),
    (N'Hạt dẻ', N'Các loại hạt'), (N'Hạt dẻ cười', N'Các loại hạt'),
    (N'Hạt hướng dương', N'Các loại hạt'), (N'Hạt lanh', N'Các loại hạt'),
    (N'Hạt mắc ca', N'Các loại hạt'), (N'Hạt phỉ', N'Các loại hạt'),
    (N'Hạt sen', N'Các loại hạt'), (N'Hạt điều', N'Các loại hạt'),
    (N'Mè trắng', N'Các loại hạt'), (N'Mè đen', N'Các loại hạt'),
    (N'Óc chó', N'Các loại hạt'),

    (N'Bột cà ri', N'Gia vị chay'), (N'Bột nghệ', N'Gia vị chay'),
    (N'Dầu ăn', N'Gia vị chay'), (N'Gừng', N'Gia vị chay'),
    (N'Hoa hồi', N'Gia vị chay'), (N'Húng chanh', N'Gia vị chay'),
    (N'Húng lủi', N'Gia vị chay'), (N'Húng quế', N'Gia vị chay'),
    (N'Hạt mùi', N'Gia vị chay'), (N'Hạt nêm', N'Gia vị chay'),
    (N'Hạt thì là', N'Gia vị chay'), (N'Lá chanh', N'Gia vị chay'),
    (N'Lá dứa', N'Gia vị chay'), (N'Lá lốt', N'Gia vị chay'),
    (N'Muối ăn', N'Gia vị chay'), (N'Nghệ tươi', N'Gia vị chay'),
    (N'Ngò rí', N'Gia vị chay'), (N'Nước', N'Khác'),
    (N'Nước tương', N'Gia vị chay'), (N'Quế', N'Gia vị chay'),
    (N'Riềng', N'Gia vị chay'), (N'Sả', N'Gia vị chay'),
    (N'Thảo quả', N'Gia vị chay'), (N'Tiêu đen', N'Gia vị chay'),
    (N'Tía tô', N'Gia vị chay'), (N'Tỏi', N'Gia vị chay'),
    (N'Tương miso', N'Gia vị chay'), (N'Đinh hương', N'Gia vị chay'),
    (N'Đường ăn', N'Gia vị chay'), (N'Ớt', N'Gia vị chay'),
    (N'Ớt bột', N'Gia vị chay'), (N'Ớt đỏ', N'Gia vị chay'),

    (N'Phô mai cheddar', N'Sữa & Trứng'), (N'Phô mai cottage', N'Sữa & Trứng'),
    (N'Phô mai cream cheese', N'Sữa & Trứng'), (N'Phô mai mozzarella', N'Sữa & Trứng'),
    (N'Phô mai parmesan', N'Sữa & Trứng'), (N'Phô mai xanh', N'Sữa & Trứng'),
    (N'Trứng gà', N'Sữa & Trứng'),

    (N'Rong biển', N'Rong biển'), (N'Tảo bẹ kombu', N'Rong biển'),
    (N'Seitan', N'Đạm thực vật'), (N'Chả giò chay', N'Thực phẩm chay chế biến'),
    (N'Phô mai chay đậu nành', N'Thực phẩm chay chế biến'),
    (N'Phô mai chay hạt điều', N'Thực phẩm chay chế biến');

IF EXISTS (
    SELECT 1
    FROM @ingredient_groups seed
    WHERE NOT EXISTS (
        SELECT 1 FROM [INGREDIENT] ingredient WHERE ingredient.name = seed.ingredient_name
    )
)
BEGIN
    THROW 51000, 'Demo ingredient group fixture references a missing ingredient.', 1;
END;

UPDATE ingredient
SET ingredient_group = seed.ingredient_group,
    updated_at = SYSUTCDATETIME()
FROM [INGREDIENT] ingredient
JOIN @ingredient_groups seed ON seed.ingredient_name = ingredient.name
WHERE ingredient.ingredient_group <> seed.ingredient_group;

COMMIT TRANSACTION;
