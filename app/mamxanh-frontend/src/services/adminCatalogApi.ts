import axios from 'axios';
import { apiClient } from '../lib/apiClient';

export type CatalogIngredient = {
  id: number;
  name: string;
  ingredientGroup: string;
  sourceName: string;
  sourceUrl: string | null;
  referenceDate: string;
  nutritionSupported: boolean;
  active: boolean;
};

export type CatalogUnit = {
  id: number;
  code: string;
  name: string;
  dimension: 'MASS' | 'VOLUME' | 'COUNT';
  baseFactor: number;
  active: boolean;
};

export type CatalogConversion = {
  ingredientId: number;
  unitId: number;
  gramsPerUnit: number;
  approximate: boolean;
  active: boolean;
};

type ApiResponse<T> = { success: boolean; data: T; message?: string };
type ApiProblem = { detail?: string; errors?: Array<{ field: string; message: string }> };

export class AdminCatalogApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
    this.name = 'AdminCatalogApiError';
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  try {
    const response = await apiClient.request<ApiResponse<T>>({
      url: path,
      method: init?.method ?? 'GET',
      ...(init?.body === undefined ? {} : { data: init.body }),
    });
    if (response.status === 204) return undefined as T;
    return response.data.data;
  } catch (cause) {
    if (!axios.isAxiosError<ApiProblem>(cause)) {
      throw cause instanceof Error ? cause : new Error('Không lưu được thay đổi.');
    }

    const status = cause.response?.status ?? 0;
    if (status === 401) {
      throw new AdminCatalogApiError('Bạn cần đăng nhập bằng tài khoản Administrator để dùng danh mục quản trị.', status);
    }
    if (status === 403) {
      throw new AdminCatalogApiError('Tài khoản hiện tại không có quyền quản lý danh mục (403 Forbidden).', status);
    }

    const problem = cause.response?.data;
    const fieldErrors = problem?.errors?.map(({ field, message }) => field + ': ' + message).join(' ');
    throw new Error(fieldErrors || problem?.detail || cause.message || 'Không lưu được thay đổi.');
  }
}

const json = (method: string, body?: unknown): RequestInit => ({
  method,
  ...(body === undefined ? {} : { body: JSON.stringify(body) }),
});

export const adminCatalogApi = {
  ingredients: (query = '') => request<CatalogIngredient[]>('/admin/ingredients' + (query ? '?query=' + encodeURIComponent(query) : '')),
  createIngredient: (body: Omit<CatalogIngredient, 'id' | 'nutritionSupported' | 'active'>) =>
    request<CatalogIngredient>('/admin/ingredients', json('POST', body)),
  updateIngredient: (id: number, body: Omit<CatalogIngredient, 'id' | 'nutritionSupported' | 'active'>) =>
    request<CatalogIngredient>('/admin/ingredients/' + id, json('PUT', body)),
  setIngredientActive: (id: number, active: boolean) =>
    request<CatalogIngredient>('/admin/ingredients/' + id + '/status', json('PATCH', { active })),
  units: () => request<CatalogUnit[]>('/admin/units'),
  createUnit: (body: Omit<CatalogUnit, 'id' | 'active'>) =>
    request<CatalogUnit>('/admin/units', json('POST', body)),
  updateUnit: (id: number, body: Omit<CatalogUnit, 'id' | 'active'>) =>
    request<CatalogUnit>('/admin/units/' + id, json('PUT', body)),
  setUnitActive: (id: number, active: boolean) =>
    request<CatalogUnit>('/admin/units/' + id + '/status', json('PATCH', { active })),
  conversions: () => request<CatalogConversion[]>('/admin/ingredient-unit-conversions'),
  saveConversion: (item: CatalogConversion, create: boolean) => {
    const path = '/admin/ingredients/' + item.ingredientId + '/unit-conversions/' + item.unitId;
    return request<CatalogConversion>(path, json(create ? 'POST' : 'PUT', {
      gramsPerUnit: item.gramsPerUnit,
      approximate: item.approximate,
    }));
  },
  setConversionActive: (item: CatalogConversion, active: boolean) =>
    request<CatalogConversion>('/admin/ingredients/' + item.ingredientId + '/unit-conversions/' + item.unitId + '/status', json('PATCH', { active })),
};