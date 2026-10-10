import { apiClient, asApiError } from '../lib/apiClient';

export type Choice = { code: string; label: string };
export type RecipeUnitOption = { unitId: number; code: string; name: string; dimension: string };
export type IngredientOption = { ingredientId: number; name: string };

export interface RecipeFormOptions {
  dishCategories: Choice[];
  vegetarianTypes: Choice[];
  difficulties: Choice[];
  units: RecipeUnitOption[];
}

export interface RecipeIngredientInput {
  ingredientId: number;
  unitId: number;
  quantity: number;
}

export interface RecipeMediaInput {
  blobUrl: string;
  mimeType: string;
  cover: boolean;
}

export interface CreateRecipeRequest {
  title: string;
  description: string;
  instructions: string;
  dishCategory: string;
  vegetarianType: string;
  difficulty: string;
  servings: number;
  prepTimeMinutes: number;
  cookTimeMinutes: number;
  youtubeUrl: string;
  ingredients: RecipeIngredientInput[];
  media: RecipeMediaInput[];
}

export interface CreateRecipeResponse {
  recipeId: number;
  title: string;
  status: 'PUBLISHED';
  publishedAt: string;
}

export interface RecipeDetail {
  recipeId: number;
  title: string;
  description: string | null;
  instructions: string;
  dishCategory: string;
  dishCategoryLabel: string;
  vegetarianType: 'VEGAN' | 'LACTO' | 'OVO' | 'LACTO_OVO';
  vegetarianTypeLabel: string;
  difficulty: string;
  difficultyLabel: string;
  servings: number;
  prepTimeMinutes: number;
  cookTimeMinutes: number;
  youtubeUrl: string | null;
  publishedAt: string;
  nutritionComplete: boolean;
  ingredientsWithoutNutrition: string[];
  ingredients: Array<{
    ingredientId: number;
    name: string;
    quantity: number;
    unitId: number;
    unitCode: string;
    unitName: string;
  }>;
  media: Array<{ blobUrl: string; mimeType: string; displayOrder: number; cover: boolean }>;
}

export interface RecipeIngredient {
  ingredientId: number;
  name: string;
  customName: null;
  unitId: number;
  unitCode: string;
  unitName: string;
  quantity: number;
}

export interface RecipeMedia {
  url: string;
  mimeType: string;
  displayOrder: number;
  cover: boolean;
}

export interface RecipePost {
  id: number;
  authorId: number;
  authorName: string;
  authorAvatarUrl: string | null;
  title: string;
  description: string | null;
  instructions: string;
  dishCategory: string;
  vegetarianType: string;
  difficulty: string;
  servings: number;
  prepTimeMin: number;
  cookTimeMin: number;
  youtubeUrl: string | null;
  status: 'PUBLISHED' | 'HIDDEN' | 'DELETED';
  media: RecipeMedia[];
  ingredients: RecipeIngredient[];
  likes: number;
  dislikes: number;
  likePercentage: number | null;
  viewCount: number;
}

export interface RecipeSearchResult {
  items: RecipePost[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface RecipeSearchParams {
  keyword: string;
  page: number;
  size: number;
  sort: 'NEWEST' | 'MOST_LIKED' | 'MOST_VIEWED' | 'MOST_COMMENTED' | 'MOST_ACTIVE' | 'TRENDING';
  viewPeriod: 'ALL_TIME' | 'LAST_24_HOURS' | 'LAST_7_DAYS' | 'LAST_30_DAYS';
  vegetarianType?: string;
  dishCategory?: string;
  ingredientIds?: number[];
  maxTotalTimeMinutes?: number;
}

export interface MealPlanWeek {
  weekStartDate: string;
  entries: Array<{
    entryId: number;
    mealDate: string;
    mealType: 'BREAKFAST' | 'LUNCH' | 'DINNER';
    plannedServings: number;
    recipeId: number;
    recipeTitle: string | null;
    recipeCoverUrl: string | null;
    dishCategory: string | null;
    totalTimeMinutes: number | null;
    recipeDeleted: boolean;
    unavailableMessage: string | null;
  }>;
}

export interface UpdateRecipePost {
  title: string;
  description: string;
  instructions: string;
  dishCategory: string;
  vegetarianType: string;
  difficulty: string;
  servings: number;
  prepTimeMin: number;
  cookTimeMin: number;
  youtubeUrl: string;
  ingredients: Array<Pick<RecipeIngredient, 'ingredientId' | 'unitId' | 'quantity'>>;
}

export interface RecipeReferenceData {
  ingredients: Array<{ ingredientId: number; name: string; unitId: number; unitCode: string; unitName: string }>;
}

async function request<T>(operation: () => Promise<{ data: T }>): Promise<T> {
  try {
    return (await operation()).data;
  } catch (error) {
    throw asApiError(error);
  }
}

export const recipesApi = {
  getFormOptions: () => request(() => apiClient.get<RecipeFormOptions>('/recipes/form-options')),
  searchIngredients: (query: string) => request(
    () => apiClient.get<IngredientOption[]>('/recipes/ingredient-options', { params: { query } }),
  ),
  publish: (payload: CreateRecipeRequest) => request(
    () => apiClient.post<CreateRecipeResponse>('/recipes', payload),
  ),
  getPublished: (recipeId: number) => request(
    () => apiClient.get<RecipeDetail>(`/recipes/${recipeId}`),
  ),
  searchPublic: (filters: RecipeSearchParams) => {
    const params = new URLSearchParams({
      keyword: filters.keyword,
      page: String(filters.page),
      size: String(filters.size),
      sort: filters.sort,
      viewPeriod: filters.viewPeriod,
    });
    if (filters.vegetarianType) params.set('vegetarianType', filters.vegetarianType);
    if (filters.dishCategory) params.set('dishCategory', filters.dishCategory);
    if (filters.maxTotalTimeMinutes) params.set('maxTotalTimeMinutes', String(filters.maxTotalTimeMinutes));
    filters.ingredientIds?.forEach((ingredientId) => params.append('ingredientIds', String(ingredientId)));
    return request(() => apiClient.get<RecipeSearchResult>('/recipes', { params }));
  },
};

export const recipeApi = {
  listMine: async (page = 0, size = 50) =>
    (await apiClient.get<{ items: RecipePost[]; page: number; size: number; totalElements: number; totalPages: number }>(
      '/recipes/mine', { params: { page, size } },
    )).data,
  getForAuthor: async (recipeId: number) => (await apiClient.get<RecipePost>(`/recipes/${recipeId}/manage`)).data,
  getReferenceData: async () => (await apiClient.get<RecipeReferenceData>('/recipes/reference-data')).data,
  update: async (recipeId: number, payload: UpdateRecipePost) =>
    (await apiClient.put<RecipePost>(`/recipes/${recipeId}`, payload)).data,
  delete: (recipeId: number) => apiClient.delete<void>(`/recipes/${recipeId}`),
};

export const mealPlanApi = {
  getWeek: async (weekStartDate: string) =>
    (await apiClient.get<MealPlanWeek>('/meal-plans', { params: { weekStartDate } })).data,
};
