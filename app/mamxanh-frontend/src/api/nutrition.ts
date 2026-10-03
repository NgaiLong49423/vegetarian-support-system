import { apiClient, asApiError } from '../lib/apiClient';

export type ActivityLevel = 'SEDENTARY' | 'LIGHTLY_ACTIVE' | 'MODERATELY_ACTIVE' | 'VERY_ACTIVE';
export type BiologicalSex = 'FEMALE' | 'MALE';
export type NutritionGoal = 'MAINTAIN_WEIGHT' | 'IMPROVE_HEALTH' | 'SUPPORT_TRAINING';

export interface NutritionProfileRequest {
  dateOfBirth: string;
  biologicalSex: BiologicalSex;
  heightCm: number;
  weightKg: number;
  activityLevel: ActivityLevel;
  nutritionGoal: NutritionGoal;
  pregnant: boolean;
  breastfeeding: boolean;
  therapeuticDietRequired: boolean;
  consentAccepted: boolean;
}

export interface NutritionTarget {
  key: string;
  label: string;
  minimum: number;
  maximum: number;
  unit: string;
  referenceType: 'AMDR' | 'AI' | 'RDA';
  source: string;
}

export interface NutritionProfileResponse {
  hasProfile: boolean;
  eligible: boolean;
  outOfScopeReasons: string[];
  profile: Omit<NutritionProfileRequest, 'pregnant' | 'breastfeeding' | 'therapeuticDietRequired' | 'consentAccepted'> | null;
  results: {
    bmi: number;
    bmiCategory: string;
    energyKcal: number;
    dailyTargets: NutritionTarget[];
    energySource: string;
    nutrientSource: string;
  } | null;
}

export interface ConfirmNutritionEligibilityRequest {
  pregnant: boolean;
  breastfeeding: boolean;
  therapeuticDietRequired: boolean;
}

async function request<T>(operation: () => Promise<{ data: T }>): Promise<T> {
  try {
    return (await operation()).data;
  } catch (error) {
    throw asApiError(error);
  }
}

export const nutritionApi = {
  getProfile: () => request(() => apiClient.get<NutritionProfileResponse>('/nutrition/profile')),
  saveProfile: (payload: NutritionProfileRequest) => request(
    () => apiClient.put<NutritionProfileResponse>('/nutrition/profile', payload),
  ),
  calculateProfile: (payload: ConfirmNutritionEligibilityRequest) => request(
    () => apiClient.post<NutritionProfileResponse>('/nutrition/profile/calculate', payload),
  ),
};
