import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react"
import { useAuth } from "./AuthContext"
import { savedRecipesApi } from "../services/savedRecipesApi"

type SavedContext = {
  ids: Set<number>
  loading: boolean
  error: boolean
  revision: number
  pending: Set<number>
  reload: () => void
  setSaved: (recipeId: number, saved: boolean) => Promise<void>
}
const Context = createContext<SavedContext | null>(null)

/** Share only saved reference IDs; every page continues reading current recipe data from Backend. */
export function SavedRecipesProvider({ children }: { children: ReactNode }) {
  const { account } = useAuth()
  const memberId = account && account.role !== "ADMIN" ? account.id : null
  const currentMember = useRef(memberId)
  currentMember.current = memberId
  const inFlight = useRef(new Set<number>())
  const [ownerId, setOwnerId] = useState<number | null>(null)
  const [ids, setIds] = useState(new Set<number>())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)
  const [revision, setRevision] = useState(0)
  const [refresh, setRefresh] = useState(0)
  const [pending, setPending] = useState(new Set<number>())

  useEffect(() => {
    let active = true
    setOwnerId(memberId)
    setIds(new Set())
    setError(false)
    inFlight.current.clear()
    setPending(new Set())
    if (memberId === null) {
      setLoading(false)
      return
    }
    setLoading(true)
    const readIds = async () => {
      const references = new Set<number>()
      let page = 0
      let totalPages = 1
      while (active && page < totalPages) {
        const response = await savedRecipesApi.list("", page, 50)
        response.content.forEach((item) => references.add(item.recipeId))
        totalPages = response.totalPages
        page += 1
      }
      if (active) setIds(references)
    }
    readIds()
      .catch(() => {
        if (active) setError(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [memberId, refresh])

  const setSaved = useCallback(
    async (recipeId: number, saved: boolean) => {
      if (memberId === null || inFlight.current.has(recipeId)) return
      inFlight.current.add(recipeId)
      setPending((current) => new Set(current).add(recipeId))
      try {
        await (saved
          ? savedRecipesApi.save(recipeId)
          : savedRecipesApi.unsave(recipeId))
        if (currentMember.current !== memberId) return
        setIds((current) => {
          const next = new Set(current)
          if (saved) next.add(recipeId)
          else next.delete(recipeId)
          return next
        })
        setRevision((current) => current + 1)
      } finally {
        if (currentMember.current === memberId) {
          inFlight.current.delete(recipeId)
          setPending((current) => {
            const next = new Set(current)
            next.delete(recipeId)
            return next
          })
        }
      }
    },
    [memberId],
  )

  return (
    <Context.Provider
      value={{
        ids: ownerId === memberId ? ids : new Set(),
        loading: loading || ownerId !== memberId,
        error: ownerId === memberId && error,
        pending: ownerId === memberId ? pending : new Set(),
        revision,
        reload: () => setRefresh((value) => value + 1),
        setSaved,
      }}
    >
      {children}
    </Context.Provider>
  )
}

export function useSavedRecipes() {
  const context = useContext(Context)
  if (!context)
    throw new Error("useSavedRecipes requires SavedRecipesProvider.")
  return context
}
