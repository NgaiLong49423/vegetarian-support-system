import {
  lazy,
  Suspense,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react"
import { MessageCircle } from "lucide-react"
import { useLocation } from "react-router-dom"
import { useAuth } from "./AuthContext"
import {
  AiChatContext,
  type AiChatContextValue,
  type RecipeArticleContext,
} from "./AiChatContext"

const AiChatPage = lazy(() =>
  import("../pages/AiChatPage").then((module) => ({ default: module.AiChatPage })),
)

export function AiChatHost({ children }: { children: ReactNode }) {
  const { account } = useAuth()
  const { pathname } = useLocation()
  const [chatOpen, setChatOpen] = useState(false)
  const [chatRecipeId, setChatRecipeId] = useState<number | null>(null)
  const [chatRecipeTitle, setChatRecipeTitle] = useState<string | null>(null)
  const [chatRecipeArticle, setChatRecipeArticle] =
    useState<RecipeArticleContext | null>(null)

  const openChat = useCallback((recipe?: RecipeArticleContext) => {
    setChatRecipeId(recipe?.recipeId ?? null)
    setChatRecipeTitle(recipe?.title.trim() || null)
    setChatRecipeArticle(recipe ?? null)
    setChatOpen(true)
  }, [])
  const closeChat = useCallback(() => setChatOpen(false), [])
  const isAdminArea =
    pathname.startsWith("/admin") || pathname.startsWith("/quan-tri")
  const isAuthPage = [
    "/dang-nhap",
    "/dang-ky",
    "/quen-mat-khau",
    "/xac-minh-email",
    "/dat-lai-mat-khau",
  ].includes(pathname)
  const canUseAssistant =
    account?.role !== "ADMIN" && !isAdminArea && !isAuthPage

  useEffect(() => {
    if (!canUseAssistant) setChatOpen(false)
  }, [canUseAssistant])

  const chatContext = useMemo<AiChatContextValue>(
    () => ({
      isOpen: chatOpen,
      recipeId: chatRecipeId,
      recipeTitle: chatRecipeTitle,
      recipeArticle: chatRecipeArticle,
      openChat,
      closeChat,
    }),
    [
      chatOpen,
      chatRecipeId,
      chatRecipeTitle,
      chatRecipeArticle,
      openChat,
      closeChat,
    ],
  )

  return (
    <AiChatContext.Provider value={chatContext}>
      {children}
      {canUseAssistant && !chatOpen && (
        <button
          type="button"
          onClick={() => openChat()}
          aria-label="Mở khung chat hỗ trợ AI"
          className="fixed bottom-5 right-5 z-40 flex h-12 w-12 items-center justify-center rounded-full bg-brand-600 text-on-brand shadow-lg shadow-brand-900/25 transition-all duration-200 hover:-translate-y-0.5 hover:bg-brand-500 hover:shadow-xl hover:scale-105 active:translate-y-0 active:scale-95 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
        >
          <MessageCircle
            className="h-6 w-6"
            aria-hidden="true"
            strokeWidth={2.2}
          />
        </button>
      )}
      {canUseAssistant && chatOpen && (
        <Suspense fallback={null}>
          <AiChatPage />
        </Suspense>
      )}
    </AiChatContext.Provider>
  )
}
