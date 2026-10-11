import axios from 'axios';
import { apiClient } from '../lib/apiClient';

export interface UploadedImage {
  url: string;
  fileName: string;
  contentType: string;
  size: number;
}

export interface RecipeMediaItem {
  id?: number;
  recipeId?: number;
  mediaUrl: string;
  mimeType: string;
  displayOrder: number;
  isCover: boolean;
}

export interface UpdateRecipeMediaPayload {
  items: Array<{
    mediaUrl: string;
    mimeType: string;
    displayOrder: number;
    isCover: boolean;
  }>;
}

type ApiResponse<T> = {
  code?: number;
  message?: string;
  result: T;
};

const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

export function validateImageFile(file: File): string | null {
  if (!ALLOWED_MIME_TYPES.includes(file.type)) {
    return 'Định dạng tệp không hợp lệ. Chỉ chấp nhận định dạng JPEG, PNG hoặc WebP.';
  }
  if (file.size > MAX_FILE_SIZE) {
    return 'Dung lượng tệp vượt quá giới hạn tối đa 5 MB.';
  }
  return null;
}

export const recipeMediaApi = {
  async uploadImage(file: File): Promise<UploadedImage> {
    const errorMsg = validateImageFile(file);
    if (errorMsg) {
      throw new Error(errorMsg);
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<ApiResponse<UploadedImage>>(
      '/recipes/media/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      },
    );

    return response.data.result;
  },

  async getRecipeMedia(recipeId: number): Promise<RecipeMediaItem[]> {
    const response = await apiClient.get<ApiResponse<RecipeMediaItem[]>>(
      `/recipes/${recipeId}/media`,
    );
    return response.data.result;
  },

  async updateRecipeMedia(
    recipeId: number,
    payload: UpdateRecipeMediaPayload,
  ): Promise<RecipeMediaItem[]> {
    const response = await apiClient.put<ApiResponse<RecipeMediaItem[]>>(
      `/recipes/${recipeId}/media`,
      payload,
    );
    return response.data.result;
  },

  async deleteRecipeMedia(recipeId: number): Promise<void> {
    await apiClient.delete(`/recipes/${recipeId}/media`);
  },
};
