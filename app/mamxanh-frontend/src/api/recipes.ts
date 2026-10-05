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
