import { apiRequest } from './client';

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

export const nutritionApi = {
  getProfile: () => apiRequest<NutritionProfileResponse>('/nutrition/profile'),
  saveProfile: (request: NutritionProfileRequest) => apiRequest<NutritionProfileResponse>('/nutrition/profile', {
    method: 'PUT',
    body: JSON.stringify(request),
  }),
  calculateProfile: (request: ConfirmNutritionEligibilityRequest) => apiRequest<NutritionProfileResponse>('/nutrition/profile/calculate', {
    method: 'POST',
    body: JSON.stringify(request),
  }),
};
