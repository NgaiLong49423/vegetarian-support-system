import { createContext, useContext } from 'react';
import type { AiRecipeFormContext, AiRecipeFormPatch, AiRecipeFormPatchAck } from '../api/recipes';

export type RecipeArticleContext = {
  recipeId: number;
  title: string;
  description: string | null;
  coverUrl: string | null;
  authorName: string;
  authorAvatarUrl: string | null;
  dishCategoryLabel: string;
  vegetarianTypeLabel: string;
  difficultyLabel: string;
  servings: number;
  prepTimeMinutes: number;
  cookTimeMinutes: number;
  ingredients: Array<{ name: string; quantity: number; unitName: string }>;
  instructions: string;
};

export type AiChatContextValue = {
  isOpen: boolean;
  recipeId: number | null;
  recipeTitle: string | null;
  recipeArticle: RecipeArticleContext | null;
  setPageRecipeArticle: (recipe: RecipeArticleContext | null) => void;
  openChat: (recipe?: RecipeArticleContext) => void;
  closeChat: () => void;
  startNewChat: () => void;
  registerRecipeForm: (snapshot: () => AiRecipeFormContext | null, apply: (patch: AiRecipeFormPatch) => Promise<AiRecipeFormPatchAck>) => () => void;
  getRecipeFormContext: () => AiRecipeFormContext | null;
  applyRecipeFormPatch: (patch: AiRecipeFormPatch) => Promise<AiRecipeFormPatchAck>;
};

export const AiChatContext = createContext<AiChatContextValue | null>(null);

export function useAiChat() {
  const context = useContext(AiChatContext);
  if (!context) throw new Error('useAiChat must be used inside the AI chat host');
  return context;
}
