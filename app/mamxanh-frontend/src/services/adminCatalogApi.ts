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

const apiBase = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/$/, '');

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${apiBase}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    });
  } catch {
    throw new Error('Không thể kết nối Backend. Hãy kiểm tra dịch vụ và thử tải lại.');
  }

  if (!response.ok) {
    if (response.status === 401) {
      throw new AdminCatalogApiError('Bạn cần đăng nhập bằng tài khoản Administrator để dùng danh mục quản trị.', response.status);
    }
    if (response.status === 403) {
      throw new AdminCatalogApiError('Tài khoản hiện tại không có quyền quản lý danh mục (403 Forbidden).', response.status);
    }

    let problem: ApiProblem = {};
    try {
      problem = await response.json() as ApiProblem;
    } catch {
      // Keep a useful status-based message when the server response is not JSON.
    }
    const fieldErrors = problem.errors?.map(({ field, message }) => `${field}: ${message}`).join(' ');
    throw new Error(fieldErrors || problem.detail || `Backend trả về lỗi ${response.status}.`);
  }

  if (response.status === 204) return undefined as T;
  const body = await response.json() as ApiResponse<T>;
  return body.data;
}

const json = (method: string, body?: unknown): RequestInit => ({
  method,
  ...(body === undefined ? {} : { body: JSON.stringify(body) }),
});

export const adminCatalogApi = {
  ingredients: (query = '') => request<CatalogIngredient[]>(`/admin/ingredients${query ? `?query=${encodeURIComponent(query)}` : ''}`),
  createIngredient: (body: Omit<CatalogIngredient, 'id' | 'nutritionSupported' | 'active'>) =>
    request<CatalogIngredient>('/admin/ingredients', json('POST', body)),
  updateIngredient: (id: number, body: Omit<CatalogIngredient, 'id' | 'nutritionSupported' | 'active'>) =>
    request<CatalogIngredient>(`/admin/ingredients/${id}`, json('PUT', body)),
  setIngredientActive: (id: number, active: boolean) =>
    request<CatalogIngredient>(`/admin/ingredients/${id}/status`, json('PATCH', { active })),
  units: () => request<CatalogUnit[]>('/admin/units'),
  createUnit: (body: Omit<CatalogUnit, 'id' | 'active'>) =>
    request<CatalogUnit>('/admin/units', json('POST', body)),
  updateUnit: (id: number, body: Omit<CatalogUnit, 'id' | 'active'>) =>
    request<CatalogUnit>(`/admin/units/${id}`, json('PUT', body)),
  setUnitActive: (id: number, active: boolean) =>
    request<CatalogUnit>(`/admin/units/${id}/status`, json('PATCH', { active })),
  conversions: () => request<CatalogConversion[]>('/admin/ingredient-unit-conversions'),
  saveConversion: (item: CatalogConversion, create: boolean) => {
    const path = `/admin/ingredients/${item.ingredientId}/unit-conversions/${item.unitId}`;
    return request<CatalogConversion>(path, json(create ? 'POST' : 'PUT', {
      gramsPerUnit: item.gramsPerUnit,
      approximate: item.approximate,
    }));
  },
  setConversionActive: (item: CatalogConversion, active: boolean) =>
    request<CatalogConversion>(`/admin/ingredients/${item.ingredientId}/unit-conversions/${item.unitId}/status`, json('PATCH', { active })),
};
