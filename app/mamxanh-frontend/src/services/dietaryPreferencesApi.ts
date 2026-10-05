import { apiClient } from '../lib/apiClient';

// FR-31 dietary preferences of the signed-in Member. Runtime contract: generated OpenAPI (/v3/api-docs).

export type VegetarianType = 'VEGAN' | 'LACTO' | 'OVO' | 'LACTO_OVO';
export type CookingDifficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type OnboardingStatus = 'NOT_STARTED' | 'SKIPPED' | 'COMPLETED';
export type DietaryRequirement = 'VEGETARIAN_TYPE' | 'AVOID_INGREDIENTS' | 'DISLIKED_INGREDIENTS';

export type PreferenceItem = { ingredientId: number | null; name: string };

export type DietaryPreferences = {
  vegetarianType: VegetarianType | null;
  avoid: { noneConfirmed: boolean; items: PreferenceItem[] };
  dislike: { noneConfirmed: boolean; items: PreferenceItem[] };
  cuisinePreference: string | null;
  maxCookingTimeMinutes: number | null;
  preferredDifficulty: CookingDifficulty | null;
  onboardingStatus: OnboardingStatus;
  aiPersonalization: { eligible: boolean; missing: DietaryRequirement[] };
};

export type SaveDietaryPreferencesPayload = {
  vegetarianType: VegetarianType;
  avoid: { noneConfirmed: boolean; items: string[] };
  dislike: { noneConfirmed: boolean; items: string[] };
  cuisinePreference: string | null;
  maxCookingTimeMinutes: number | null;
  preferredDifficulty: CookingDifficulty | null;
};

export type IngredientSuggestion = { id: number; name: string; ingredientGroup: string };

const BASE = '/nutrition/dietary-preferences';

export async function getDietaryPreferences(): Promise<DietaryPreferences> {
  const { data } = await apiClient.get<DietaryPreferences>(BASE);
  return data;
}

export async function saveDietaryPreferences(payload: SaveDietaryPreferencesPayload): Promise<DietaryPreferences> {
  const { data } = await apiClient.put<DietaryPreferences>(BASE, payload);
  return data;
}

export async function skipOnboarding(): Promise<void> {
  await apiClient.post(`${BASE}/onboarding/skip`);
}

/** AC-31.10: true only the first time an unanswered invitation is claimed; the Backend records it. */
export async function claimOnboardingInvitation(): Promise<boolean> {
  const { data } = await apiClient.post<{ show: boolean }>(`${BASE}/onboarding/invitation`);
  return data.show;
}

export async function suggestIngredients(query: string): Promise<IngredientSuggestion[]> {
  const { data } = await apiClient.get<IngredientSuggestion[]>(`${BASE}/ingredient-suggestions`, { params: { query } });
  return data;
}
