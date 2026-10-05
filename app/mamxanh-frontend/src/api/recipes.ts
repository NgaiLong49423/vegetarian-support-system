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
};

export const recipeApi = {
  getForAuthor: async (recipeId: number) => (await apiClient.get<RecipePost>(`/recipes/${recipeId}/manage`)).data,
  getReferenceData: async () => (await apiClient.get<RecipeReferenceData>('/recipes/reference-data')).data,
  update: async (recipeId: number, payload: UpdateRecipePost) =>
    (await apiClient.put<RecipePost>(`/recipes/${recipeId}`, payload)).data,
  delete: (recipeId: number) => apiClient.delete<void>(`/recipes/${recipeId}`),
};
