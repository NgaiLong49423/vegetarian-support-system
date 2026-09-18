export type DietTag =
  | 'Thuần Chay'
  | 'Lacto'
  | 'Ovo'
  | 'Lacto-Ovo'
  | 'Chay Kỳ';

export type Difficulty = 'Dễ' | 'Trung bình' | 'Khó';

export type MealSlot = 'Sáng' | 'Trưa' | 'Tối';

export interface Author {
  id: string;
  name: string;
  avatar: string;
  bio?: string;
  verified?: boolean;
}

export interface Ingredient {
  id: string;
  name: string;
  quantity: string;
  group: string; // e.g. "Rau củ quả tươi"
  note?: string;
}

export interface Recipe {
  id: string;
  slug: string;
  name: string;
  image: string;
  description: string;
  prepTime: number; // minutes
  cookTime: number; // minutes
  servings: number;
  calories: number;
  difficulty: Difficulty;
  diet: DietTag;
  category: string;
  rating: number;
  reviews: number;
  author: Author;
  ingredients: Ingredient[];
  steps: string[];
  tags: string[];
}

export interface Post {
  id: string;
  slug: string;
  title: string;
  excerpt: string;
  image: string;
  author: Author;
  publishedAt: string;
  readTime: number;
  likes: number;
  comments: number;
  tags: string[];
  body: string[];
}

export interface MealPlanItem {
  slot: MealSlot;
  recipe: Recipe;
}

export interface DayPlan {
  date: string; // "14/10"
  weekday: string; // "Thứ Hai"
  meals: MealPlanItem[];
  today?: boolean;
}

export interface ShoppingListItem {
  id: string;
  name: string;
  quantity: string;
  group: string;
  checked: boolean;
  note?: string;
}
