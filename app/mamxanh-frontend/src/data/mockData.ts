import type {
  Category,
  DayPlan,
  ExpertApplication,
  NutrientComparisonItem,
  Post,
  Recipe,
  ShoppingListItem,
} from '../types';

/* --------------------------------------------------------------------------
 * Demo user + AI plan used across mock screens.
 * ------------------------------------------------------------------------ */

export const currentUser = {
  id: 'u1',
  name: 'Lan Anh',
  avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200',
  bio: 'Yêu ẩm thực thuần chay và lối sống cân bằng vi chất.',
};

export const demoAiPlan = 'FREE';

/* --------------------------------------------------------------------------
 * Recipes — 6 mode sort cần: popular, rating, time, calories, newest, views
 * ------------------------------------------------------------------------ */

const recipeAuthors: Record<string, Recipe['author']> = {
  a1: {
    id: 'a1',
    name: 'Bếp Chay Tâm An',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200',
    verified: true,
    bio: 'Chuyên gia thực dưỡng · 86 công thức chia sẻ',
  },
  a2: {
    id: 'a2',
    name: 'Bếp Chay Lan',
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200',
    verified: true,
    bio: 'Chuyên gia ẩm thực thực vật · 42 công thức',
  },
  a3: {
    id: 'a3',
    name: 'Bếp Chay Lan',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200',
    verified: true,
    bio: 'Chuyên gia ẩm thực chay · 35 công thức',
  },
  a4: {
    id: 'a4',
    name: 'Minh Foodie',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200',
    verified: false,
    bio: 'Food blogger thực vật · 52 công thức',
  },
  a5: {
    id: 'a5',
    name: 'Thảo My',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200',
    verified: true,
    bio: 'Chuyên gia ẩm thực Ấn · 28 công thức',
  },
  a6: {
    id: 'a6',
    name: 'Tú An',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200',
    verified: false,
    bio: 'Đầu bếp chay tại gia · 20 công thức',
  },
};

const recipeEngagement: Record<string, Pick<Recipe, 'viewCount' | 'likes' | 'dislikes' | 'likePercentage'>> = {
  r1: { viewCount: 12480, likes: 248, dislikes: 10, likePercentage: 96 },
  r2: { viewCount: 8560, likes: 178, dislikes: 6, likePercentage: 97 },
  r3: { viewCount: 6420, likes: 165, dislikes: 8, likePercentage: 94 },
  r4: { viewCount: 9480, likes: 220, dislikes: 7, likePercentage: 97 },
  r5: { viewCount: 5280, likes: 132, dislikes: 13, likePercentage: 91 },
  r6: { viewCount: 4180, likes: 96, dislikes: 5, likePercentage: 95 },
};

export const recipes: Recipe[] = [
  {
    id: 'r1',
    slug: 'dau-hu-non-sot-nam-dong-co',
    name: 'Đậu hũ non sốt nấm đông cô tiêu xanh',
    image: 'https://images.unsplash.com/photo-1589442305595-62647c1514f9?w=900',
    diet: 'Thuần Chay',
    category: 'Món mặn',
    tags: ['đạm thực vật', 'món Việt', 'thanh đạm'],
    prepTime: 15,
    cookTime: 20,
    servings: 4,
    calories: 210,
    difficulty: 'Trung bình',
    description:
      'Đậu hũ non mềm mịn hòa quyện cùng sốt nấm đông cô đậm đà, thơm nồng tiêu xanh Phú Quốc. Món ăn cân bằng đạm thực vật và vi chất, phù hợp cho bữa cơm gia đình.',
    ingredients: [
      { id: 'i1', name: 'Đậu hũ non Nhật Bản', quantity: '300 g', group: 'Đạm thực vật' },
      { id: 'i2', name: 'Nấm đông cô tươi', quantity: '150 g', group: 'Rau củ' },
      { id: 'i3', name: 'Nước tương Tamari', quantity: '2 thìa canh', group: 'Gia vị' },
      { id: 'i4', name: 'Tiêu xanh Phú Quốc', quantity: '2 nhánh', group: 'Gia vị' },
      { id: 'i5', name: 'Hành boa-rô', quantity: '30 g', group: 'Rau củ' },
    ],
    steps: [
      'Nhẹ tay lấy đậu hũ non ra khỏi hộp, cắt thành 4 khối vuông đều nhau. Đặt lên đĩa sâu lòng có lót khăn giấy để ráo nước.',
      'Nấm đông cô rửa sạch, cắt bỏ chân, thái lát dày khoảng 3mm.',
      'Hòa nước tương Tamari, dầu hào nấm và bột năng với 100ml nước lọc.',
      'Đun nóng chảo với dầu mè, phi thơm hành boa-rô rồi cho nấm vào xào lửa lớn.',
      'Đổ hỗn hợp sốt vào chảo nấm, hạ lửa vừa, khuấy đến khi sốt sánh lại.',
      'Hấp đậu hũ non trong xửng 5 phút cho nóng đều.',
      'Rưới sốt nấm lên đậu hũ, rắc tiêu xanh đập dập và vài lá ngò.',
    ],
    author: recipeAuthors.a1,
    youtubeUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
    ...recipeEngagement.r1,
  },
  {
    id: 'r2',
    slug: 'salad-quinoa-bo-sap-sot-chanh',
    name: 'Salad Quinoa Bơ Sáp Sốt Chanh',
    image: 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=900',
    diet: 'Thuần Chay',
    category: 'Salad',
    tags: ['salad', 'đạm thực vật', 'eat clean'],
    prepTime: 10,
    cookTime: 5,
    servings: 2,
    calories: 345,
    difficulty: 'Dễ',
    description:
      'Quinoa giàu đạm kết hợp cùng bơ sáp béo ngậy và nước cốt chanh tươi mát, món salad này là lựa chọn hoàn hảo cho bữa trưa nhẹ nhàng.',
    ingredients: [
      { id: 'i6', name: 'Quinoa chín', quantity: '1 cup', group: 'Ngũ cốc' },
      { id: 'i7', name: 'Bơ sáp chín', quantity: '120 g', group: 'Rau củ' },
      { id: 'i8', name: 'Nước cốt chanh', quantity: '2 thìa canh', group: 'Gia vị' },
      { id: 'i9', name: 'Hạt bí rang', quantity: '30 g', group: 'Hạt dinh dưỡng' },
    ],
    steps: [
      'Quinoa nấu chín để nguội.',
      'Bơ sáp cắt hạt lựu vừa ăn.',
      'Trộn đều quinoa, bơ, nước cốt chanh và dầu ô liu.',
      'Rắc hạt bí rang lên trên, nêm muối tiêu vừa ăn.',
    ],
    author: recipeAuthors.a2,
    youtubeUrl: 'https://www.youtube.com/watch?v=7wtfhZwyrcc',
    ...recipeEngagement.r2,
  },
  {
    id: 'r3',
    slug: 'pho-chay-nam-huong-rung',
    name: 'Phở Chay Nấm Hương Rừng',
    image: 'https://images.unsplash.com/photo-1555126634-323283e090fa?w=900',
    diet: 'Thuần Chay',
    category: 'Món nước',
    tags: ['món nước', 'phở chay', 'thanh đạm'],
    prepTime: 20,
    cookTime: 40,
    servings: 3,
    calories: 280,
    difficulty: 'Trung bình',
    description:
      'Nước dùng phở chay ngọt thanh từ rau củ và nấm hương rừng, sợi phở mềm dai, thơm lừng quế hồi.',
    ingredients: [
      { id: 'i10', name: 'Nấm hương rừng', quantity: '80 g', group: 'Rau củ quả tươi' },
      { id: 'i11', name: 'Bánh phở tươi', quantity: '400 g', group: 'Ngũ cốc' },
      { id: 'i12', name: 'Quế, hồi, thảo quả', quantity: '1 gói', group: 'Gia vị & Tinh dầu hạt' },
    ],
    steps: [
      'Nướng thơm gừng, hành và gia vị.',
      'Hầm rau củ 40 phút lấy nước ngọt.',
      'Chần bánh phở, xếp topping, chan nước dùng.',
    ],
    author: recipeAuthors.a3,
    youtubeUrl: 'https://www.youtube.com/watch?v=kXYiU_JCYtU',
    ...recipeEngagement.r3,
  },
  {
    id: 'r4',
    slug: 'goi-cuon-cau-vong-sot-dau',
    name: 'Gỏi Cuốn Cầu Vồng Sốt Đậu',
    image: 'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=900',
    diet: 'Thuần Chay',
    category: 'Khai vị',
    tags: ['gỏi cuốn', 'khai vị', 'ít calo'],
    prepTime: 15,
    cookTime: 0,
    servings: 4,
    calories: 190,
    difficulty: 'Dễ',
    description:
      'Bảy sắc cầu vồng cuộn trong từng chiếc bánh tráng, chấm cùng sốt tương đậu đậm đà. Món khai vị thanh mát, dễ làm.',
    ingredients: [
      { id: 'i14', name: 'Bánh tráng gạo', quantity: '8 cái', group: 'Ngũ cốc' },
      { id: 'i15', name: 'Rau xà lách', quantity: '150 g', group: 'Rau củ' },
      { id: 'i16', name: 'Cà rốt & dưa leo', quantity: '150 g', group: 'Rau củ' },
      { id: 'i17', name: 'Tương đậu', quantity: '3 thìa canh', group: 'Gia vị' },
    ],
    steps: [
      'Rửa sạch và cắt sợi rau củ.',
      'Tráng bánh với nước ấm, xếp nhân theo màu cầu vồng.',
      'Cuộn chặt tay, cắt đôi nếu muốn.',
      'Pha sốt tương đậu với chút đậu phộng rang.',
    ],
    author: recipeAuthors.a4,
    ...recipeEngagement.r4,
  },
  {
    id: 'r5',
    slug: 'ca-ri-hat-dieu-beo-ngay',
    name: 'Cà Ri Hạt Điều Béo Ngậy Đủ Chất',
    image: 'https://images.unsplash.com/photo-1570368336224-455084c1fb31?w=900',
    diet: 'Lacto',
    category: 'Món mặn',
    tags: ['cà ri', 'đạm thực vật', 'Ấn Độ'],
    prepTime: 10,
    cookTime: 20,
    servings: 4,
    calories: 380,
    difficulty: 'Trung bình',
    description:
      'Hạt điều rang béo ngậy kết hợp cùng nước cốt dừa và bột cà ri Ấn Độ tạo nên món cà ri đậm đà, giàu chất béo tốt.',
    ingredients: [
      { id: 'i18', name: 'Hạt điều rang', quantity: '100 g', group: 'Hạt dinh dưỡng' },
      { id: 'i19', name: 'Nước cốt dừa', quantity: '200 ml', group: 'Nguyên liệu khác' },
      { id: 'i20', name: 'Bột cà ri Ấn Độ', quantity: '2 thìa canh', group: 'Gia vị' },
      { id: 'i21', name: 'Khoai tây', quantity: '200 g', group: 'Rau củ' },
    ],
    steps: [
      'Rang hạt điều rồi xay nhuyễn cùng nước cốt dừa.',
      'Rang bột cà ri với dầu dừa cho dậy mùi.',
      'Cho khoai tây, cà rốt vào xào săn.',
      'Đổ hỗn hợp hạt điều vào, nấu lửa nhỏ đến khi sánh.',
    ],
    author: recipeAuthors.a5,
    youtubeUrl: 'https://www.youtube.com/watch?v=fJ9rUzIMcZQ',
    ...recipeEngagement.r5,
  },
  {
    id: 'r6',
    slug: 'chao-rong-bien-hat-sen-duong-sinh',
    name: 'Cháo Rong Biển Hạt Sen Dưỡng Sinh',
    image: 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=900',
    diet: 'Thuần Chay',
    category: 'Món canh',
    tags: ['cháo', 'dưỡng sinh', 'thanh đạm'],
    prepTime: 10,
    cookTime: 30,
    servings: 3,
    calories: 185,
    difficulty: 'Dễ',
    description:
      'Cháo rong biển hạt sen thanh mát, dễ tiêu hóa, phù hợp cho bữa sáng dưỡng sinh hoặc người đang ăn nhẹ.',
    ingredients: [
      { id: 'i22', name: 'Gạo tẻ', quantity: '100 g', group: 'Ngũ cốc' },
      { id: 'i23', name: 'Rong biển khô', quantity: '10 g', group: 'Rau củ' },
      { id: 'i24', name: 'Hạt sen tươi', quantity: '80 g', group: 'Hạt dinh dưỡng' },
    ],
    steps: [
      'Ngâm gạo và hạt sen 30 phút.',
      'Nấu cháo với nước dùng rau củ trong 30 phút.',
      'Thêm rong biển, nêm nếm vừa ăn.',
      'Múc ra tô, rắc tiêu và hành lá.',
    ],
    author: recipeAuthors.a6,
    status: 'PUBLISHED',
    ...recipeEngagement.r6,
  },
  {
    id: 'r7',
    slug: 'canh-chua-chay-nam-dau-bap',
    name: 'Canh chua chay nấm đậu bắp thanh nhiệt',
    image: 'https://images.unsplash.com/photo-1547592180-85f173990554?w=900',
    diet: 'Thuần Chay',
    category: 'Món canh',
    tags: ['canh chua', 'thanh nhiệt', 'món Việt', 'nấm'],
    prepTime: 15,
    cookTime: 15,
    servings: 4,
    calories: 145,
    difficulty: 'Dễ',
    description: 'Canh chua chay nấu cùng nấm rơm, cà chua và đậu bắp thanh nhiệt.',
    ingredients: [
      { id: 'i25', name: 'Nấm rơm tươi', quantity: '150 g', group: 'Rau củ' },
      { id: 'i26', name: 'Đậu bắp non', quantity: '100 g', group: 'Rau củ' },
    ],
    steps: ['Sơ chế nấm và đậu bắp.', 'Nấu nước sôi nêm me.', 'Múc ra tô thưởng thức.'],
    author: recipeAuthors.a2,
    youtubeUrl: 'https://youtu.be/dQw4w9WgXcQ',
    status: 'PUBLISHED',
    viewCount: 7820,
    likes: 195,
    dislikes: 4,
    likePercentage: 98,
  },
  {
    id: 'r-hidden',
    slug: 'mon-chay-vi-pham-tieu-chuan',
    name: 'Công thức vi phạm quy định cộng đồng',
    image: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=900',
    diet: 'Thuần Chay',
    category: 'Món mặn',
    tags: ['ẩn'],
    prepTime: 10,
    cookTime: 10,
    servings: 2,
    calories: 200,
    difficulty: 'Dễ',
    description: 'Nội dung này đã bị quản trị viên ẩn do vi phạm tiêu chuẩn cộng đồng.',
    ingredients: [],
    steps: [],
    author: recipeAuthors.a1,
    status: 'HIDDEN',
    viewCount: 0,
    likes: 0,
    dislikes: 0,
  },
];

/* --------------------------------------------------------------------------
 * Categories
 * ------------------------------------------------------------------------ */

export const categories: Category[] = [
  { id: 'c1', name: 'Thuần Chay (Vegan)', desc: '100% nguyên liệu thực vật, không sản phẩm động vật.', count: 720, tone: 'leaf' },
  { id: 'c2', name: 'Lacto – Vegetarian', desc: 'Kết hợp sữa và chế phẩm từ sữa trong chế biến.', count: 650, tone: 'brand' },
  { id: 'c3', name: 'Ovo – Vegetarian', desc: 'Bổ sung nguồn đạm dồi dào từ trứng.', count: 280, tone: 'leaf' },
  { id: 'c4', name: 'Lacto – Ovo Veg', desc: 'Đầy đủ lựa chọn với trứng, sữa và đạm thực vật.', count: 390, tone: 'brand' },
];

/* --------------------------------------------------------------------------
 * Posts
 * ------------------------------------------------------------------------ */

export const posts: Post[] = [
  {
    id: 'p1',
    slug: 'bo-sung-vitamin-b12-sat-huu-co-cho-nguoi-an-chay',
    title: 'Cách bổ sung đủ Vitamin B12 và Sắt hữu cơ cho người mới ăn thuần chay',
    excerpt: 'Hướng dẫn thực tế để cân bằng vi chất khi bắt đầu hành trình ăn thuần thực vật.',
    body: [
      'Vitamin B12 là vi chất gần như không có trong thực vật chưa tăng cường. Người ăn thuần chay cần chủ động bổ sung qua men dinh dưỡng hoặc thực phẩm tăng cường.',
      'Sắt hữu cơ từ thực vật hấp thu kém hơn sắt heme, nhưng có thể tăng cường bằng cách kết hợp với vitamin C trong cùng bữa ăn.',
      'Nên kiểm tra định kỳ và tham khảo chuyên gia dinh dưỡng để có phác đồ phù hợp.',
    ],
    image: 'https://images.unsplash.com/photo-1758293121435-396ed31ebcf4?w=900',
    tags: ['Dinh dưỡng', 'Vi chất'],
    author: {
      id: 'a7',
      name: 'Dinh Dưỡng Thảo Mộc',
      avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200',
      verified: true,
    },
    readTime: 5,
    likes: 340,
    comments: 24,
    publishedAt: '18/10/2024',
  },
  {
    id: 'p2',
    slug: 'top-10-nguon-dam-thuc-vat-thay-the-thit-ca',
    title: 'Top 10 nguồn đạm thực vật dồi dào thay thế thịt cá cho người tập luyện',
    excerpt: 'Danh sách các loại hạt, đậu và ngũ cốc giàu đạm cho người ăn chay tập gym.',
    body: [
      'Đậu gà, đậu lăng, đậu hũ, tempeh là những nguồn đạm thực vật hoàn chỉnh hoặc gần hoàn chỉnh.',
      'Kết hợp ngũ cốc với họ đậu giúp bổ sung đủ axit amin thiết yếu.',
      'Nên chia đạm đều các bữa để tối ưu tổng hợp protein.',
    ],
    image: 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=900',
    tags: ['Đạm thực vật', 'Luyện tập'],
    author: {
      id: 'a1',
      name: 'Bếp Chay Tâm An',
      avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200',
      verified: true,
    },
    readTime: 5,
    likes: 412,
    comments: 32,
    publishedAt: '17/10/2024',
  },
  {
    id: 'p3',
    slug: 'lo-trinh-21-ngay-chuyen-sang-an-chay',
    title: 'Lộ trình 21 ngày chuyển đổi sang ăn chay nhẹ nhàng không lo thiếu chất',
    excerpt: 'Kế hoạch từng bước để cơ thể thích nghi và bạn duy trì được lối sống thuần chay.',
    body: [
      'Tuần 1: giảm dần thịt đỏ, tăng rau và đậu.',
      'Tuần 2: thay thế hoàn toàn thịt bằng đạm thực vật.',
      'Tuần 3: ổn định thực đơn và bổ sung vi chất cần thiết.',
    ],
    image: 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=900',
    tags: ['Lối sống', 'Thực đơn'],
    author: {
      id: 'a8',
      name: 'Bác sĩ Hoàng Nam',
      avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200',
      verified: true,
    },
    readTime: 7,
    likes: 520,
    comments: 48,
    publishedAt: '16/10/2024',
  },
];

/* --------------------------------------------------------------------------
 * Week plan, shopping list, nutrients, expert applications
 * ------------------------------------------------------------------------ */

export const weekPlan: DayPlan[] = [
  { date: '14/10', weekday: 'Thứ Hai', meals: [{ slot: 'Sáng', recipe: recipes[5], servings: 1 }, { slot: 'Trưa', recipe: recipes[1], servings: 1 }, { slot: 'Tối', recipe: recipes[0], servings: 1 }] },
  { date: '15/10', weekday: 'Thứ Ba', meals: [{ slot: 'Sáng', recipe: recipes[2], servings: 1 }, { slot: 'Trưa', recipe: recipes[4], servings: 1 }, { slot: 'Tối', recipe: recipes[3], servings: 1 }] },
  { date: '16/10', weekday: 'Thứ Tư', meals: [{ slot: 'Sáng', recipe: recipes[1], servings: 1 }, { slot: 'Trưa', recipe: recipes[0], servings: 1 }] },
  { date: '17/10', weekday: 'Thứ Năm', meals: [{ slot: 'Sáng', recipe: recipes[5], servings: 1 }, { slot: 'Trưa', recipe: recipes[2], servings: 1 }, { slot: 'Tối', recipe: recipes[4], servings: 1 }] },
  { date: '18/10', weekday: 'Thứ Sáu', today: true, meals: [{ slot: 'Sáng', recipe: recipes[1], servings: 1 }, { slot: 'Trưa', recipe: recipes[3], servings: 1 }, { slot: 'Tối', recipe: recipes[0], servings: 1 }] },
  { date: '19/10', weekday: 'Thứ Bảy', meals: [{ slot: 'Sáng', recipe: recipes[2], servings: 1 }, { slot: 'Trưa', recipe: recipes[4], servings: 1 }, { slot: 'Tối', recipe: recipes[3], servings: 1 }] },
  { date: '20/10', weekday: 'Chủ Nhật', meals: [{ slot: 'Sáng', recipe: recipes[0], servings: 1 }, { slot: 'Tối', recipe: recipes[5], servings: 1 }] },
];

export const shoppingList: ShoppingListItem[] = [
  { id: 's1', group: '1. Rau củ quả tươi', name: 'Cải ngọt xanh Đà Lạt', quantity: '600 g', checked: false, note: 'Dùng cho bữa trưa T2 & canh T5' },
  { id: 's2', group: '1. Rau củ quả tươi', name: 'Nấm đùi gà tươi hữu cơ', quantity: '450 g', checked: false },
  { id: 's3', group: '1. Rau củ quả tươi', name: 'Cà rốt Đà Lạt', quantity: '2 củ', checked: false },
  { id: 's4', group: '1. Rau củ quả tươi', name: 'Rau mùi thơm & Ngò gai', quantity: 'vừa đủ', checked: true },
  { id: 's5', group: '2. Đậu phụ & Đạm thực vật', name: 'Đậu hũ non Nhật Bản', quantity: '4 hộp', checked: false },
  { id: 's6', group: '2. Đậu phụ & Đạm thực vật', name: 'Đậu gà hạt khô hữu cơ', quantity: '300 g', checked: false },
  { id: 's7', group: '2. Đậu phụ & Đạm thực vật', name: 'Tempeh đậu nành', quantity: '200 g', checked: false },
  { id: 's8', group: '3. Gia vị & Đồ khô', name: 'Nước tương Tamari ủ lâu năm', quantity: '1 chai (250ml)', checked: false },
  { id: 's9', group: '3. Gia vị & Đồ khô', name: 'Dầu mè nguyên chất ép lạnh', quantity: '1 chai', checked: false },
  { id: 's10', group: '3. Gia vị & Đồ khô', name: 'Bột nêm củ cải & nấm hữu cơ', quantity: '1 gói', checked: true },
];

export const nutritionTargets = [
  { key: 'Năng lượng', actual: '1,850 kcal', target: '2,000 kcal', pct: 92, status: 'good' as const },
  { key: 'Đạm thực vật', actual: '68 g / 65 g', target: '65 g', pct: 100, status: 'good' as const },
  { key: 'Carbohydrate', actual: '240 g', target: '270 g', pct: 89, status: 'good' as const },
  { key: 'Chất béo tốt', actual: '48 g', target: '55 g', pct: 87, status: 'good' as const },
  { key: 'Chất xơ', actual: '34 g', target: '25-30 g', pct: 100, status: 'good' as const },
  { key: 'Natri', actual: '1,800 mg', target: '<2,000 mg', pct: 90, status: 'good' as const },
  { key: 'Sắt hữu cơ', actual: '14 mg', target: '18 mg', pct: 78, status: 'low' as const },
  { key: 'Canxi thực vật', actual: '850 mg', target: '1,000 mg', pct: 85, status: 'good' as const },
  { key: 'Vitamin B12', actual: '—', target: '2.4 mcg', pct: 0, status: 'missing' as const },
];

export const nutrientsDay: NutrientComparisonItem[] = [
  { name: 'Tổng năng lượng', actual: '1,290 kcal', target: '1,850 kcal', percentage: '70%', status: 'low' },
  { name: 'Đạm thực vật', actual: '56.4 g', target: '60.0 g', percentage: '94%', status: 'good' },
  { name: 'Carbohydrate phức hợp', actual: '182 g', target: '220 g', percentage: '83%', status: 'good' },
  { name: 'Chất béo tốt', actual: '34 g', target: '45 g', percentage: '76%', status: 'good' },
  { name: 'Chất xơ hòa tan & thô', actual: '17.8 g', target: '25.0 g', percentage: '71%', status: 'low' },
  { name: 'Natri', actual: '1,420 mg', target: '< 2,000 mg', percentage: '71%', status: 'good' },
  { name: 'Sắt hữu cơ', actual: '16.2 mg', target: '18.0 mg', percentage: '90%', status: 'good' },
  { name: 'Canxi thực vật', actual: '850 mg', target: '1,000 mg', percentage: '85%', status: 'good' },
  { name: 'Vitamin B12 & vi lượng', actual: '—', target: '2.4 mcg', percentage: '—', status: 'missing' },
];

export const nutrientsWeek: NutrientComparisonItem[] = [
  { name: 'Tổng năng lượng', actual: '12,194 kcal', target: '12,950 kcal', percentage: '94%', status: 'good' },
  { name: 'Đạm thực vật', actual: '407.4 g', target: '420.0 g', percentage: '97%', status: 'good' },
  { name: 'Carbohydrate phức hợp', actual: '1,435 g', target: '1,540 g', percentage: '93%', status: 'good' },
  { name: 'Chất béo tốt', actual: '287 g', target: '315 g', percentage: '91%', status: 'good' },
  { name: 'Chất xơ hòa tan & thô', actual: '151 g', target: '175 g', percentage: '86%', status: 'good' },
  { name: 'Natri', actual: '14,560 mg', target: '14,000 mg', percentage: '104%', status: 'high' },
  { name: 'Sắt hữu cơ', actual: '119 mg', target: '126 mg', percentage: '94%', status: 'good' },
  { name: 'Canxi thực vật', actual: '6,160 mg', target: '7,000 mg', percentage: '88%', status: 'good' },
  { name: 'Vitamin B12 & vi lượng', actual: '—', target: '16.8 mcg', percentage: '—', status: 'missing' },
];

export const initialExpertApplications: ExpertApplication[] = [
  {
    id: 'EX-24018',
    applicantId: 'u2',
    applicantName: 'Nguyễn Minh Anh',
    applicantEmail: 'minhanh@example.com',
    experience: 'Tôi có hơn 4 năm xây dựng thực đơn chay gia đình, tập trung vào món Việt cân bằng đạm thực vật và nguyên liệu theo mùa.',
    dietaryStyle: 'Thuần Chay',
    sampleRecipe: 'Đậu hũ non sốt nấm đông cô: áp chảo nấm cùng gừng, nước tương tamari rồi phủ lên đậu hấp, hoàn thiện bằng tiêu xanh.',
    status: 'PENDING',
    submittedAt: '18/10/2024 09:15',
  },
  {
    id: 'EX-24017',
    applicantId: 'u3',
    applicantName: 'Trần Hoàng Nam',
    applicantEmail: 'hoangnam@example.com',
    experience: 'Đầu bếp chay 6 năm kinh nghiệm, từng làm việc tại các nhà hàng chay lớn tại Hà Nội và Đà Nẵng.',
    dietaryStyle: 'Lacto',
    sampleRecipe: 'Cà ri hạt điều béo ngậy: rang hạt điều, xay nhuyễn với nước cốt dừa, nấu cùng khoai tây và cà rốt.',
    status: 'PENDING',
    submittedAt: '17/10/2024 14:30',
  },
  {
    id: 'EX-24016',
    applicantId: 'u4',
    applicantName: 'Lê Thảo Vy',
    applicantEmail: 'thaovy@example.com',
    experience: 'Yêu thích nấu chay tại gia, thường xuyên chia sẻ công thức trên các hội nhóm ăn chay.',
    dietaryStyle: 'Lacto-Ovo',
    sampleRecipe: 'Bánh mì sourdough trứng mây: bánh mì nướng giòn, trứng đánh mềm nấu lửa nhỏ, thêm bơ sáp và hành lá.',
    status: 'APPROVED',
    submittedAt: '16/10/2024 10:00',
    reviewedAt: '17/10/2024 08:20',
  },
];