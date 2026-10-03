import { apiClient } from '../lib/apiClient';

export interface RecipeIngredient {
  ingredientId: number | null;
  name: string | null;
  customName: string | null;
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
  ingredients: Array<Pick<RecipeIngredient, 'ingredientId' | 'customName' | 'unitId' | 'quantity'>>;
  media: RecipeMedia[];
}

export interface RecipeReferenceData {
  ingredients: Array<{ ingredientId: number; name: string; unitId: number; unitCode: string; unitName: string }>;
  customIngredientUnits: Array<{ unitId: number; code: string; name: string }>;
}

export const recipeApi = {
  getForAuthor: async (recipeId: number) => (await apiClient.get<RecipePost>(`/recipes/${recipeId}/manage`)).data,
  getReferenceData: async () => (await apiClient.get<RecipeReferenceData>('/recipes/reference-data')).data,
  update: async (recipeId: number, request: UpdateRecipePost) =>
    (await apiClient.put<RecipePost>(`/recipes/${recipeId}`, request)).data,
  delete: (recipeId: number) => apiClient.delete<void>(`/recipes/${recipeId}`),
};
