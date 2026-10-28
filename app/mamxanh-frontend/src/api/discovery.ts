import { apiClient, asApiError } from '../lib/apiClient';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ExpertDirectoryEntry {
  userId: number;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  followerCount: number;
  following: boolean;
}

export interface IngredientGroup {
  ingredientGroup: string;
  ingredientCount: number;
}

export interface PublicIngredient {
  ingredientId: number;
  name: string;
  ingredientGroup: string;
  imageUrl: string | null;
}

export interface PublicIngredientNutrition {
  energyKcalPer100g: number | null;
  proteinGramsPer100g: number | null;
  carbohydrateGramsPer100g: number | null;
  totalFatGramsPer100g: number | null;
  fiberGramsPer100g: number | null;
  calciumMgPer100g: number | null;
  ironMgPer100g: number | null;
  vitaminB12MicrogramsPer100g: number | null;
  zincMgPer100g: number | null;
}

export interface PublicIngredientDetail extends PublicIngredient {
  nutrition: PublicIngredientNutrition;
}

export interface PublicIngredientUnit {
  id: number;
  code: string;
  name: string;
  dimension: string;
  gramsPerUnit: number;
  approximate: boolean;
}

async function request<T>(operation: () => Promise<{ data: T }>): Promise<T> {
  try {
    return (await operation()).data;
  } catch (error) {
    throw asApiError(error);
  }
}

async function fetchIngredientPage(page: number, size = 100): Promise<PageResponse<PublicIngredient>> {
  return request(() => apiClient.get<PageResponse<PublicIngredient>>('/ingredients', {
    params: { page, size },
  }));
}

async function getAllIngredients(): Promise<PublicIngredient[]> {
  const firstPage = await fetchIngredientPage(0);
  const ingredients = [...firstPage.content];
  for (let page = 1; page < firstPage.totalPages; page += 1) {
    const nextPage = await fetchIngredientPage(page);
    ingredients.push(...nextPage.content);
  }
  return ingredients;
}

export const discoveryApi = {
  searchExperts: (query: string, page: number, size = 12) => request(
    () => apiClient.get<PageResponse<ExpertDirectoryEntry>>('/experts', { params: { query, page, size } }),
  ),
  getIngredientGroups: () => request(
    () => apiClient.get<IngredientGroup[]>('/ingredients/groups'),
  ),
  getAllIngredients,
  searchIngredients: (group: string | undefined, query: string, page: number, size = 20) => request(
    () => apiClient.get<PageResponse<PublicIngredient>>('/ingredients', { params: { group, query, page, size } }),
  ),
  getIngredient: (ingredientId: number) => request(
    () => apiClient.get<PublicIngredientDetail>(`/ingredients/${ingredientId}`),
  ),
  getIngredientUnits: (ingredientId: number) => request(
    () => apiClient.get<PublicIngredientUnit[]>(`/ingredients/${ingredientId}/units`),
  ),
  searchShoppingIngredients: (query: string) => request(
    () => apiClient.get<PageResponse<PublicIngredient>>('/ingredients', { params: { query, page: 0, size: 8 } }),
  ),
};
