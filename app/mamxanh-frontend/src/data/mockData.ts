import type { Author, Post, Recipe, ShoppingListItem, DayPlan } from '../types';

const A = '/assets';

// --- Authors ---------------------------------------------------------------
export const authors: Record<string, Author> = {
  hoangTam: { id: 'u1', name: 'Lương Bếp Chay Tâm An', avatar: `${A}/f680c.png`, bio: 'Chuyên gia ẩm thực dưỡng sinh thuần thực vật', verified: true },
  bepLan: { id: 'u2', name: 'Bếp Chay Lan', avatar: `${A}/1a85b.png`, verified: true },
  thoMoc: { id: 'u3', name: 'Thô Mộc Kitchen', avatar: `${A}/7478c.png` },
  haNhi: { id: 'u4', name: 'Hà Nhi Foodie', avatar: `${A}/f2393.png`, verified: true },
  chuBa: { id: 'u5', name: 'Bếp Chú Ba', avatar: `${A}/62df0.png` },
  tuAn: { id: 'u6', name: 'Tú An Healthy', avatar: `${A}/be2c9.png` },
  quangHuy: { id: 'u7', name: 'Quang Huy', avatar: `${A}/b8638.png` },
  coBay: { id: 'u8', name: 'Cô Bảy An Lạc', avatar: `${A}/405d1.png`, verified: true },
};

export const currentUser: Author = {
  id: 'me',
  name: 'Lan Anh',
  avatar: `${A}/96011.png`,
  bio: 'Đang theo đuổi lối sống chay thanh đạm 🌿',
};

// UI-only placeholder until account/subscription data is supplied by the backend.
export const demoAiPlan = 'FREE';

// --- Recipes ---------------------------------------------------------------
const dietTagOptions = ['Thuần Chay', 'Lacto', 'Ovo', 'Lacto-Ovo'] as const;

export const recipes: Recipe[] = [
  {
    id: 'r1',
    slug: 'dau-hu-non-sot-nam-dong-co',
    name: 'Đậu Hũ Non Sốt Nấm Đông Cô Tiêu Xanh',
    image: `${A}/95427.png`,
    description:
      'Đậu hũ non mềm mượt tan trong miệng, quyện cùng sốt nấm đông cô đậm đà và tiêu xanh thơm nồng. Món chay dân dã mà tinh tế, đủ đạm thực vật cho bữa cơm gia đình.',
    prepTime: 15,
    cookTime: 20,
    servings: 4,
    calories: 210,
    difficulty: 'Dễ',
    diet: 'Thuần Chay',
    category: 'Món mặn',
    rating: 4.9,
    reviews: 328,
    author: authors.hoangTam,
    ingredients: [
      { id: 'i1', group: 'Đạm thực vật', name: 'Đậu hũ non Nhật Bản', quantity: '300 g', note: 'loại mềm mượt' },
      { id: 'i2', group: 'Rau củ quả tươi', name: 'Nấm đông cô tươi', quantity: '150 g', note: 'chần sơ, cắt lát' },
      { id: 'i3', group: 'Rau củ quả tươi', name: 'Hành boa-rô', quantity: '30 g' },
      { id: 'i4', group: 'Gia vị & Tinh dầu hạt', name: 'Tiêu xanh Phú Quốc', quantity: '2 nhánh' },
      { id: 'i5', group: 'Gia vị & Tinh dầu hạt', name: 'Nước tương Tamari', quantity: '2 muỗng canh' },
      { id: 'i6', group: 'Gia vị & Tinh dầu hạt', name: 'Dầu hào chay', quantity: '1.5 muỗng canh' },
      { id: 'i7', group: 'Gia vị & Tinh dầu hạt', name: 'Bột năng', quantity: '1 muỗng cà phê', note: 'hoà nước tạo độ sánh' },
      { id: 'i8', group: 'Gia vị & Tinh dầu hạt', name: 'Dầu mè nguyên chất', quantity: '1 muỗng cà phê' },
    ],
    steps: [
      'Đậu hũ non cắt khối vuông vừa ăn, đặt nhẹ nhàng ra đĩa để ráo nước.',
      'Phi thơm hành boa-rô với chút dầu mè, cho nấm đông cô vào xào săn.',
      'Thêm nước tương Tamari, dầu hào chay và 100ml nước, đun sôi lăn tăn 3 phút.',
      'Cho bột năng đã hoà nước vào khuấy đều đến khi sốt sánh mịn.',
      'Nhẹ nhàng cho đậu hũ non vào, rưới sốt lên, đun liu riu 2 phút.',
      'Rắc tiêu xanh, tắt bếp, dùng nóng cùng cơm gạo lứt.',
    ],
    tags: ['#anthuộcduong', '#dạmthựcvật', '#songlanh'],
  },
  {
    id: 'r2',
    slug: 'salad-quinoa-bo-sap',
    name: 'Salad Quinoa Bơ Sáp',
    image: `${A}/cddd4.png`,
    description: 'Salad quinoa giàu chất xơ, bơ sáp béo ngậy cùng sốt chanh dây thanh mát — bữa trưa nhẹ mà đủ năng lượng.',
    prepTime: 10, cookTime: 15, servings: 2, calories: 320, difficulty: 'Dễ', diet: 'Thuần Chay',
    category: 'Salad', rating: 4.8, reviews: 156, author: authors.tuAn,
    ingredients: [
      { id: 'i1', group: 'Ngũ cốc', name: 'Hạt quinoa', quantity: '120 g' },
      { id: 'i2', group: 'Rau củ quả tươi', name: 'Bơ sáp Đắk Lắk', quantity: '1 quả' },
      { id: 'i3', group: 'Rau củ quả tươi', name: 'Cà chua bi', quantity: '100 g' },
    ],
    steps: ['Nấu quinoa chín tơi, để nguội.', 'Trộn cùng bơ, cà chua và sốt chanh dây.', 'Rắc hạt điều rang, dùng liền.'],
    tags: ['#saladchay', '#eatclean'],
  },
  {
    id: 'r3',
    slug: 'pho-chay-nam-huong-rung',
    name: 'Phở Chay Nấm Hương Rừng',
    image: `${A}/f2961.png`,
    description: 'Nước dùng phở chay ngọt thanh từ rau củ và nấm hương rừng, sợi phở mềm dai, thơm lừng quế hồi.',
    prepTime: 20, cookTime: 40, servings: 3, calories: 280, difficulty: 'Trung bình', diet: 'Thuần Chay',
    category: 'Món nước', rating: 4.9, reviews: 421, author: authors.bepLan,
    ingredients: [
      { id: 'i1', group: 'Rau củ quả tươi', name: 'Nấm hương rừng', quantity: '80 g' },
      { id: 'i2', group: 'Ngũ cốc', name: 'Bánh phở tươi', quantity: '400 g' },
      { id: 'i3', group: 'Gia vị & Tinh dầu hạt', name: 'Quế, hồi, thảo quả', quantity: '1 gói' },
    ],
    steps: ['Nướng thơm gừng, hành và gia vị.', 'Hầm rau củ 40 phút lấy nước ngọt.', 'Chần bánh phở, xếp topping, chan nước dùng.'],
    tags: ['#phochay', '#monnuoc'],
  },
  {
    id: 'r4',
    slug: 'goi-cuon-chay-cu-vang',
    name: 'Gỏi Cuốn Chay Củ Vàng Sốt Đậu Phộng',
    image: `${A}/09723.png`,
    description: 'Gỏi cuốn tươi mát với rau củ theo mùa, chấm sốt đậu phộng béo bùi — món khai vị thanh nhẹ.',
    prepTime: 25, cookTime: 0, servings: 4, calories: 190, difficulty: 'Dễ', diet: 'Thuần Chay',
    category: 'Khai vị', rating: 4.7, reviews: 210, author: authors.haNhi,
    ingredients: [
      { id: 'i1', group: 'Rau củ quả tươi', name: 'Cà rốt, dưa leo, xà lách', quantity: '300 g' },
      { id: 'i2', group: 'Ngũ cốc', name: 'Bánh tráng cuốn', quantity: '12 cái' },
      { id: 'i3', group: 'Gia vị & Tinh dầu hạt', name: 'Bơ đậu phộng', quantity: '3 muỗng canh' },
    ],
    steps: ['Sơ chế rau củ thái sợi.', 'Cuốn bánh tráng với rau củ và bún.', 'Pha sốt đậu phộng, dùng kèm.'],
    tags: ['#goicuon', '#khaivi'],
  },
  {
    id: 'r5',
    slug: 'ca-ri-hat-dieu-bi-do',
    name: 'Cà Ri Hạt Điều Bí Đỏ Nắng',
    image: `${A}/5a53c.png`,
    description: 'Cà ri chay béo ngậy nước cốt dừa, bí đỏ bùi ngọt và hạt điều giòn — ấm bụng ngày se lạnh.',
    prepTime: 20, cookTime: 35, servings: 4, calories: 360, difficulty: 'Trung bình', diet: 'Lacto',
    category: 'Món mặn', rating: 4.8, reviews: 189, author: authors.chuBa,
    ingredients: [
      { id: 'i1', group: 'Rau củ quả tươi', name: 'Bí đỏ', quantity: '400 g' },
      { id: 'i2', group: 'Gia vị & Tinh dầu hạt', name: 'Hạt điều rang', quantity: '80 g' },
      { id: 'i3', group: 'Gia vị & Tinh dầu hạt', name: 'Nước cốt dừa', quantity: '200 ml' },
    ],
    steps: ['Xào thơm bột cà ri.', 'Cho bí đỏ, khoai và nước cốt dừa hầm mềm.', 'Thêm hạt điều, nêm vừa ăn.'],
    tags: ['#cari', '#monmua'],
  },
  {
    id: 'r6',
    slug: 'canh-rong-bien-hat-sen',
    name: 'Canh Rong Biển Hạt Sen Thanh Mát',
    image: `${A}/40053.png`,
    description: 'Canh rong biển hạt sen thanh nhiệt, ngọt tự nhiên từ củ quả — giải nhiệt ngày oi bức.',
    prepTime: 15, cookTime: 25, servings: 4, calories: 120, difficulty: 'Dễ', diet: 'Thuần Chay',
    category: 'Món canh', rating: 4.6, reviews: 98, author: authors.coBay,
    ingredients: [
      { id: 'i1', group: 'Rau củ quả tươi', name: 'Rong biển khô', quantity: '20 g' },
      { id: 'i2', group: 'Đạm thực vật', name: 'Hạt sen tươi', quantity: '150 g' },
    ],
    steps: ['Ngâm nở rong biển.', 'Hầm hạt sen mềm.', 'Cho rong biển, nêm nhạt, tắt bếp.'],
    tags: ['#canhchay', '#thanhnhiet'],
  },
  {
    id: 'r7',
    slug: 'banh-mi-sourdough-nhan-dau',
    name: 'Bánh Mì Sourdough Trứng Nhân Đậu Bơ',
    image: `${A}/8ef48.png`,
    description: 'Bánh mì sourdough lên men tự nhiên, kẹp trứng và sốt bơ — bữa sáng giàu năng lượng.',
    prepTime: 10, cookTime: 10, servings: 2, calories: 340, difficulty: 'Dễ', diet: 'Ovo',
    category: 'Bữa sáng', rating: 4.7, reviews: 143, author: authors.quangHuy,
    ingredients: [
      { id: 'i1', group: 'Ngũ cốc', name: 'Bánh mì sourdough', quantity: '2 lát' },
      { id: 'i2', group: 'Đạm thực vật', name: 'Trứng gà', quantity: '2 quả' },
    ],
    steps: ['Nướng giòn bánh mì.', 'Ốp trứng lòng đào.', 'Phết sốt bơ, kẹp và dùng.'],
    tags: ['#buasang', '#sourdough'],
  },
  {
    id: 'r8',
    slug: 'tau-hu-nuoc-duong-gung',
    name: 'Tàu Hũ Nước Đường Gừng Lá Dứa',
    image: `${A}/40c2c.png`,
    description: 'Tàu hũ mềm mịn chan nước đường gừng ấm nồng, thơm lá dứa — món tráng miệng dân dã.',
    prepTime: 30, cookTime: 15, servings: 4, calories: 160, difficulty: 'Trung bình', diet: 'Thuần Chay',
    category: 'Tráng miệng', rating: 4.9, reviews: 267, author: authors.thoMoc,
    ingredients: [
      { id: 'i1', group: 'Đạm thực vật', name: 'Sữa đậu nành', quantity: '1 L' },
      { id: 'i2', group: 'Gia vị & Tinh dầu hạt', name: 'Đường thốt nốt, gừng, lá dứa', quantity: '1 phần' },
    ],
    steps: ['Làm đông tàu hũ với bột gelatin chay.', 'Nấu nước đường gừng lá dứa.', 'Múc tàu hũ, chan nước đường.'],
    tags: ['#trangmieng', '#tauhu'],
  },
];

recipes.forEach((r, idx) => { if (!dietTagOptions.includes(r.diet as never)) r.diet = dietTagOptions[idx % 4]; });

export const categories = [
  { id: 'c1', name: 'Thuần Chay (Vegan)', desc: '100% nguyên liệu từ thực vật', count: 842, tone: 'leaf' },
  { id: 'c2', name: 'Lacto - Vegetarian', desc: 'Kết hợp cùng sản phẩm từ sữa', count: 356, tone: 'brand' },
  { id: 'c3', name: 'Ovo - Vegetarian', desc: 'Ăn chay kèm trứng gia cầm', count: 214, tone: 'brand' },
  { id: 'c4', name: 'Lacto-Ovo Veg', desc: 'Linh hoạt cả trứng và sữa', count: 398, tone: 'leaf' },
];

// --- Community posts -------------------------------------------------------
export const posts: Post[] = [
  {
    id: 'p1', slug: 'vitamin-b12-cho-nguoi-an-chay',
    title: 'Cách bổ sung đủ Vitamin B12 và Sắt hữu cơ cho người ăn chay dài ngày',
    excerpt: 'Chế độ chay lành mạnh vẫn có thể đủ vi chất nếu bạn hiểu cách kết hợp thực phẩm. Cùng tìm hiểu nguồn B12 và sắt thực vật tối ưu.',
    image: `${A}/95427.png`, author: authors.hoangTam, publishedAt: '15/09/2025', readTime: 6,
    likes: 342, comments: 58, tags: ['Dinh dưỡng', 'Vi chất'],
    body: [
      'Vitamin B12 là vi chất thường thiếu hụt ở người ăn chay thuần vì chủ yếu có trong thực phẩm động vật. Tuy nhiên, bạn hoàn toàn có thể bổ sung qua thực phẩm lên men, nấm men dinh dưỡng và các sản phẩm tăng cường.',
      'Sắt thực vật (non-heme) hấp thu tốt hơn khi kết hợp cùng vitamin C. Hãy ăn rau xanh đậm cùng cam, ổi hoặc ớt chuông trong cùng bữa.',
      'Lời khuyên: theo dõi vi chất định kỳ và tham khảo chuyên gia dinh dưỡng để có kế hoạch phù hợp với thể trạng.',
    ],
  },
  {
    id: 'p2', slug: 'thuc-don-7-ngay-thuan-chay',
    title: 'Top 10 nguồn đạm thực vật dễ tìm giúp thực đơn chay của bạn đủ chất',
    excerpt: 'Không cần thịt cá, bạn vẫn có thể nạp đủ đạm mỗi ngày với 10 nguyên liệu quen thuộc, giá mềm và dễ chế biến.',
    image: `${A}/cddd4.png`, author: authors.tuAn, publishedAt: '12/09/2025', readTime: 5,
    likes: 289, comments: 41, tags: ['Đạm thực vật', 'Thực đơn'],
    body: [
      'Đậu hũ, tempeh, đậu gà, đậu lăng, hạt quinoa... là những nguồn đạm thực vật dồi dào và linh hoạt trong chế biến.',
      'Kết hợp đa dạng các nhóm đậu và ngũ cốc nguyên cám giúp bổ sung đủ các acid amin thiết yếu.',
    ],
  },
  {
    id: 'p3', slug: 'meo-chuyen-doi-sang-an-chay',
    title: 'Lời khuyên từ chuyên gia: chuyển đổi sang ăn chay như thế nào để bền vững?',
    excerpt: 'Ăn chay không phải cuộc đua. Hãy bắt đầu từ những thay đổi nhỏ, lắng nghe cơ thể và duy trì niềm vui trong từng bữa ăn.',
    image: `${A}/40053.png`, author: authors.bepLan, publishedAt: '08/09/2025', readTime: 7,
    likes: 401, comments: 73, tags: ['Lối sống', 'Bí quyết'],
    body: [
      'Bắt đầu bằng việc thay thế dần một vài bữa trong tuần thay vì chuyển đổi hoàn toàn ngay lập tức.',
      'Chuẩn bị sẵn nguyên liệu và lên kế hoạch bữa ăn giúp bạn duy trì thói quen dễ dàng hơn.',
    ],
  },
];

// --- Meal plan (7 days) ----------------------------------------------------
const pick = (i: number) => recipes[i % recipes.length];
export const weekPlan: DayPlan[] = [
  { weekday: 'Thứ Hai', date: '14/10', meals: [{ slot: 'Sáng', recipe: pick(6) }, { slot: 'Trưa', recipe: pick(2) }, { slot: 'Tối', recipe: pick(0) }] },
  { weekday: 'Thứ Ba', date: '15/10', meals: [{ slot: 'Sáng', recipe: pick(6) }, { slot: 'Trưa', recipe: pick(3) }, { slot: 'Tối', recipe: pick(5) }] },
  { weekday: 'Thứ Tư', date: '16/10', meals: [{ slot: 'Sáng', recipe: pick(1) }, { slot: 'Trưa', recipe: pick(2) }, { slot: 'Tối', recipe: pick(4) }] },
  { weekday: 'Thứ Năm', date: '17/10', meals: [{ slot: 'Sáng', recipe: pick(5) }, { slot: 'Trưa', recipe: pick(1) }, { slot: 'Tối', recipe: pick(4) }] },
  { weekday: 'Thứ Sáu', date: '18/10', today: true, meals: [{ slot: 'Sáng', recipe: pick(6) }, { slot: 'Trưa', recipe: pick(3) }] },
  { weekday: 'Thứ Bảy', date: '19/10', meals: [{ slot: 'Sáng', recipe: pick(6) }, { slot: 'Trưa', recipe: pick(4) }, { slot: 'Tối', recipe: pick(3) }] },
  { weekday: 'Chủ Nhật', date: '20/10', meals: [{ slot: 'Sáng', recipe: pick(2) }, { slot: 'Tối', recipe: pick(0) }] },
];

// --- Shopping list ---------------------------------------------------------
export const shoppingList: ShoppingListItem[] = [
  { id: 's1', group: 'Nhóm Rau củ quả tươi', name: 'Cải ngọt xanh Đà Lạt', quantity: '600 g', checked: false, note: 'Dùng cho bữa trưa T2 & canh T5' },
  { id: 's2', group: 'Nhóm Rau củ quả tươi', name: 'Nấm đông cô tươi hữu cơ', quantity: '450 g', checked: false, note: 'Dùng cho món kho chay & canh T3' },
  { id: 's3', group: 'Nhóm Rau củ quả tươi', name: 'Cà rốt Đà Lạt', quantity: '2 củ', checked: false, note: 'Dùng cho món xào chay & nước hầm T4, T6' },
  { id: 's4', group: 'Nhóm Rau củ quả tươi', name: 'Rau mùi thơm & Ngò gai', quantity: 'vừa đủ', checked: true, note: 'Lưu ý PRO: "vừa đủ" giữ nguyên khẩu quy đổi số' },
  { id: 's5', group: 'Nhóm Đậu phụ & Đạm thực vật', name: 'Đậu hũ non Nhật Bản', quantity: '4 hộp', checked: false },
  { id: 's6', group: 'Nhóm Đậu phụ & Đạm thực vật', name: 'Đậu gà hạt khô hữu cơ', quantity: '300 g', checked: false },
  { id: 's7', group: 'Nhóm Đậu phụ & Đạm thực vật', name: 'Tempeh đậu nành', quantity: '200 g', checked: false, note: 'Chế biến ướp chua ngọt chanh dây' },
  { id: 's8', group: 'Nhóm Đậu phụ & Đạm thực vật', name: 'Hạt sen tươi Huế', quantity: '250 g', checked: false },
  { id: 's9', group: 'Nhóm Đậu phụ & Đạm thực vật', name: 'Mì đen ngũ thơm', quantity: '100 g', checked: true },
  { id: 's10', group: 'Nhóm Gia vị & Tinh dầu hạt, Đồ khô', name: 'Nước tương Tamari Lũ Sáu Nấm', quantity: '1 chai (250ml)', checked: false },
  { id: 's11', group: 'Nhóm Gia vị & Tinh dầu hạt, Đồ khô', name: 'Dầu mè nguyên chất ép lạnh', quantity: '1 chai', checked: false },
  { id: 's12', group: 'Nhóm Gia vị & Tinh dầu hạt, Đồ khô', name: 'Tiêu xanh Phú Quốc tươi', quantity: '50 g', checked: false },
  { id: 's13', group: 'Nhóm Gia vị & Tinh dầu hạt, Đồ khô', name: 'Miến dong mộc Điện Biên', quantity: '400 g', checked: true },
  { id: 's14', group: 'Nhóm Gia vị & Tinh dầu hạt, Đồ khô', name: 'Bột nêm củ cải & nấm hữu cơ', quantity: '1 gói', checked: true },
];

// --- Nutrition (daily) -----------------------------------------------------
export const nutritionTargets = [
  { key: 'Năng lượng (Energy)', actual: '1,290 kcal', target: '1,850 kcal', pct: 70, status: 'low' },
  { key: 'Chất đạm (Protein)', actual: '56.4 g', target: '60 g', pct: 94, status: 'ok' },
  { key: 'Carbohydrate phức hợp', actual: '182 g', target: '220 g', pct: 83, status: 'ok' },
  { key: 'Chất béo tốt (Total Fat)', actual: '34 g', target: '45 g', pct: 76, status: 'ok' },
  { key: 'Chất xơ hoà tan (Fiber)', actual: '17.8 g', target: '25.0 g', pct: 71, status: 'low' },
  { key: 'Natri (Sodium)', actual: '1,420 mg', target: '< 2,000 mg', pct: 71, status: 'ok' },
  { key: 'Sắt hữu cơ (Non-heme Iron)', actual: '14.2 mg', target: '18.0 mg', pct: 90, status: 'ok' },
  { key: 'Canxi thực vật (Calcium)', actual: '850 mg', target: '1,000 mg', pct: 85, status: 'ok' },
  { key: 'Vitamin B12 & Vi lượng', actual: '— µg', target: '2.4 mcg', pct: 0, status: 'missing' },
];
