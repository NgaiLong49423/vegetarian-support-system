import { apiClient, asApiError } from '../lib/apiClient';

export type CommentAuthor = { userId: number; displayName: string; avatarUrl: string | null };

/** FR-46 comment; `content` and `author` are null for a tombstone (`deleted = true`). */
export interface RecipeComment {
  commentId: number;
  parentCommentId: number | null;
  depth: number;
  content: string | null;
  author: CommentAuthor | null;
  deleted: boolean;
  edited: boolean;
  createdAt: string;
  replies: RecipeComment[];
}

export interface RecipeCommentPage {
  content: RecipeComment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  commentCount: number;
}

/** Deepest level of a thread (BR-66); a reply to this level is attached one level up by the Backend. */
export const MAX_COMMENT_DEPTH = 5;
export const COMMENT_PAGE_SIZE = 10;

async function request<T>(operation: () => Promise<{ data: T }>): Promise<T> {
  try {
    return (await operation()).data;
  } catch (error) {
    throw asApiError(error);
  }
}

export const commentsApi = {
  list: (recipeId: number, page: number) => request(
    () => apiClient.get<RecipeCommentPage>(`/recipes/${recipeId}/comments`, { params: { page, size: COMMENT_PAGE_SIZE } }),
  ),
  create: (recipeId: number, content: string, parentCommentId?: number) => request(
    () => apiClient.post<RecipeComment>(`/recipes/${recipeId}/comments`,
      parentCommentId ? { content, parentCommentId } : { content }),
  ),
  update: (commentId: number, content: string) => request(
    () => apiClient.put<RecipeComment>(`/comments/${commentId}`, { content }),
  ),
  remove: (commentId: number) => request(() => apiClient.delete<void>(`/comments/${commentId}`)),
};
