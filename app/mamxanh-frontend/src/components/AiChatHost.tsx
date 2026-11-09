import {
  lazy,
  Suspense,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react"
import { MessageCircle } from "lucide-react"
import { useLocation } from "react-router-dom"
import { useAuth } from "./AuthContext"
import type { AiRecipeFormContext, AiRecipeFormPatch, AiRecipeFormPatchAck } from "../api/recipes"
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
  const [chatInitialized, setChatInitialized] = useState(false)
  const formBridge = useRef<{ snapshot: () => AiRecipeFormContext | null; apply: (patch: AiRecipeFormPatch) => Promise<AiRecipeFormPatchAck> } | null>(null)
  const registerRecipeForm = useCallback((snapshot: () => AiRecipeFormContext | null, apply: (patch: AiRecipeFormPatch) => Promise<AiRecipeFormPatchAck>) => {
    const bridge = { snapshot, apply }
    formBridge.current = bridge
    return () => { if (formBridge.current === bridge) formBridge.current = null }
  }, [])
  const getRecipeFormContext = useCallback(() => pathname === "/dang-cong-thuc" ? formBridge.current?.snapshot() ?? null : null, [pathname])
  const applyRecipeFormPatch = useCallback((patch: AiRecipeFormPatch) => pathname === "/dang-cong-thuc" && formBridge.current
    ? formBridge.current.apply(patch) : Promise.resolve({ status: "CONFLICT" as const }), [pathname])
  const [chatRecipeId, setChatRecipeId] = useState<number | null>(null)
  const [chatRecipeTitle, setChatRecipeTitle] = useState<string | null>(null)
  const [chatRecipeArticle, setChatRecipeArticle] =
    useState<RecipeArticleContext | null>(null)
  const [pageRecipeArticle, setPageRecipeArticle] =
    useState<RecipeArticleContext | null>(null)
  const [chatRecipePath, setChatRecipePath] = useState<string | null>(null)

  const routeRecipeMatch = /^\/cong-thuc\/id\/(\d+)\/?$/.exec(pathname)
  const parsedRouteId = routeRecipeMatch ? Number(routeRecipeMatch[1]) : null
  const routeRecipeId = parsedRouteId !== null && Number.isSafeInteger(parsedRouteId) && parsedRouteId > 0
    ? parsedRouteId : null
  const activeRecipeId = routeRecipeId ?? (chatRecipePath === pathname ? chatRecipeId : null)
  const activeArticle = chatRecipeArticle?.recipeId === activeRecipeId
    ? chatRecipeArticle
    : pageRecipeArticle?.recipeId === activeRecipeId ? pageRecipeArticle : null

  const openChat = useCallback((recipe?: RecipeArticleContext) => {
    if (recipe) {
      setChatRecipeId(recipe.recipeId)
      setChatRecipeTitle(recipe.title.trim() || null)
      setChatRecipeArticle(recipe)
      setChatRecipePath(pathname)
    }
    setChatInitialized(true)
    setChatOpen(true)
  }, [pathname])
  const closeChat = useCallback(() => setChatOpen(false), [])
  const startNewChat = useCallback(() => {
    setChatRecipeId(null)
    setChatRecipeTitle(null)
    setChatRecipeArticle(null)
    setChatRecipePath(null)
    setChatInitialized(true)
    setChatOpen(true)
  }, [])
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
      recipeId: activeRecipeId,
      recipeTitle: activeArticle?.title ?? (activeRecipeId === chatRecipeId ? chatRecipeTitle : null),
      recipeArticle: activeArticle,
      setPageRecipeArticle,
      openChat,
      closeChat,
      startNewChat,
      registerRecipeForm,
      getRecipeFormContext,
      applyRecipeFormPatch,
    }),
    [
      chatOpen,
      activeRecipeId,
      chatRecipeTitle,
      activeArticle,
      setPageRecipeArticle,
      openChat,
      closeChat,
      startNewChat,
      registerRecipeForm,
      getRecipeFormContext,
      applyRecipeFormPatch,
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
      {canUseAssistant && chatInitialized && (
        <Suspense fallback={null}>
          <AiChatPage />
        </Suspense>
      )}
    </AiChatContext.Provider>
  )
}
