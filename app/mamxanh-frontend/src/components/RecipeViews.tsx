import { useEffect, useState } from 'react';
import { useAuth } from './AuthContext';
import { RecipeViewCount } from './RecipeViewCount';
import { recipesApi, type RecipeDetail } from '../api/recipes';
import { anonymousRecipeViewer } from '../lib/recipeViewer';

export function RecipeViews({ recipe }: { recipe: RecipeDetail }) {
  const recipeId = recipe.recipeId;
  const { account } = useAuth();
  // Keep the identity of this visit through retries and session rejection.
  // Clearing a rejected Member session must never trigger a second Guest event.
  const [viewerAccountId] = useState(account?.id);
  const [viewCount, setViewCount] = useState<number | undefined>(recipe.statistics?.viewCount);
  const [viewError, setViewError] = useState('');
  const [recordingView, setRecordingView] = useState(false);
  const [viewAttempt, setViewAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    setRecordingView(true);
    setViewError('');
    // Runs after the detail has rendered; a failed GET never records a view.
    Promise.resolve().then(() => {
      if (!active) return;
      return recipesApi.recordView(recipeId, viewerAccountId === undefined ? anonymousRecipeViewer() : undefined);
    }).then((result) => {
      if (active && result) setViewCount(result.viewCount);
    }).catch((error: unknown) => {
      if (active) setViewError(error instanceof Error ? error.message : 'Chưa thể ghi nhận lượt xem.');
    }).finally(() => { if (active) setRecordingView(false); });
    return () => { active = false; };
  }, [recipe, recipeId, viewerAccountId, viewAttempt]);

  return (
    <>
      <div className="mt-3 text-sm text-ink-muted" aria-live="polite" aria-atomic="true">
        <RecipeViewCount count={viewCount} />
      </div>
      {viewError && (
        <div className="mt-3 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">
          <p role="status">Chưa ghi nhận được lượt xem. {viewError}</p>
          <button type="button" disabled={recordingView} onClick={() => setViewAttempt((attempt) => attempt + 1)}
            className="mt-2 rounded-lg border border-amber-300 px-3 py-1.5 font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-700 disabled:opacity-50">
            Thử lại lượt xem
          </button>
        </div>
      )}
    </>
  );
}
