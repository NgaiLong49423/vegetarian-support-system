import axios from 'axios';
import { apiClient } from '../lib/apiClient';

export type CatalogIngredient = {
  id: number;
  name: string;
  ingredientGroup: string;
  sourceName: string;
  imageUrl: string | null;
  sourceUrl: string | null;
  referenceDate: string;
  nutritionSupported: boolean;
  active: boolean;
  nutrition: IngredientNutrition;
  recipeUsageCount?: number;
};

export type IngredientNutrition = {
  energyKcalPer100g: number | null;
  proteinGramsPer100g: number | null;
  carbohydrateGramsPer100g: number | null;
  totalFatGramsPer100g: number | null;
  fiberGramsPer100g: number | null;
  calciumMgPer100g: number | null;
  ironMgPer100g: number | null;
  vitaminB12MicrogramsPer100g: number | null;
  zincMgPer100g: number | null;
};

export type IngredientSearch = {
  query: string;
  ingredientGroup: string;
  active: boolean | null;
  nutritionSupported: boolean | null;
  page: number;
  size: number;
};

export type CatalogIngredientPage = {
  content: CatalogIngredient[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type IngredientCatalogOptions = { groups: string[]; sources: string[] };
export type IngredientRecipePage = {
  content: IngredientRecipeReference[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type IngredientRecipeReference = {
  recipeId: number;
  title: string;
  status: string;
  publishedAt: string | null;
};

type ApiResponse<T> = { success: boolean; data: T; message?: string };
type ApiProblem = { detail?: string; errors?: Array<{ field: string; message: string }> };

export class AdminCatalogApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
    this.name = 'AdminCatalogApiError';
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  try {
    const response = await apiClient.request<ApiResponse<T>>({
      url: path,
      method: init?.method ?? 'GET',
      ...(init?.body === undefined ? {} : { data: init.body }),
      ...(init?.body instanceof FormData ? { headers: { 'Content-Type': 'multipart/form-data' } } : {}),
    });
    if (response.status === 204) return undefined as T;
    return response.data.data;
  } catch (cause) {
    if (!axios.isAxiosError<ApiProblem>(cause)) {
      throw cause instanceof Error ? cause : new Error('Không lưu được thay đổi.');
    }

    const status = cause.response?.status ?? 0;
    if (status === 401) {
      throw new AdminCatalogApiError('Bạn cần đăng nhập bằng tài khoản Administrator để dùng danh mục quản trị.', status);
    }
    if (status === 403) {
      throw new AdminCatalogApiError('Tài khoản hiện tại không có quyền quản lý danh mục (403 Forbidden).', status);
    }

    const problem = cause.response?.data;
    const fieldErrors = problem?.errors?.map(({ field, message }) => field + ': ' + message).join(' ');
    throw new Error(fieldErrors || problem?.detail || cause.message || 'Không lưu được thay đổi.');
  }
}

const json = (method: string, body?: unknown): RequestInit => ({
  method,
  ...(body === undefined ? {} : { body: JSON.stringify(body) }),
});

export const adminCatalogApi = {
  ingredients: (query = '') => request<CatalogIngredient[]>('/admin/ingredients' + (query ? '?query=' + encodeURIComponent(query) : '')),
  searchIngredients: (filters: IngredientSearch) => {
    const query = new URLSearchParams({ page: String(filters.page), size: String(filters.size) });
    if (filters.query) query.set('query', filters.query);
    if (filters.ingredientGroup) query.set('ingredientGroup', filters.ingredientGroup);
    if (filters.active !== null) query.set('active', String(filters.active));
    if (filters.nutritionSupported !== null) query.set('nutritionSupported', String(filters.nutritionSupported));
    return request<CatalogIngredientPage>('/admin/ingredients/search?' + query.toString());
  },
  ingredientOptions: () => request<IngredientCatalogOptions>('/admin/ingredients/options'),
  ingredient: (id: number) => request<CatalogIngredient>('/admin/ingredients/' + id),
  ingredientRecipes: (id: number) => request<IngredientRecipeReference[]>('/admin/ingredients/' + id + '/recipes'),
  searchIngredientRecipes: (id: number, page: number, size: number) =>
    request<IngredientRecipePage>('/admin/ingredients/' + id + '/recipes/search?' + new URLSearchParams({ page: String(page), size: String(size) })),
  createIngredient: (body: Omit<CatalogIngredient, 'id' | 'imageUrl' | 'nutritionSupported' | 'active' | 'nutrition' | 'recipeUsageCount'>) =>
    request<CatalogIngredient>('/admin/ingredients', json('POST', body)),
  updateIngredient: (id: number, body: Omit<CatalogIngredient, 'id' | 'imageUrl' | 'nutritionSupported' | 'active' | 'nutrition' | 'recipeUsageCount'>) =>
    request<CatalogIngredient>('/admin/ingredients/' + id, json('PUT', body)),
  uploadIngredientImage: (id: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<{ ingredientId: number; imageUrl: string }>('/admin/ingredients/' + id + '/image', {
      method: 'POST',
      body: form,
    });
  },
  updateNutrition: (id: number, body: IngredientNutrition) =>
    request<CatalogIngredient>('/admin/ingredients/' + id + '/nutrition', json('PUT', body)),
  setNutritionSupported: (id: number, enabled: boolean) =>
    request<CatalogIngredient>('/admin/ingredients/' + id + '/nutrition-support', json('PATCH', { enabled })),
  setIngredientActive: (id: number, active: boolean) =>
    request<CatalogIngredient>('/admin/ingredients/' + id + '/status', json('PATCH', { active })),
};
