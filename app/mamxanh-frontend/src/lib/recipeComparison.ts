import { useEffect, useState } from 'react';
import { asApiError } from './apiClient';
import { recipesApi, type RecipeDetail } from '../api/recipes';

// FR-60: the comparison keeps its two recipe IDs in the URL (`a` = first, `b` = second) and
// stores nothing else. The picker page fills one side and returns to the comparison page.

export type ComparisonSide = 'first' | 'second';

export type PublishedRecipeState =
  | { status: 'empty' }
  | { status: 'loading' }
  | { status: 'ready'; recipe: RecipeDetail }
  | { status: 'unavailable' }
  | { status: 'error'; message: string };

export function parseRecipeId(value: string | null) {
  if (!value || !/^\d{1,15}$/.test(value)) return null;
  const id = Number(value);
  return id > 0 ? id : null;
}

function withIds(path: string, firstId: number | null, secondId: number | null, extra: Record<string, string> = {}) {
  const params = new URLSearchParams();
  if (firstId !== null) params.set('a', String(firstId));
  if (secondId !== null) params.set('b', String(secondId));
  Object.entries(extra).forEach(([key, value]) => params.set(key, value));
  const query = params.toString();
  return query ? `${path}?${query}` : path;
}

export function comparisonHref(firstId: number | null, secondId: number | null) {
  return withIds('/so-sanh', firstId, secondId);
}

/** The picker fills the second recipe unless it is asked to replace the first one (or no first recipe exists). */
export function pickerHref(side: ComparisonSide, firstId: number | null, secondId: number | null) {
  return withIds('/so-sanh/chon-mon', firstId, secondId, side === 'first' && firstId !== null ? { doi: 'dau-tien' } : {});
}

export function pickerSide(params: URLSearchParams, firstId: number | null): ComparisonSide {
  return params.get('doi') === 'dau-tien' || firstId === null ? 'first' : 'second';
}

/** Loads one public recipe (FR-20 detail with FR-39 nutrition); a 404 means it is no longer public. */
export function usePublishedRecipe(recipeId: number | null) {
  const [state, setState] = useState<PublishedRecipeState>({ status: recipeId === null ? 'empty' : 'loading' });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (recipeId === null) {
      setState({ status: 'empty' });
      return;
    }
    let active = true;
    setState({ status: 'loading' });
    recipesApi.getPublished(recipeId)
      .then((recipe) => { if (active) setState({ status: 'ready', recipe }); })
      .catch((cause: unknown) => {
        if (!active) return;
        const error = asApiError(cause);
        setState(error.status === 404 ? { status: 'unavailable' } : { status: 'error', message: error.message });
      });
    return () => { active = false; };
  }, [recipeId, attempt]);
  return { state, retry: () => setAttempt((value) => value + 1) };
}
