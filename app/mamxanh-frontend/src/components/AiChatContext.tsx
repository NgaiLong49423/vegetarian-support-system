import { createContext, useContext } from 'react';

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
  openChat: (recipe?: RecipeArticleContext) => void;
  closeChat: () => void;
};

export const AiChatContext = createContext<AiChatContextValue | null>(null);

export function useAiChat() {
  const context = useContext(AiChatContext);
  if (!context) throw new Error('useAiChat must be used inside the application layout');
  return context;
}
