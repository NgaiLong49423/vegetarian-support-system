import { apiClient, ApiError } from '../lib/apiClient';

export interface UploadedImage {
  blobUrl: string;
  mimeType: string;
  sizeBytes: number;
}

export interface RecipeMediaItem {
  mediaId?: number;
  recipeId?: number;
  blobUrl: string;
  mimeType: string;
  displayOrder: number;
  isCover: boolean;
}

export interface UpdateRecipeMediaPayload {
  mediaItems: Array<{
    blobUrl: string;
    mimeType: string;
    displayOrder: number;
    isCover: boolean;
  }>;
}

type ApiResponse<T> = {
  success: boolean;
  message?: string;
  data: T;
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
    const uploaded = response.data.data;
    if (
      !uploaded ||
      typeof uploaded.blobUrl !== 'string' ||
      !uploaded.blobUrl.startsWith('https://')
    ) {
      throw new ApiError(
        'Phản hồi tải ảnh không hợp lệ. Vui lòng thử lại.',
        502,
      );
    }
    return uploaded;
  },

  async getRecipeMedia(recipeId: number): Promise<RecipeMediaItem[]> {
    const response = await apiClient.get<ApiResponse<RecipeMediaItem[]>>(
      `/recipes/${recipeId}/media`,
    );
    return response.data.data;
  },

  async updateRecipeMedia(
    recipeId: number,
    payload: UpdateRecipeMediaPayload,
  ): Promise<RecipeMediaItem[]> {
    const response = await apiClient.put<ApiResponse<RecipeMediaItem[]>>(
      `/recipes/${recipeId}/media`,
      payload,
    );
    return response.data.data;
  },

  async deleteRecipeMedia(recipeId: number): Promise<void> {
    await apiClient.delete(`/recipes/${recipeId}/media`);
  },
};
