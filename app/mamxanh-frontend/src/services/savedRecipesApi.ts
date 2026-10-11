import { apiClient } from "../lib/apiClient"

export type SavedRecipe = {
  recipeId: number
  title: string | null
  description: string | null
  coverUrl: string | null
  authorName: string | null
  available: boolean
  unavailableMessage: string | null
  savedAt: string
}

export type SavedRecipePage = {
  content: SavedRecipe[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const savedRecipesApi = {
  list: async (keyword = "", page = 0, size = 20) =>
    (
      await apiClient.get<SavedRecipePage>("/saved-recipes", {
        params: { keyword, page, size },
      })
    ).data,
  save: async (recipeId: number) => {
    await apiClient.put<void>(`/saved-recipes/${recipeId}`)
  },
  unsave: async (recipeId: number) => {
    await apiClient.delete<void>(`/saved-recipes/${recipeId}`)
  },
}
