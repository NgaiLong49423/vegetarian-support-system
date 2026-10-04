/* --------------------------------------------------------------------------
 * Shared TypeScript types for the Mâm Xanh frontend demo.
 * ------------------------------------------------------------------------ */

export type UserRole = 'GUEST' | 'CUSTOMER' | 'EXPERT' | 'ADMIN';

export type DietTag = 'Thuần Chay' | 'Lacto' | 'Ovo' | 'Lacto-Ovo';

export type Difficulty = 'Dễ' | 'Trung bình' | 'Khó';

export type ExpertApplicationStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED';

export type MealSlot = 'Sáng' | 'Trưa' | 'Tối';

export interface Author {
  id: string;
  name: string;
  avatar: string;
  verified?: boolean;
  bio?: string;
}

export interface Ingredient {
  id: string;
  name: string;
  quantity: string;
  group: string;
  note?: string;
}

export interface Recipe {
  id: string;
  slug: string;
  name: string;
  image: string;
  diet: DietTag;
  category: string;
  tags: string[];
  prepTime: number;
  cookTime: number;
  servings: number;
  calories: number;
  difficulty: Difficulty;
  description: string;
  ingredients: Ingredient[];
  steps: string[];
  author: Author;
  /** Tổng số lượt xem — theo BR-70 (RECIPE_VIEW) */
  viewCount: number;
  likes: number;
  dislikes: number;
  likePercentage?: number;
  /** Liên kết YouTube tùy chọn (FR-15 / BR-10) */
  youtubeUrl?: string;
  /** Trạng thái bài viết: PUBLISHED (mặc định), HIDDEN, DELETED (BR-05) */
  status?: 'PUBLISHED' | 'HIDDEN' | 'DELETED';
}

export interface Post {
  id: string;
  slug: string;
  title: string;
  excerpt: string;
  body: string[];
  image: string;
  tags: string[];
  author: Author;
  readTime: number;
  likes: number;
  comments: number;
  publishedAt: string;
}

export interface Category {
  id: string;
  name: string;
  desc: string;
  count: number;
  tone: 'brand' | 'leaf';
}

export interface DayPlan {
  date: string;
  weekday: string;
  today?: boolean;
  meals: { slot: MealSlot; recipe: Recipe; servings: number }[];
}

export interface ShoppingListItem {
  id: string;
  group: string;
  name: string;
  quantity: string;
  note?: string;
  checked: boolean;
}

export interface NutrientComparisonItem {
  name: string;
  actual: string;
  target: string;
  percentage: string;
  status: 'good' | 'low' | 'high' | 'missing';
}

export interface ExpertApplication {
  id: string;
  applicantId: string;
  applicantName: string;
  applicantEmail: string;
  experience: string;
  dietaryStyle: DietTag;
  sampleRecipe: string;
  status: ExpertApplicationStatus;
  submittedAt: string;
  reviewedAt?: string;
  rejectionReason?: string;
}