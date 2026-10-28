import { apiClient } from "../lib/apiClient"

export interface ShoppingItem {
  id: number
  ingredientId: number | null
  name: string
  ingredientGroup: string
  imageUrl: string | null
  quantity: number
  unitId: number
  unitCode: string
  unitName: string
  bought: boolean
  sources?: Array<{
    recipeId: number
    title: string | null
    available: boolean
    originalQuantity: number
    originalUnitId: number
    originalUnitCode: string | null
  }>
}
export interface ShoppingSummary {
  id: number
  name: string
  createdAt: string
  updatedAt: string
}
export interface ShoppingChecklist extends ShoppingSummary {
  items: ShoppingItem[]
}
export interface ShoppingWarning {
  code: string
  message: string
  mealPlanEntryId: number | null
  recipeId: number | null
}
export interface ShoppingCreation {
  shoppingList: ShoppingChecklist
  warnings: ShoppingWarning[]
  incompatibleItemIds: number[]
}
export interface ShoppingUnit {
  id: number
  code: string
  name: string
  active: boolean
}
interface ShoppingPage {
  content: ShoppingSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
type Quantity = { quantity: number; unitId: number }
export type ShoppingManualItemInput = Quantity & { name: string }
export interface AddRecipeShoppingOptions {
  selectedIngredientIds?: number[]
  manualItems?: ShoppingManualItemInput[]
}

export const shoppingApi = {
  createEmpty: async (name: string) =>
    (await apiClient.post<ShoppingChecklist>("/shopping-lists", { name })).data,
  addRecipe: async (listId: number, recipeId: number, servings: number, options: AddRecipeShoppingOptions = {}) =>
    (
      await apiClient.post<ShoppingCreation>(`/shopping-lists/${listId}/recipes`, {
        recipeId,
        servings,
        ...options,
      })
    ).data,
  createFromRecipe: async (name: string, recipeId: number, servings: number, options: AddRecipeShoppingOptions = {}) =>
    (
      await apiClient.post<ShoppingCreation>("/shopping-lists/from-recipe", {
        name,
        recipeId,
        servings,
        ...options,
      })
    ).data,
  list: async (page = 0) =>
    (
      await apiClient.get<ShoppingPage>("/shopping-lists", {
        params: { page, size: 20 },
      })
    ).data,
  get: async (id: number) =>
    (await apiClient.get<ShoppingChecklist>(`/shopping-lists/${id}`)).data,
  renameList: async (id: number, name: string) =>
    (await apiClient.patch<ShoppingSummary>(`/shopping-lists/${id}`, { name })).data,
  removeList: async (id: number) => {
    await apiClient.delete(`/shopping-lists/${id}`)
  },
  options: async () =>
    (await apiClient.get<ShoppingUnit[]>("/shopping-lists/options")).data,
  create: async (weekStartDate: string, selectedDates?: string[]) =>
    (
      await apiClient.post<ShoppingCreation>("/shopping-lists/from-meal-plan", {
        weekStartDate,
        ...(selectedDates ? { selectedDates } : {}),
      })
    ).data,
  add: async (id: number, item: Quantity & { name: string }) =>
    (await apiClient.post<ShoppingItem>(`/shopping-lists/${id}/items`, item))
      .data,
  update: async (id: number, itemId: number, item: Quantity) =>
    (
      await apiClient.put<ShoppingItem>(
        `/shopping-lists/${id}/items/${itemId}`,
        item,
      )
    ).data,
  bought: async (id: number, itemId: number, bought: boolean) =>
    (
      await apiClient.patch<ShoppingItem>(
        `/shopping-lists/${id}/items/${itemId}/bought`,
        { bought },
      )
    ).data,
  remove: async (id: number, itemId: number) => {
    await apiClient.delete(`/shopping-lists/${id}/items/${itemId}`)
  },
  clearBought: async (id: number) => {
    await apiClient.delete(`/shopping-lists/${id}/items`, {
      params: { bought: true },
    })
  },
}
