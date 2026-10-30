import { useCallback, useMemo, useState, type ReactNode } from "react"
import { useLocation } from "react-router-dom"
import { AppHeader } from "./AppHeader"
import { AppFooter } from "./AppFooter"
import {
  AiChatContext,
  type AiChatContextValue,
  type RecipeArticleContext,
} from "./AiChatContext"
import { AiChatPage } from "../pages/AiChatPage"

import { AdminLayout } from "./AdminLayout"

export function Layout({ children }: { children: ReactNode }) {
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

  if (
    [
      "/dang-nhap",
      "/dang-ky",
      "/quen-mat-khau",
      "/xac-minh-email",
      "/dat-lai-mat-khau",
    ].includes(pathname)
  ) {
    return <>{children}</>
  }

  // Pure Admin Workspace separation
  if (pathname.startsWith("/admin") || pathname.startsWith("/quan-tri")) {
    return <AdminLayout>{children}</AdminLayout>
  }

  return (
    <AiChatContext.Provider value={chatContext}>
      <div className="food-app flex min-h-screen flex-col bg-cream">
        <AppHeader />
        <main className="flex-1">{children}</main>
        <AppFooter />
        <AiChatPage />
      </div>
    </AiChatContext.Provider>
  )
}

export function PageContainer({
  children,
  className = "",
}: {
  children: ReactNode
  className?: string
}) {
  const hasCustomMaxW = /\bmax-w-/.test(className)
  return (
    <div
      className={`mx-auto w-full ${
        hasCustomMaxW ? "" : "max-w-[1344px]"
      } px-4 sm:px-6 lg:px-8 ${className}`}
    >
      {children}
    </div>
  )
}
