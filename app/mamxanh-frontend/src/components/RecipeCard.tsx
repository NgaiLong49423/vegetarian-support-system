import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  CalendarPlus,
  Check,
  Clock,
  Eye,
  Flame,
  Heart,
  Lock,
  ThumbsUp,
  Users,
} from 'lucide-react';
import type { Recipe } from '../types';
import { Badge } from './ui';
import { isSaved, toggleSaved, subscribeSaved } from '../lib/savedRecipes';
import { useAuth } from './AuthContext';
import { HeartCheckbox } from './HeartCheckbox';

export function RecipeCard({ recipe }: { recipe: Recipe }) {
  const [saved, setSaved] = useState(false);
  const [addedToPlan, setAddedToPlan] = useState(false);
  const [guestNotice, setGuestNotice] = useState<'save' | 'plan' | null>(null);
  const navigate = useNavigate();
  const { isAuthenticated: active } = useAuth();

  useEffect(() => {
    setSaved(isSaved(recipe.slug));
    const unsub = subscribeSaved(() => setSaved(isSaved(recipe.slug)));
    return unsub;
  }, [recipe.slug]);

  const handleToggleSave = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    // AC-01.5 / BR-05: Guest phải đăng nhập
    if (!active) {
      setGuestNotice('save');
      return;
    }

    toggleSaved({
      slug: recipe.slug,
      name: recipe.name,
      image: recipe.image,
      diet: recipe.diet,
      calories: recipe.calories,
      prepTime: recipe.prepTime,
      cookTime: recipe.cookTime,
      author: recipe.author.name,
    });
  };

  const handleAddToPlan = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    // AC-01.5 / BR-05: Guest phải đăng nhập
    if (!active) {
      setGuestNotice('plan');
      return;
    }

    // AC-17.4: thêm món vào lịch ăn — mô phỏng hành vi UI
    setAddedToPlan(true);
    setTimeout(() => setAddedToPlan(false), 2000);
  };

  return (
      <Link
          to={`/cong-thuc/${recipe.slug}`}
          className="group flex flex-col overflow-hidden rounded-2xl border border-brand-100 bg-white transition-all hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl hover:shadow-brand-900/5"
      >
        <div className="relative aspect-[4/3] overflow-hidden">
          <img
              src={recipe.image}
              alt={recipe.name}
              className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />

          {/* Badge loại chay — góc trên trái */}
          <div className="absolute left-3 top-3">
            <Badge tone="leaf" soft={false}>
              {recipe.diet}
            </Badge>
          </div>

          {/* Nút Lưu — góc trên phải */}
          <button
              type="button"
              onClick={handleToggleSave}
              className={`absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full shadow-sm backdrop-blur transition-all hover:scale-105 ${
                  saved
                      ? 'bg-brand-600 text-white'
                      : 'bg-white/90 text-ink-soft hover:text-brand-600'
              }`}
              aria-label={saved ? 'Bỏ lưu công thức' : 'Lưu công thức'}
              title={saved ? 'Bỏ lưu' : 'Lưu công thức'}
          >
            <Heart className={`h-[18px] w-[18px] ${saved ? 'fill-current text-white' : ''}`} />
          </button>
        </div>

        <div className="flex flex-1 flex-col p-4">
          <div className="mb-2 flex items-center gap-2 text-sm">
            {recipe.likePercentage !== undefined ? (
                <span className="inline-flex items-center gap-1 rounded-full bg-leaf-50 px-2 py-0.5 text-xs font-bold text-leaf-700">
              <ThumbsUp className="h-3.5 w-3.5 text-leaf-600" /> {recipe.likePercentage}%
            </span>
            ) : (
                <span className="inline-flex items-center gap-1 rounded-full bg-brand-50 px-2 py-0.5 text-xs font-bold text-brand-700">
              Mới
            </span>
            )}
            <span className="ml-auto text-xs font-semibold text-brand-600">{recipe.category}</span>
          </div>

          <h3 className="mb-3 line-clamp-2 font-bold leading-snug text-ink transition-colors group-hover:text-brand-700">
            {recipe.name}
          </h3>

          {/* Meta: thời gian + kcal + servings + view_count (AC-01.1) */}
          <div className="mt-auto flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-ink-muted">
          <span className="inline-flex items-center gap-1">
            <Clock className="h-3.5 w-3.5" /> ≈ {recipe.prepTime + recipe.cookTime} phút
          </span>
            <span className="inline-flex items-center gap-1">
            <Flame className="h-3.5 w-3.5" /> ≈ {recipe.calories} kcal
          </span>
            <span className="inline-flex items-center gap-1">
            <Users className="h-3.5 w-3.5" /> {recipe.servings} người
          </span>
            <span className="inline-flex items-center gap-1">
            <Eye className="h-3.5 w-3.5" /> {recipe.viewCount.toLocaleString('vi-VN')} lượt xem
          </span>
          </div>

          <div className="mt-3 flex items-center gap-2 border-t border-brand-50 pt-3">
            <img src={recipe.author.avatar} alt="" className="h-6 w-6 rounded-full object-cover" />
            <span className="truncate text-xs font-medium text-ink-soft">{recipe.author.name}</span>
          </div>

          {/* Nút "Thêm vào lịch ăn" — theo AC-17.4 */}
          <button
              onClick={handleAddToPlan}
              className={`mt-3 flex w-full items-center justify-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold transition-colors ${
                  addedToPlan
                      ? 'bg-leaf-100 text-leaf-700'
                      : 'bg-brand-50 text-brand-700 hover:bg-brand-100'
              }`}
              title="Thêm món vào lịch ăn tuần"
          >
            {addedToPlan ? (
                <>
                  <Check className="h-3.5 w-3.5" /> Đã thêm vào lịch ăn
                </>
            ) : (
                <>
                  <CalendarPlus className="h-3.5 w-3.5" /> Thêm vào lịch ăn
                </>
            )}
          </button>

          {/* Inline notice cho Guest — AC-01.5 / BR-05 */}
          {guestNotice && (
              <div
                  role="alert"
                  className="mt-3 flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 p-2.5 text-[11px] leading-relaxed text-amber-800"
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    navigate('/dang-nhap');
                  }}
              >
                <Lock className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-600" />
                <div className="flex-1">
                  <p className="font-semibold">
                    {guestNotice === 'save'
                        ? 'Vui lòng đăng nhập để lưu công thức.'
                        : 'Vui lòng đăng nhập để thêm vào lịch ăn.'}
                  </p>
                  <span className="mt-0.5 inline-block font-bold text-amber-900 underline">
                Đăng nhập ngay →
              </span>
                </div>
              </div>
          )}
        </div>
      </Link>
  );
}
