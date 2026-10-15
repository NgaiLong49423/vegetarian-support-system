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
  /** FR-23 (AC-23.1): current public data of the author's account. */
  author: { userId: number; displayName: string; avatarUrl: string | null };
  nutritionComplete: boolean;
  ingredientsWithoutNutrition: string[];
  nutrition: {
    complete: boolean;
    ingredientsMissingData: string[];
    total: Record<string, number | null>;
    perServing: Record<string, number | null>;
  };
  statistics: {
    likes: number;
    dislikes: number;
    reactionCount: number;
    likePercentage: number | null;
    viewCount: number;
  };
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

export interface MealPlanExport {
  weekStartDate: string;
  entries: Array<{
    mealDate: string;
    mealType: MealPlanWeek['entries'][number]['mealType'];
    title: string;
    plannedServings: number | null;
    energyKcal: number | null;
    available: boolean;
  }>;
}

export interface AiRecipeSuggestion {
  items: Array<{ recipe: RecipePost; reason: string }>;
}

export interface AiWeeklyMenuPreview {
  slots: Array<{
    dayOffset: number;
    mealType: 'BREAKFAST' | 'LUNCH' | 'DINNER';
    recipeId: number;
    recipeTitle: string;
    coverUrl: string | null;
    dishCategory: string;
    vegetarianType: string;
    reason: string;
  }>;
}

export type AiMenuMode = 'PROFILE' | 'THIS_WEEK';
export type AiMenuGoal = 'MAINTAIN_WEIGHT' | 'LOSE_WEIGHT' | 'GAIN_WEIGHT' | 'BUILD_MUSCLE' | 'IMPROVE_HEALTH' | 'SUPPORT_TRAINING';
export interface AiMenuMeal {
  mealDate: string;
  mealType: MealPlanWeek['entries'][number]['mealType'];
  recipeId: number;
  recipeTitle: string;
  coverUrl: string | null;
  nutritionPerServing: Record<string, number>;
  reason: string;
}
export interface AiMenuPreview {
  weekStartDate: string;
  mode: AiMenuMode;
  goal: AiMenuGoal | null;
  meals: AiMenuMeal[];
  nutritionDisclaimer: string;
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

export type RecipeAiContentType = 'DESCRIPTION' | 'INSTRUCTIONS';
export interface RecipeAiAssistanceRequest {
  contentType: RecipeAiContentType;
  title: string;
  dishCategory: string;
  vegetarianType: string;
  servings: number;
  ingredients: RecipeIngredientInput[];
}
export interface RecipeAiAssistanceResponse {
  contentType: RecipeAiContentType;
  content: string;
}

export interface RecipeReferenceData {
  ingredients: Array<{ ingredientId: number; name: string; unitId: number; unitCode: string; unitName: string }>;
}

export type AiChatContext = 'GENERAL' | 'RECIPE';
export interface AiChatTurn { speaker: 'USER' | 'ASSISTANT'; content: string }
export interface AiChatRequest {
  contextType: AiChatContext;
  recipeId?: number;
  question: string;
  history: AiChatTurn[];
}
export interface AiChatResponse {
  contextType: AiChatContext;
  recipeId: number | null;
  answer: string;
  recommendations: Array<{
    id: number;
    title: string;
    description: string | null;
    coverUrl: string | null;
    totalTimeMinutes: number;
    difficulty: string;
    reason: string;
  }>;
}

async function request<T>(operation: () => Promise<{ data: T }>): Promise<T> {
  try {
    return (await operation()).data;
  } catch (error) {
    throw asApiError(error);
  }
}

export const recipesApi = {
  chat: (payload: AiChatRequest) => request(
    () => apiClient.post<AiChatResponse>('/recipes/ai/chat', payload, { timeout: 12_000 }),
  ),
  recommendByIngredients: (ingredientIds: number[]) => request(
    () => apiClient.post<AiRecipeSuggestion>('/recipes/ai/recommendations', { ingredientIds }, { timeout: 60_000 }),
  ),
  createWeeklyMenuPreview: (ingredientIds: number[]) => request(
    () => apiClient.post<AiWeeklyMenuPreview>('/recipes/ai/weekly-menu', { ingredientIds }, { timeout: 60_000 }),
  ),
  related: (recipeId: number) => request(
    () => apiClient.get<{ items: RecipePost[] }>(`/recipes/${recipeId}/related`),
  ),
  recommendRelated: (recipeId: number) => request(
    () => apiClient.post<{ items: RecipePost[] }>(`/recipes/${recipeId}/related/ai`, undefined, { timeout: 60_000 }),
  ),
  getFormOptions: () => request(() => apiClient.get<RecipeFormOptions>('/recipes/form-options')),
  searchIngredients: (query: string) => request(
    () => apiClient.get<IngredientOption[]>('/recipes/ingredient-options', { params: { query } }),
  ),
  publish: (payload: CreateRecipeRequest) => request(
    () => apiClient.post<CreateRecipeResponse>('/recipes', payload),
  ),
  suggestText: (payload: RecipeAiAssistanceRequest) => request(
    // Gemini may take up to 45 seconds; allow time for the Backend request and response too.
    () => apiClient.post<RecipeAiAssistanceResponse>('/recipes/ai-assistance', payload, { timeout: 60_000 }),
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
  getExport: async (weekStartDate: string) =>
    (await apiClient.get<MealPlanExport>(`/meal-plans/${weekStartDate}/export`)).data,
  moveEntry: async (weekStartDate: string, entryId: number, payload: {
    mealDate: string; mealType: MealPlanWeek['entries'][number]['mealType'];
  }) => (await apiClient.patch<MealPlanWeek['entries'][number]>(
    `/meal-plans/${weekStartDate}/entries/${entryId}/position`, payload,
  )).data,
  getWeek: async (weekStartDate: string) =>
    (await apiClient.get<MealPlanWeek>('/meal-plans', { params: { weekStartDate } })).data,
  openWeek: async (weekStartDate: string) =>
    (await apiClient.put<MealPlanWeek>('/meal-plans', undefined, { params: { weekStartDate } })).data,
  applyAiWeeklyMenu: async (weekStartDate: string, slots: Array<{
    dayOffset: number; mealType: 'BREAKFAST' | 'LUNCH' | 'DINNER'; recipeId: number;
  }>) => (await apiClient.post<MealPlanWeek>(`/meal-plans/${weekStartDate}/ai-menu`, { slots })).data,
  addEntry: async (weekStartDate: string, payload: {
    mealDate: string; mealType: MealPlanWeek['entries'][number]['mealType']; recipeId: number;
  }) => (await apiClient.post<MealPlanWeek['entries'][number]>(`/meal-plans/${weekStartDate}/entries`, payload)).data,
  replaceEntry: async (weekStartDate: string, entryId: number, recipeId: number) =>
    (await apiClient.put<MealPlanWeek['entries'][number]>(`/meal-plans/${weekStartDate}/entries/${entryId}`, { recipeId })).data,
  removeEntry: async (weekStartDate: string, entryId: number) => {
    await apiClient.delete<void>(`/meal-plans/${weekStartDate}/entries/${entryId}`);
  },
  updateServings: async (weekStartDate: string, entryId: number, plannedServings: number) =>
    (await apiClient.patch<MealPlanWeek['entries'][number]>(
      `/meal-plans/${weekStartDate}/entries/${entryId}/servings`, { plannedServings },
    )).data,
  nutrition: async (weekStartDate: string, date?: string) =>
    (await apiClient.get<MealPlanNutrition>(`/meal-plans/${weekStartDate}/nutrition`, { params: date ? { date } : {} })).data,
  nutritionPdf: async (weekStartDate: string, date?: string) =>
    (await apiClient.get<Blob>(`/meal-plans/${weekStartDate}/nutrition/report`, {
      params: date ? { date } : {},
      responseType: 'blob',
      headers: { Accept: 'application/pdf, application/problem+json' },
    })).data,
  nutritionRecommendations: async (weekStartDate: string, entryId: number) =>
    (await apiClient.post<MealPlanNutritionRecommendations>(`/meal-plans/${weekStartDate}/nutrition/recommendations/${entryId}`)).data,
  createAiMenuPreview: async (payload: {
    weekStartDate: string; mode: AiMenuMode; goal?: AiMenuGoal | null;
    preferredIngredientIds?: number[]; avoidedIngredientIds?: number[];
  }) => (await apiClient.post<AiMenuPreview>('/meal-plans/ai-menu/previews', payload, { timeout: 60_000 })).data,
  applyAiMenu: async (payload: {
    weekStartDate: string; mode: AiMenuMode; goal?: AiMenuGoal | null;
    avoidedIngredientIds?: number[]; meals: Array<Pick<AiMenuMeal, 'mealDate' | 'mealType' | 'recipeId'>>;
  }) => (await apiClient.put<MealPlanWeek>('/meal-plans/ai-menu', payload)).data,
};

export interface MealPlanNutrition {
  weekStartDate: string;
  scope: 'DAY' | 'WEEK';
  selectedDate: string | null;
  referenceBasis: string;
  source: string;
  sourceUrl: string;
  disclaimer: string;
  targets: Array<{key: string; label: string; minimum: number | null; maximum: number | null; unit: string; referenceType: string; source: string; sourceUrl: string; version: string}>;
  days: Array<{date: string; nutrients: Record<string, number | null>; entries: Array<{entryId: number; date: string; mealType: 'BREAKFAST' | 'LUNCH' | 'DINNER'; recipeId: number; recipeTitle: string; plannedServings: number; complete: boolean; ingredientsMissingData: string[]; nutrients: Record<string, number | null>}>}>;
  weeklyAverage: Record<string, number | null>;
  attainmentDays: Record<string, number>;
  incompleteEntries: Array<{entryId: number; date: string; mealType: string; recipeTitle: string; ingredientsMissingData: string[]}>;
}

export interface MealPlanNutritionRecommendations {
  entryId: number;
  analysis: string;
  recommendations: Array<{recipeId: number; title: string; complete: boolean; ingredientsMissingData: string[]}>;
}
