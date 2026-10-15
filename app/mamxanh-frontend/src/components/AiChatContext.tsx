import { createContext, useContext } from 'react';

export type AiChatContextValue = {
  isOpen: boolean;
  recipeId: number | null;
  openChat: (recipeId?: number) => void;
  closeChat: () => void;
};

export const AiChatContext = createContext<AiChatContextValue | null>(null);

export function useAiChat() {
  const context = useContext(AiChatContext);
  if (!context) throw new Error('useAiChat must be used inside the application layout');
  return context;
}
