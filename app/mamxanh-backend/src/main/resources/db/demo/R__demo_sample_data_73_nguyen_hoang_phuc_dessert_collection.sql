-- Local-only dessert and beverage collection for the demo Expert Nguyen Hoang Phuc.
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
WHERE email = 'demo-expert-03@mamxanh.local'
  AND role = 'EXPERT'
  AND account_status = 'ACTIVE';

SELECT @gram = unit_id
FROM [UNIT]
WHERE code = N'g' AND dimension = 'MASS' AND is_active = 1;

IF @author_id IS NULL
    THROW 51077, 'Active demo Expert demo-expert-03@mamxanh.local is required.', 1;
IF @gram IS NULL
    THROW 51078, 'The active standardized gram unit is required.', 1;

DECLARE @recipes TABLE (
    title NVARCHAR(120) PRIMARY KEY,
    description NVARCHAR(2000) NOT NULL,
    instructions NVARCHAR(MAX) NOT NULL,
    category VARCHAR(20) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    servings INT NOT NULL,
    prep INT NOT NULL,
    cook INT NOT NULL,
    youtube_url VARCHAR(2048) NULL,
    image_url VARCHAR(2048) NOT NULL
);

INSERT INTO @recipes (
    title, description, instructions, category, difficulty, servings, prep, cook, youtube_url, image_url
) VALUES
(
    N'Sinh tố dâu tằm chuối sứ sữa đậu nành',
    N'Sinh tố thuần chay có vị chua dịu của dâu tằm, vị ngọt tự nhiên từ chuối sứ và độ mượt nhẹ của sữa đậu nành. Công thức không cần thêm đá vào cối xay; làm lạnh nguyên liệu trước giúp giữ màu tím đẹp và kết cấu sánh. Dùng ngay sau khi xay cho bữa sáng hoặc bữa phụ.',
    N'Khẩu phần: 2 ly. Làm lạnh sữa đậu nành và chuối trước khi bắt đầu.' + NCHAR(10) + NCHAR(10) +
    N'1. Nhặt bỏ cuống dâu tằm, rửa nhanh dưới vòi nước nhẹ rồi để thật ráo. Bóc chuối sứ và cắt lát dày khoảng 2 cm.' + NCHAR(10) + NCHAR(10) +
    N'2. Cho dâu tằm, chuối và khoảng hai phần ba lượng sữa đậu nành vào cối. Xay ở tốc độ thấp vài giây để phá nhỏ trái cây, sau đó tăng lên tốc độ cao.' + NCHAR(10) + NCHAR(10) +
    N'3. Xay 30–45 giây đến khi hỗn hợp mịn, không còn mảnh chuối lớn. Dừng máy và vét thành cối một lần nếu trái cây bám trên thành.' + NCHAR(10) + NCHAR(10) +
    N'4. Thêm phần sữa còn lại từng ít một đến khi đạt độ sánh mong muốn; không thêm toàn bộ cùng lúc nếu muốn ly sinh tố đặc.' + NCHAR(10) + NCHAR(10) +
    N'5. Nếm và điều chỉnh bằng một ít chuối hoặc sữa, không cần thêm đường nếu trái cây đã chín ngọt. Rót ra ly lạnh, dùng ngay; không để ở nhiệt độ phòng quá lâu.',
    'DESSERT', 'EASY', 2, 10, 0, NULL, '/assets/recipe-phuc-01-sinh-to-dau-tam.png'
),
(
    N'Nước mơ gừng ngâm lạnh',
    N'Nước mơ có vị chua thơm, hậu ngọt dịu và mùi gừng ấm; pha loãng vừa phải để thức uống không gắt. Phần siro được nấu chín rồi làm lạnh trước khi pha, giúp vị ổn định và dễ điều chỉnh cho từng ly. Dùng lạnh cùng đá hoặc pha thêm nước ấm khi thời tiết mát.',
    N'Khẩu phần: khoảng 3 ly. Công thức dùng mơ tươi đã bỏ hạt; không dùng hạt mơ trong món.' + NCHAR(10) + NCHAR(10) +
    N'1. Rửa mơ, để ráo, bổ đôi và bỏ hạt. Gọt vỏ gừng nếu cần, thái lát mỏng để hương thoát đều.' + NCHAR(10) + NCHAR(10) +
    N'2. Cho mơ, gừng, nước và đường vào nồi đáy dày. Đun đến khi nước vừa sôi lăn tăn, khuấy cho đường tan hoàn toàn.' + NCHAR(10) + NCHAR(10) +
    N'3. Hạ lửa nhỏ, nấu 8–10 phút; không đun sôi mạnh để miếng mơ không nát và siro không đục. Khi mơ chuyển mềm trong nhưng vẫn giữ hình, tắt bếp.' + NCHAR(10) + NCHAR(10) +
    N'4. Nếm siro khi còn ấm, thêm nước nếu vị quá đậm. Để nguội tự nhiên, lọc bỏ gừng nếu muốn vị nhẹ, rồi bảo quản kín trong ngăn mát.' + NCHAR(10) + NCHAR(10) +
    N'5. Pha mỗi ly với khoảng một phần siro và ba phần nước lạnh; khuấy đều, nếm lại độ chua ngọt rồi thêm đá. Dùng trong vài ngày và luôn lấy siro bằng muỗng sạch.',
    'DESSERT', 'EASY', 3, 15, 10, NULL, '/assets/recipe-phuc-02-nuoc-mo-gung.png'
),
(
    N'Nước ép dưa lưới quýt thanh mát',
    N'Nước ép kết hợp dưa lưới mọng nước với vị thơm ngọt và chút chua nhẹ của quýt. Lọc vừa phải để thức uống vẫn có thân vị, không quá loãng; độ ngọt được cân chỉnh theo độ chín của trái cây. Ướp lạnh nguyên liệu trước khi ép để có thể dùng ngay mà không cần nhiều đá.',
    N'Khẩu phần: 2 ly. Chọn dưa lưới chín thơm nhưng thịt còn chắc, quýt mọng nước.' + NCHAR(10) + NCHAR(10) +
    N'1. Rửa sạch vỏ dưa và quýt trước khi bổ. Gọt dưa, bỏ hạt, cắt miếng vừa miệng máy ép; bóc quýt và bỏ hạt nếu có.' + NCHAR(10) + NCHAR(10) +
    N'2. Ép dưa lưới trước, sau đó ép quýt để nước quýt đẩy phần nước dưa còn lại trong máy. Nếu dùng máy xay, xay từng loại riêng với lượng nước nhỏ rồi lọc qua rây.' + NCHAR(10) + NCHAR(10) +
    N'3. Khuấy hai phần nước ép trong bình lạnh. Để yên khoảng một phút cho bọt nổi lên rồi hớt bọt nếu muốn bề mặt trong.' + NCHAR(10) + NCHAR(10) +
    N'4. Nếm trước khi thêm đường; nếu trái cây đã ngọt, giữ nguyên vị tự nhiên. Nếu muốn chua rõ hơn, tăng phần quýt thay vì thêm nhiều chất tạo ngọt.' + NCHAR(10) + NCHAR(10) +
    N'5. Rót vào ly lạnh và dùng ngay. Không để nước ép lâu ngoài không khí vì hương quýt giảm và màu có thể sẫm; phần còn lại đậy kín, giữ lạnh và dùng trong ngày.',
    'DESSERT', 'EASY', 2, 12, 0, NULL, '/assets/recipe-phuc-03-ep-dua-luoi-quyt.png'
),
(
    N'Sữa hạt kiều mạch chà là',
    N'Sữa thực vật thơm bùi từ kiều mạch, được làm ngọt nhẹ bằng chà là và giữ kết cấu mượt sau khi lọc. Hạt được ngâm rồi nấu chín trước khi xay để vị dịu hơn và thức uống không còn cảm giác bột sống. Dùng ấm hoặc làm lạnh, lắc đều trước khi rót.',
    N'Khẩu phần: khoảng 3 ly. Không dùng kiều mạch sống để xay trực tiếp.' + NCHAR(10) + NCHAR(10) +
    N'1. Nhặt bỏ hạt lỗi, rửa kiều mạch đến khi nước trong rồi ngâm 4–6 giờ trong ngăn mát. Chắt bỏ nước ngâm và rửa lại.' + NCHAR(10) + NCHAR(10) +
    N'2. Cho kiều mạch vào nồi với nước mới. Nấu sôi nhẹ 12–15 phút đến khi hạt mềm nhưng không nhão; để nguội bớt trước khi xay.' + NCHAR(10) + NCHAR(10) +
    N'3. Bỏ hạt chà là, cắt nhỏ phần thịt quả. Cho kiều mạch đã nấu, chà là và một phần nước ấm vào máy xay.' + NCHAR(10) + NCHAR(10) +
    N'4. Xay 60–90 giây đến khi hỗn hợp đồng nhất. Thêm nước từng ít một để đạt độ loãng mong muốn; không đổ quá nhiều ngay từ đầu.' + NCHAR(10) + NCHAR(10) +
    N'5. Lọc qua túi lọc sữa hạt hoặc rây mịn, ép bã nhẹ tay. Nếm và điều chỉnh bằng nước; chà là đã tạo độ ngọt nên không cần thêm nhiều đường.' + NCHAR(10) + NCHAR(10) +
    N'6. Đun sữa ở lửa nhỏ 3–5 phút, khuấy liên tục và không để sôi trào. Làm nguội nhanh, bảo quản kín trong ngăn mát và dùng trong 2 ngày; lắc đều trước khi uống.',
    'DESSERT', 'MEDIUM', 3, 20, 20, NULL, '/assets/recipe-phuc-04-sua-kieu-mach.png'
),
(
    N'Trà vải thanh trà lá dứa',
    N'Trà trái cây có hương lá dứa thoảng nhẹ, vị vải ngọt và thanh trà chua dịu. Nền nước lá dứa được nấu riêng rồi làm nguội trước khi phối với trái cây để giữ mùi tươi, không làm vải bị bở. Pha lạnh, thêm đá ngay trước khi dùng.',
    N'Khẩu phần: khoảng 3 ly. Điều chỉnh lượng đường theo độ ngọt của vải.' + NCHAR(10) + NCHAR(10) +
    N'1. Rửa sạch lá dứa, buộc lỏng thành bó. Gọt thanh trà, bỏ hạt và cắt miếng; bóc vải, bỏ hạt, giữ lại phần thịt quả.' + NCHAR(10) + NCHAR(10) +
    N'2. Đun nước với lá dứa ở lửa nhỏ 8 phút, không đun quá lâu để nước không có vị hăng. Tắt bếp, đậy nắp 5 phút rồi bỏ lá.' + NCHAR(10) + NCHAR(10) +
    N'3. Khi nước còn ấm, khuấy đường đến khi tan hoàn toàn. Để nguội rồi làm lạnh; không cho trái cây vào nước nóng.' + NCHAR(10) + NCHAR(10) +
    N'4. Nghiền nhẹ một phần vải và thanh trà để lấy hương, giữ lại phần còn nguyên miếng. Không nghiền nát hạt hoặc ép mạnh phần vỏ quả.' + NCHAR(10) + NCHAR(10) +
    N'5. Trộn trái cây với nước lá dứa đã lạnh, nếm lại độ chua ngọt. Ủ trong ngăn mát 20–30 phút để hương hòa quyện.' + NCHAR(10) + NCHAR(10) +
    N'6. Chia ra ly cùng đá, thêm miếng trái cây nguyên để tạo kết cấu. Dùng trong ngày và giữ phần chưa rót trong bình có nắp ở ngăn mát.',
    'DESSERT', 'EASY', 3, 15, 8, NULL, '/assets/recipe-phuc-05-tra-vai-thanh-tra.png'
),
(
    N'Chè hạt dền nhãn lá dứa',
    N'Chè hạt dền có hạt nhỏ mềm, nước chè trong và thơm nhẹ lá dứa; nhãn tươi được cho vào cuối để giữ độ mọng. Một ít dừa nạo tạo điểm bùi nhưng không lấn át vị trái cây. Món ngon khi dùng mát nhẹ hoặc ấm vừa.',
    N'Khẩu phần: 4 chén. Hạt dền cần được rửa kỹ và nấu đủ thời gian để chín mềm.' + NCHAR(10) + NCHAR(10) +
    N'1. Vo hạt dền trong rây mịn dưới vòi nước nhẹ, để ráo. Bóc nhãn, bỏ hạt và kiểm tra phần thịt quả.' + NCHAR(10) + NCHAR(10) +
    N'2. Buộc lá dứa thành bó. Đun nước với lá dứa 5 phút ở lửa nhỏ, vớt lá ra trước khi cho hạt vào.' + NCHAR(10) + NCHAR(10) +
    N'3. Rắc hạt dền từ từ vào nồi, khuấy đều để không vón đáy. Nấu liu riu 18–22 phút, khuấy định kỳ đến khi hạt trong và mềm.' + NCHAR(10) + NCHAR(10) +
    N'4. Thêm đường và một nhúm muối, nấu thêm 2 phút cho tan. Nếu chè đặc quá, thêm nước nóng từng ít một.' + NCHAR(10) + NCHAR(10) +
    N'5. Tắt bếp, cho nhãn vào khi chè còn nóng nhưng không sôi để thịt quả vừa ấm, không teo. Múc ra chén, rắc dừa nạo.' + NCHAR(10) + NCHAR(10) +
    N'6. Dùng ấm hoặc làm nguội rồi bảo quản kín trong ngăn mát. Khi chè lạnh đặc lại, khuấy với một ít nước trước khi dùng; không đun lại nhãn quá lâu.',
    'DESSERT', 'EASY', 4, 15, 25, NULL, '/assets/recipe-phuc-06-che-hat-den-nhan.png'
),
(
    N'Chè lúa mạch hạt sen',
    N'Chè lúa mạch nấu cùng hạt sen tạo vị bùi, mềm dẻo và nước chè thanh. Hạt sen được nấu gần mềm trước, lúa mạch cho vào sau để hai nguyên liệu chín đều mà không nát. Lá dứa và gừng tạo mùi thơm nhẹ, phù hợp dùng ấm hoặc ướp lạnh.',
    N'Khẩu phần: 4 chén. Nếu dùng hạt sen khô, ngâm trước theo hướng dẫn trên bao bì.' + NCHAR(10) + NCHAR(10) +
    N'1. Nhặt lúa mạch, rửa đến khi nước trong. Rửa hạt sen, bỏ tim sen nếu còn để hạn chế vị đắng.' + NCHAR(10) + NCHAR(10) +
    N'2. Cho hạt sen vào nồi nước mới, nấu lửa nhỏ 15 phút hoặc đến khi gần mềm. Hớt bọt để nước chè trong.' + NCHAR(10) + NCHAR(10) +
    N'3. Thêm lúa mạch đã rửa và vài lát gừng. Nấu liu riu 25–35 phút, khuấy nhẹ mỗi vài phút để hạt không dính đáy.' + NCHAR(10) + NCHAR(10) +
    N'4. Khi lúa mạch nở mềm nhưng vẫn giữ hạt, thêm đường và một nhúm muối. Nấu thêm 3 phút, nếm độ ngọt và chỉnh bằng nước nóng nếu chè quá đặc.' + NCHAR(10) + NCHAR(10) +
    N'5. Tắt bếp, đậy nắp 5 phút để hạt tiếp tục hút nước. Vớt gừng nếu muốn vị nhẹ hơn.' + NCHAR(10) + NCHAR(10) +
    N'6. Múc chè ra chén, dùng nóng hoặc để nguội rồi làm lạnh. Khi hâm lại, thêm nước và khuấy nhẹ để hạt không vỡ.',
    'DESSERT', 'MEDIUM', 4, 15, 40, NULL, '/assets/recipe-phuc-07-che-lua-mach-hat-sen.png'
),
(
    N'Chè củ năng mận hậu',
    N'Chè lạnh kết hợp củ năng giòn với mận hậu chua ngọt, tạo cảm giác tươi và nhẹ sau bữa ăn. Củ năng được nấu vừa chín để giữ độ giòn; mận chỉ cho vào khi nước chè đã nguội để giữ màu và hương quả. Dùng lạnh, có thể thêm đá khi phục vụ.',
    N'Khẩu phần: 4 chén. Rửa sạch vỏ củ năng trước khi gọt và bỏ phần dập.' + NCHAR(10) + NCHAR(10) +
    N'1. Gọt củ năng, cắt hạt lựu đều nhau rồi ngâm trong nước lạnh để không thâm. Rửa mận, bỏ hạt và cắt múi nhỏ.' + NCHAR(10) + NCHAR(10) +
    N'2. Đun nước với đường và một nhúm muối đến khi tan. Cho củ năng vào nấu 5–7 phút; dừng khi củ trong nhẹ nhưng vẫn còn độ giòn.' + NCHAR(10) + NCHAR(10) +
    N'3. Nếu muốn vị chua rõ, thêm một phần nhỏ mận vào nồi và nấu thêm 2 phút; không nấu toàn bộ mận vì miếng quả dễ nát.' + NCHAR(10) + NCHAR(10) +
    N'4. Tắt bếp, để nước chè nguội hoàn toàn. Nếm lại, thêm nước nếu siro quá đậm.' + NCHAR(10) + NCHAR(10) +
    N'5. Cho phần mận còn lại vào khi chè đã nguội, đậy kín và làm lạnh ít nhất 1 giờ để hương hòa quyện.' + NCHAR(10) + NCHAR(10) +
    N'6. Chia vào chén lạnh, dùng trong ngày để củ năng còn giòn. Không để món đã pha ngoài nhiệt độ phòng quá lâu.',
    'DESSERT', 'EASY', 4, 15, 10, NULL, '/assets/recipe-phuc-08-che-cu-nang-man.png'
),
(
    N'Sorbet chôm chôm kiwi',
    N'Sorbet trái cây không sữa có vị ngọt thanh của chôm chôm và chút chua tươi của kiwi. Trái cây được cấp đông thành lớp mỏng rồi xay ngắt quãng để thành phẩm mịn, không bị tảng đá cứng. Dùng ngay khi vừa xay hoặc cấp đông lại ngắn hạn để tạo viên.',
    N'Khẩu phần: 4 phần nhỏ. Cần chuẩn bị trước thời gian cấp đông trái cây.' + NCHAR(10) + NCHAR(10) +
    N'1. Bóc chôm chôm, bỏ hạt; gọt kiwi và cắt miếng. Thấm khô phần nước thừa để hỗn hợp không bị dăm đá.' + NCHAR(10) + NCHAR(10) +
    N'2. Trải trái cây thành một lớp trên khay có lót giấy, cấp đông 4–6 giờ đến khi đông cứng. Không xếp thành đống để máy xay xử lý đều.' + NCHAR(10) + NCHAR(10) +
    N'3. Cho trái cây đông lạnh vào máy xay công suất phù hợp, thêm đường và một ít nước. Xay theo nhịp ngắn, dừng máy để vét thành cối và tránh làm nóng lưỡi dao.' + NCHAR(10) + NCHAR(10) +
    N'4. Thêm nước từng thìa nhỏ chỉ khi máy không cuộn được trái cây. Xay đến khi hỗn hợp mịn nhưng còn lạnh; không xay liên tục quá lâu.' + NCHAR(10) + NCHAR(10) +
    N'5. Nếm và cân bằng vị bằng một ít chanh nếu cần. Dùng ngay dạng mềm hoặc cho vào hộp nông, dàn phẳng và cấp đông thêm 45–60 phút để múc viên.' + NCHAR(10) + NCHAR(10) +
    N'6. Nếu sorbet đông cứng, để ngoài 5–8 phút trước khi múc. Bảo quản kín và dùng trong vài ngày để hạn chế tinh thể đá lớn.',
    'DESSERT', 'MEDIUM', 4, 20, 0, NULL, '/assets/recipe-phuc-09-sorbet-chom-chom-kiwi.png'
),
(
    N'Kem mít dừa sữa đậu nành',
    N'Kem thuần chay tận dụng độ ngọt và hương thơm của mít chín, phối với sữa đậu nành và dừa nạo để tạo kết cấu béo nhẹ. Mít được cấp đông trước rồi xay với lượng sữa vừa đủ để hỗn hợp đặc, mịn. Ăn ngay dạng kem mềm hoặc làm đông thêm để múc viên.',
    N'Khẩu phần: 4 phần. Dùng mít chín thơm, bỏ hạt và xơ cứng.' + NCHAR(10) + NCHAR(10) +
    N'1. Cắt múi mít thành miếng nhỏ, trải trên khay và cấp đông ít nhất 4 giờ. Làm lạnh sữa đậu nành để hỗn hợp không tan nhanh khi xay.' + NCHAR(10) + NCHAR(10) +
    N'2. Cho mít đông lạnh, sữa đậu nành và một phần dừa nạo vào máy xay. Bắt đầu với lượng sữa ít để giữ kem đặc.' + NCHAR(10) + NCHAR(10) +
    N'3. Xay theo nhịp ngắn, dùng que vét nếu cần và chỉ thêm sữa từng ít một. Dừng khi hỗn hợp mịn, đặc và có thể giữ vệt xoáy.' + NCHAR(10) + NCHAR(10) +
    N'4. Nếm độ ngọt; nếu mít đã chín ngọt thì không thêm đường. Trộn phần dừa nạo còn lại bằng thìa để giữ chút kết cấu.' + NCHAR(10) + NCHAR(10) +
    N'5. Múc dùng ngay dạng kem mềm. Nếu muốn viên cứng hơn, dàn vào hộp nông, đậy kín và cấp đông thêm 1–2 giờ.' + NCHAR(10) + NCHAR(10) +
    N'6. Trước khi múc kem đã đông, để hộp ở ngăn mát 10 phút. Bảo quản kín và dùng sớm để hạn chế dăm đá.',
    'DESSERT', 'EASY', 4, 15, 0, NULL, '/assets/recipe-phuc-10-kem-mit-dua.png'
),
(
    N'Bánh chuối hấp nước cốt dừa lá dứa',
    N'Bánh chuối hấp mềm dẻo, thơm mùi lá dứa và có lớp sốt dừa nạo sánh nhẹ. Bột được trộn vừa đủ để bao trái chuối, hấp bằng hơi ổn định đến khi mặt bánh trong và phần giữa chín. Ăn ấm hoặc để nguội, cắt miếng bằng dao ướt để mặt bánh gọn.',
    N'Khẩu phần: 6 miếng. Hấp bánh bằng khuôn chịu nhiệt và làm nóng xửng trước.' + NCHAR(10) + NCHAR(10) +
    N'1. Bóc chuối sứ, cắt lát dày khoảng 1 cm. Buộc lá dứa thành bó; chuẩn bị khuôn và quét một lớp dầu mỏng để chống dính.' + NCHAR(10) + NCHAR(10) +
    N'2. Khuấy bột năng với nước, đường và muối đến khi không còn vón. Để bột nghỉ 10 phút để bột hút nước đều.' + NCHAR(10) + NCHAR(10) +
    N'3. Xếp chuối thành một lớp trong khuôn, rót hỗn hợp bột phủ đều. Gõ nhẹ khuôn cho bọt khí lớn thoát ra.' + NCHAR(10) + NCHAR(10) +
    N'4. Đặt khuôn vào xửng nước đang sôi, phủ khăn sạch dưới nắp để nước ngưng không nhỏ lên mặt bánh. Hấp lửa vừa 30–35 phút.' + NCHAR(10) + NCHAR(10) +
    N'5. Kiểm tra bằng que tre ở giữa bánh: que rút ra không còn bột lỏng, mặt bánh trong và đàn hồi nhẹ. Nếu còn bột ướt, hấp thêm 5 phút rồi kiểm tra lại.' + NCHAR(10) + NCHAR(10) +
    N'6. Nấu nước với dừa nạo và lá dứa ở lửa nhỏ 8–10 phút, khuấy đều; nêm đường và muối. Hòa một ít bột năng với nước lạnh, rót từ từ vào nồi đến khi sốt phủ được mặt thìa.' + NCHAR(10) + NCHAR(10) +
    N'7. Để bánh nghỉ 15 phút trước khi lấy khỏi khuôn. Cắt bằng dao làm ướt, rưới sốt dừa ấm và dùng trong ngày.',
    'DESSERT', 'MEDIUM', 6, 20, 40, NULL, '/assets/recipe-phuc-11-banh-chuoi-hap.png'
),
(
    N'Bánh khoai mỡ hấp lá dứa',
    N'Bánh khoai mỡ có màu tím tự nhiên, ruột mềm dẻo và mùi lá dứa dịu. Khoai được hấp chín rồi nghiền mịn trước khi phối bột, giúp khối bánh đồng đều và không còn miếng sống. Hấp lửa vừa để bánh chín sâu mà mặt bánh không rỗ.',
    N'Khẩu phần: 6 miếng. Hấp khoai đến mềm hoàn toàn trước khi nghiền.' + NCHAR(10) + NCHAR(10) +
    N'1. Gọt khoai mỡ, rửa sạch và cắt khối đều nhau. Rửa lá dứa rồi buộc thành bó.' + NCHAR(10) + NCHAR(10) +
    N'2. Hấp khoai 18–22 phút đến khi xiên que xuyên dễ dàng. Để khoai thoát hơi 5 phút, sau đó nghiền khi còn ấm đến khi mịn.' + NCHAR(10) + NCHAR(10) +
    N'3. Trộn khoai với bột năng, bột bắp, đường, muối và nước từng ít một. Dừng thêm nước khi hỗn hợp dẻo, đặc và rơi thành dải chậm khỏi phới.' + NCHAR(10) + NCHAR(10) +
    N'4. Quét dầu mỏng vào khuôn, rót bột và dàn phẳng. Gõ nhẹ khuôn để loại bọt khí lớn.' + NCHAR(10) + NCHAR(10) +
    N'5. Đun sôi nước xửng, cho lá dứa vào phần nước tạo hương. Hấp khuôn ở lửa vừa 30–35 phút; giữ nắp kín nhưng không để nước sôi trào vào khuôn.' + NCHAR(10) + NCHAR(10) +
    N'6. Thử phần giữa bằng que tre. Khi que sạch, bánh đàn hồi nhẹ và không còn bột ướt, tắt bếp; nếu chưa đạt, hấp tiếp từng 5 phút.' + NCHAR(10) + NCHAR(10) +
    N'7. Để bánh nguội 15 phút rồi mới cắt. Dùng ấm hoặc để nguội hoàn toàn, bảo quản kín trong ngăn mát và hấp lại nhẹ trước khi ăn.',
    'DESSERT', 'MEDIUM', 6, 20, 40, NULL, '/assets/recipe-phuc-12-banh-khoai-mo.png'
),
(
    N'Bánh kếp kiều mạch hạnh nhân',
    N'Bánh kếp thuần chay có vị bùi của kiều mạch và hạnh nhân, mềm ở giữa với viền áp chảo vàng nhẹ. Chuối nghiền giúp kết dính bột và tạo vị ngọt dịu, không cần trứng. Nấu bánh nhỏ ở lửa vừa thấp để phần giữa chín trước khi mặt ngoài sậm màu.',
    N'Khẩu phần: 6 bánh nhỏ. Dùng chảo chống dính và để bột nghỉ trước khi áp chảo.' + NCHAR(10) + NCHAR(10) +
    N'1. Rửa kiều mạch, để ráo rồi rang khô trên chảo 3–4 phút đến khi thơm. Để nguội và xay thành bột mịn; rây bỏ phần hạt còn thô.' + NCHAR(10) + NCHAR(10) +
    N'2. Nghiền chuối sứ thật mịn trong tô. Thêm bột kiều mạch, bột hạnh nhân, sữa đậu nành, một nhúm muối và khuấy đến khi vừa hòa quyện.' + NCHAR(10) + NCHAR(10) +
    N'3. Để bột nghỉ 10 phút cho bột hút ẩm. Nếu bột quá đặc, thêm sữa từng thìa; bột cần chảy chậm khỏi muỗng chứ không loãng như nước.' + NCHAR(10) + NCHAR(10) +
    N'4. Làm nóng chảo ở lửa vừa thấp, quét một lớp dầu thật mỏng. Múc bột thành bánh đường kính khoảng 8 cm, không dàn quá mỏng.' + NCHAR(10) + NCHAR(10) +
    N'5. Nấu 2–3 phút đến khi mặt bánh xuất hiện lỗ nhỏ và viền se lại. Lật nhẹ, áp chảo thêm 1–2 phút đến khi hai mặt vàng và giữa bánh không còn bột ướt.' + NCHAR(10) + NCHAR(10) +
    N'6. Làm lần lượt, hạ lửa nếu bánh sẫm nhanh. Xếp bánh lên giá để hơi thoát, dùng ấm với trái cây tươi hoặc bảo quản lạnh tối đa một ngày.',
    'DESSERT', 'MEDIUM', 3, 15, 20, NULL, '/assets/recipe-phuc-13-banh-kep-kieu-mach.png'
),
(
    N'Bánh bắp hấp lá dứa',
    N'Bánh bắp hấp có màu vàng tươi, vị ngô ngọt và mùi lá dứa nhẹ. Bột bắp phối cùng bột năng để bánh mềm nhưng vẫn cắt được thành miếng; hạt ngô được trộn cuối để giữ độ giòn mọng. Hấp ở lửa vừa giúp bánh chín đều, mặt bánh không lõm.',
    N'Khẩu phần: 6 miếng. Làm nóng xửng trước và để khuôn thoát hơi tốt.' + NCHAR(10) + NCHAR(10) +
    N'1. Rửa lá dứa, buộc thành bó. Tách một nửa ngô hạt để xay, giữ nửa còn lại nguyên hạt.' + NCHAR(10) + NCHAR(10) +
    N'2. Xay phần ngô đầu với sữa đậu nành đến khi mịn vừa; không cần lọc hết bã để giữ vị ngô.' + NCHAR(10) + NCHAR(10) +
    N'3. Trộn bột bắp, bột năng, đường và muối trong tô. Rót hỗn hợp ngô xay vào từ từ, khuấy đến khi bột đồng nhất, không còn cục khô.' + NCHAR(10) + NCHAR(10) +
    N'4. Thêm ngô hạt còn lại và dầu ăn, đảo nhẹ. Để bột nghỉ 8 phút rồi khuấy lại một lần trước khi rót vào khuôn đã quét dầu.' + NCHAR(10) + NCHAR(10) +
    N'5. Đặt khuôn vào xửng đang có hơi nước mạnh, đặt bó lá dứa ở phần nước. Hấp lửa vừa 30–35 phút; phủ khăn dưới nắp để nước không nhỏ xuống bánh.' + NCHAR(10) + NCHAR(10) +
    N'6. Xiên que vào giữa bánh; nếu que không dính bột lỏng, mặt bánh đàn hồi và hạt ngô chín mềm là đạt. Hấp thêm 5 phút nếu phần giữa còn ướt.' + NCHAR(10) + NCHAR(10) +
    N'7. Để bánh nguội trong khuôn 15 phút rồi lấy ra. Cắt bằng dao ướt, dùng trong ngày hoặc đậy kín và bảo quản lạnh.',
    'DESSERT', 'MEDIUM', 6, 15, 40, NULL, '/assets/recipe-phuc-14-banh-bap-hap.png'
),
(
    N'Bánh khoai môn hấp dừa nạo',
    N'Bánh khoai môn hấp có ruột mềm bùi, điểm dừa nạo thơm và lớp bột dẻo vừa phải để giữ miếng. Khoai được hấp chín trước khi trộn, còn bột được khuấy tan riêng để tránh vón. Món dùng ấm hoặc để nguội cắt lát, hợp làm món ngọt nhẹ trong ngày.',
    N'Khẩu phần: 6 miếng. Hấp khoai trước và kiểm tra khuôn vừa với xửng.' + NCHAR(10) + NCHAR(10) +
    N'1. Gọt khoai môn, rửa sạch và cắt khối 2 cm. Hấp 15–18 phút đến khi mềm nhưng chưa bở; để nguội bớt rồi nghiền thô, giữ lại một ít miếng nhỏ.' + NCHAR(10) + NCHAR(10) +
    N'2. Hòa bột gạo và bột năng với nước, khuấy đến khi mịn. Thêm đường, muối và dừa nạo, trộn đều.' + NCHAR(10) + NCHAR(10) +
    N'3. Cho khoai nghiền vào hỗn hợp bột, đảo nhẹ để phân bố đều mà không làm nát hết các miếng khoai.' + NCHAR(10) + NCHAR(10) +
    N'4. Quét dầu mỏng vào khuôn, rót bột và dàn mặt. Đặt khuôn vào xửng nước sôi, phủ khăn dưới nắp để mặt bánh không bị đọng nước.' + NCHAR(10) + NCHAR(10) +
    N'5. Hấp lửa vừa 35–40 phút. Sau 30 phút, thử que tre ở giữa; nếu còn bột lỏng thì hấp tiếp từng 5 phút.' + NCHAR(10) + NCHAR(10) +
    N'6. Khi bánh chắc, que rút ra sạch và mặt bánh hơi trong, tắt bếp. Để bánh nghỉ 15–20 phút trong khuôn để cấu trúc ổn định.' + NCHAR(10) + NCHAR(10) +
    N'7. Cắt miếng bằng dao làm ướt, rắc thêm dừa nạo nếu muốn. Bảo quản kín trong ngăn mát và hấp lại nhẹ để bánh mềm trước khi dùng.',
    'DESSERT', 'MEDIUM', 6, 20, 45, NULL, '/assets/recipe-phuc-15-banh-khoai-mon-hap.png'
);

IF (SELECT COUNT(*) FROM @recipes) <> 15
    THROW 51079, 'The Nguyen Hoang Phuc dessert collection must contain exactly 15 recipes.', 1;

DECLARE @ingredients TABLE (
    title NVARCHAR(120) NOT NULL,
    ingredient_name NVARCHAR(200) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (title, ingredient_name)
);

INSERT INTO @ingredients (title, ingredient_name, quantity) VALUES
(N'Sinh tố dâu tằm chuối sứ sữa đậu nành', N'Dâu tằm', 220),
(N'Sinh tố dâu tằm chuối sứ sữa đậu nành', N'Chuối sứ', 160),
(N'Sinh tố dâu tằm chuối sứ sữa đậu nành', N'Sữa đậu nành', 360),
(N'Sinh tố dâu tằm chuối sứ sữa đậu nành', N'Chà là', 20),
(N'Nước mơ gừng ngâm lạnh', N'Mơ', 180),
(N'Nước mơ gừng ngâm lạnh', N'Gừng', 12),
(N'Nước mơ gừng ngâm lạnh', N'Nước', 900),
(N'Nước mơ gừng ngâm lạnh', N'Đường ăn', 90),
(N'Nước mơ gừng ngâm lạnh', N'Muối ăn', 1),
(N'Nước ép dưa lưới quýt thanh mát', N'Dưa lưới', 360),
(N'Nước ép dưa lưới quýt thanh mát', N'Quýt', 180),
(N'Nước ép dưa lưới quýt thanh mát', N'Chanh', 12),
(N'Nước ép dưa lưới quýt thanh mát', N'Nước', 120),
(N'Sữa hạt kiều mạch chà là', N'Kiều mạch', 100),
(N'Sữa hạt kiều mạch chà là', N'Chà là', 35),
(N'Sữa hạt kiều mạch chà là', N'Nước', 900),
(N'Sữa hạt kiều mạch chà là', N'Muối ăn', 1),
(N'Trà vải thanh trà lá dứa', N'Vải', 180),
(N'Trà vải thanh trà lá dứa', N'Thanh trà', 120),
(N'Trà vải thanh trà lá dứa', N'Lá dứa', 8),
(N'Trà vải thanh trà lá dứa', N'Nước', 750),
(N'Trà vải thanh trà lá dứa', N'Đường ăn', 30),
(N'Chè hạt dền nhãn lá dứa', N'Hạt dền', 100),
(N'Chè hạt dền nhãn lá dứa', N'Nhãn', 160),
(N'Chè hạt dền nhãn lá dứa', N'Lá dứa', 8),
(N'Chè hạt dền nhãn lá dứa', N'Dừa nạo', 40),
(N'Chè hạt dền nhãn lá dứa', N'Nước', 1200),
(N'Chè hạt dền nhãn lá dứa', N'Đường ăn', 65),
(N'Chè hạt dền nhãn lá dứa', N'Muối ăn', 1),
(N'Chè lúa mạch hạt sen', N'Lúa mạch', 120),
(N'Chè lúa mạch hạt sen', N'Hạt sen', 120),
(N'Chè lúa mạch hạt sen', N'Gừng', 8),
(N'Chè lúa mạch hạt sen', N'Nước', 1400),
(N'Chè lúa mạch hạt sen', N'Đường ăn', 60),
(N'Chè lúa mạch hạt sen', N'Muối ăn', 1),
(N'Chè củ năng mận hậu', N'Củ năng', 280),
(N'Chè củ năng mận hậu', N'Mận hậu', 180),
(N'Chè củ năng mận hậu', N'Nước', 900),
(N'Chè củ năng mận hậu', N'Đường ăn', 55),
(N'Chè củ năng mận hậu', N'Chanh', 10),
(N'Sorbet chôm chôm kiwi', N'Chôm chôm', 260),
(N'Sorbet chôm chôm kiwi', N'Kiwi', 180),
(N'Sorbet chôm chôm kiwi', N'Đường ăn', 25),
(N'Sorbet chôm chôm kiwi', N'Chanh', 10),
(N'Sorbet chôm chôm kiwi', N'Nước', 60),
(N'Sorbet chôm chôm kiwi', N'Muối ăn', 1),
(N'Kem mít dừa sữa đậu nành', N'Mít', 280),
(N'Kem mít dừa sữa đậu nành', N'Dừa nạo', 55),
(N'Kem mít dừa sữa đậu nành', N'Sữa đậu nành', 220),
(N'Kem mít dừa sữa đậu nành', N'Chà là', 25),
(N'Bánh chuối hấp nước cốt dừa lá dứa', N'Chuối sứ', 420),
(N'Bánh chuối hấp nước cốt dừa lá dứa', N'Bột năng', 120),
(N'Bánh chuối hấp nước cốt dừa lá dứa', N'Nước', 180),
(N'Bánh chuối hấp nước cốt dừa lá dứa', N'Đường ăn', 55),
(N'Bánh chuối hấp nước cốt dừa lá dứa', N'Muối ăn', 2),
(N'Bánh chuối hấp nước cốt dừa lá dứa', N'Dừa nạo', 100),
(N'Bánh chuối hấp nước cốt dừa lá dứa', N'Lá dứa', 8),
(N'Bánh khoai mỡ hấp lá dứa', N'Khoai mỡ', 420),
(N'Bánh khoai mỡ hấp lá dứa', N'Bột năng', 90),
(N'Bánh khoai mỡ hấp lá dứa', N'Bột bắp', 35),
(N'Bánh khoai mỡ hấp lá dứa', N'Đường ăn', 60),
(N'Bánh khoai mỡ hấp lá dứa', N'Muối ăn', 2),
(N'Bánh khoai mỡ hấp lá dứa', N'Nước', 140),
(N'Bánh khoai mỡ hấp lá dứa', N'Lá dứa', 8),
(N'Bánh kếp kiều mạch hạnh nhân', N'Kiều mạch', 100),
(N'Bánh kếp kiều mạch hạnh nhân', N'Bột hạnh nhân', 80),
(N'Bánh kếp kiều mạch hạnh nhân', N'Chuối sứ', 130),
(N'Bánh kếp kiều mạch hạnh nhân', N'Sữa đậu nành', 180),
(N'Bánh kếp kiều mạch hạnh nhân', N'Dầu ăn', 8),
(N'Bánh kếp kiều mạch hạnh nhân', N'Muối ăn', 1),
(N'Bánh bắp hấp lá dứa', N'Bột bắp', 150),
(N'Bánh bắp hấp lá dứa', N'Bột năng', 45),
(N'Bánh bắp hấp lá dứa', N'Ngô hạt', 160),
(N'Bánh bắp hấp lá dứa', N'Sữa đậu nành', 150),
(N'Bánh bắp hấp lá dứa', N'Nước', 180),
(N'Bánh bắp hấp lá dứa', N'Đường ăn', 55),
(N'Bánh bắp hấp lá dứa', N'Muối ăn', 2),
(N'Bánh bắp hấp lá dứa', N'Lá dứa', 8),
(N'Bánh khoai môn hấp dừa nạo', N'Khoai môn', 380),
(N'Bánh khoai môn hấp dừa nạo', N'Bột gạo', 90),
(N'Bánh khoai môn hấp dừa nạo', N'Bột năng', 55),
(N'Bánh khoai môn hấp dừa nạo', N'Dừa nạo', 55),
(N'Bánh khoai môn hấp dừa nạo', N'Nước', 170),
(N'Bánh khoai môn hấp dừa nạo', N'Đường ăn', 55),
(N'Bánh khoai môn hấp dừa nạo', N'Muối ăn', 2);

IF EXISTS (
    SELECT 1
    FROM @recipes recipe
    LEFT JOIN @ingredients ingredient ON ingredient.title = recipe.title
    WHERE ingredient.title IS NULL
)
    THROW 51080, 'Every Nguyen Hoang Phuc recipe must have at least one ingredient.', 1;

IF EXISTS (
    SELECT 1
    FROM @ingredients seed
    LEFT JOIN [INGREDIENT] catalog
      ON catalog.name = seed.ingredient_name AND catalog.status = 'ACTIVE'
    WHERE catalog.ingredient_id IS NULL
)
    THROW 51081, 'A catalog ingredient required by Nguyen Hoang Phuc dessert collection is missing or inactive.', 1;

-- Preserve any biography already entered; initialize only an empty local demo profile.
UPDATE [USER]
SET bio = N'Mình yêu thích những món tráng miệng và thức uống chay làm từ trái cây, ngũ cốc và nguyên liệu theo mùa. Mỗi công thức đều ghi rõ định lượng, dấu hiệu món đạt và cách bảo quản để bạn dễ thực hiện tại nhà.',
    updated_at = SYSUTCDATETIME()
WHERE user_id = @author_id AND (bio IS NULL OR LEN(LTRIM(RTRIM(bio))) = 0);

INSERT INTO [RECIPE_POST] (
    author_id, title, description, instructions, dish_category, vegetarian_type,
    difficulty, servings, prep_time_min, cook_time_min, youtube_url, published_at
)
SELECT @author_id, seed.title, seed.description, seed.instructions, seed.category, 'VEGAN',
       seed.difficulty, seed.servings, seed.prep, seed.cook, seed.youtube_url, SYSUTCDATETIME()
FROM @recipes seed
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_POST] existing
    WHERE existing.author_id = @author_id AND existing.title = seed.title
);

INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT post.recipe_id, catalog.ingredient_id, @gram, seed.quantity
FROM @ingredients seed
JOIN [INGREDIENT] catalog
  ON catalog.name = seed.ingredient_name AND catalog.status = 'ACTIVE'
JOIN [RECIPE_POST] post
  ON post.author_id = @author_id AND post.title = seed.title
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_INGREDIENT] existing
    WHERE existing.recipe_id = post.recipe_id AND existing.ingredient_id = catalog.ingredient_id
);

-- Add one generated cover per post while preserving any media already curated locally.
INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT post.recipe_id, seed.image_url, 'image/png', 1, 1
FROM @recipes seed
JOIN [RECIPE_POST] post
  ON post.author_id = @author_id AND post.title = seed.title
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_MEDIA] media WHERE media.recipe_id = post.recipe_id
);

COMMIT TRANSACTION;
