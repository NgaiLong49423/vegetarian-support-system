/**
 * Ingredient image resolver and group icon mapping.
 * Connects ingredient records with existing public demo assets (/assets/*.png)
 * and recipe imagery from mockData.
 */

import { normalizeIngredientGroup } from './vietnameseText';

export interface IngredientVisualInfo {
  imageUrl: string | null;
  groupIcon: 'apple' | 'salad' | 'layers' | 'wheat' | 'nut' | 'sparkles' | 'carrot' | 'leaf';
  accentColor: string;
}

// Normalized mapping to existing demo assets and recipe photographs
const INGREDIENT_IMAGE_MAP: Record<string, string> = {
  // Demo assets from R__demo_sample_data.sql & public/assets
  'đậu hũ': '/assets/95427.png',
  'đậu hũ non': '/assets/95427.png',
  'đậu hũ non nhật bản': '/assets/95427.png',
  'đậu phụ': '/assets/95427.png',
  'cà chua': '/assets/95427.png',
  'rau muống': '/assets/cddd4.png',
  'tỏi': '/assets/cddd4.png',
  'tiêu đen': '/assets/40c2c.png',
  'tiêu xanh': '/assets/40c2c.png',
  'tiêu xanh phú quốc': '/assets/40c2c.png',
  'nấm đùi gà': '/assets/40c2c.png',
  'nấm đùi gà tươi': '/assets/40c2c.png',
  'nấm hương': '/assets/40053.png',
  'nấm hương rừng': '/assets/40053.png',
  'nấm đông cô': '/assets/40053.png',
  'nấm đông cô tươi': '/assets/40053.png',
  'nấm bào ngư': '/assets/40053.png',
  'nấm rơm': '/assets/40053.png',
  'nấm rơm tươi': '/assets/40053.png',
  'cà rốt': '/assets/40053.png',
  'cà rốt & dưa leo': '/assets/40053.png',
  'khoai lang': '/assets/8ef48.png',
  'bông cải xanh': '/assets/09723.png',
  'đậu lăng': '/assets/5a53c.png',

  // Mock recipe ingredients with existing photography
  'chuối': 'https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=600&auto=format&fit=crop&q=80',
  'chuối tây': 'https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=600&auto=format&fit=crop&q=80',
  'bơ sáp': 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=600&auto=format&fit=crop&q=80',
  'bơ sáp chín': 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=600&auto=format&fit=crop&q=80',
  'quinoa': 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=600&auto=format&fit=crop&q=80',
  'quinoa chín': 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=600&auto=format&fit=crop&q=80',
  'bánh phở tươi': 'https://images.unsplash.com/photo-1555126634-323283e090fa?w=600&auto=format&fit=crop&q=80',
  'bánh phở': 'https://images.unsplash.com/photo-1555126634-323283e090fa?w=600&auto=format&fit=crop&q=80',
  'bánh tráng gạo': 'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=600&auto=format&fit=crop&q=80',
  'bánh tráng': 'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=600&auto=format&fit=crop&q=80',
  'rau xà lách': 'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=600&auto=format&fit=crop&q=80',
  'hạt điều rang': 'https://images.unsplash.com/photo-1570368336224-455084c1fb31?w=600&auto=format&fit=crop&q=80',
  'hạt điều': 'https://images.unsplash.com/photo-1570368336224-455084c1fb31?w=600&auto=format&fit=crop&q=80',
  'khoai tây': 'https://images.unsplash.com/photo-1570368336224-455084c1fb31?w=600&auto=format&fit=crop&q=80',
  'gạo tẻ': 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=600&auto=format&fit=crop&q=80',
  'rong biển khô': 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=600&auto=format&fit=crop&q=80',
  'rong biển': 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=600&auto=format&fit=crop&q=80',
  'hạt sen tươi': 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=600&auto=format&fit=crop&q=80',
  'hạt sen': 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=600&auto=format&fit=crop&q=80',
  'đậu bắp non': 'https://images.unsplash.com/photo-1547592180-85f173990554?w=600&auto=format&fit=crop&q=80',
  'đậu bắp': 'https://images.unsplash.com/photo-1547592180-85f173990554?w=600&auto=format&fit=crop&q=80',
  'hành boa-rô': '/assets/cddd4.png',
};

/**
 * Resolves the image URL for an ingredient name by matching exact or partial keywords.
 * Returns null if no reliable match exists.
 */
export function getIngredientImageUrl(name: string): string | null {
  if (!name) return null;
  const normalized = name.trim().toLowerCase();

  // 1. Direct key match
  if (INGREDIENT_IMAGE_MAP[normalized]) {
    return INGREDIENT_IMAGE_MAP[normalized];
  }

  // 2. Keyword match (longest matching substring)
  let bestMatch: string | null = null;
  let bestMatchLength = 0;

  for (const [key, url] of Object.entries(INGREDIENT_IMAGE_MAP)) {
    if (normalized.includes(key) && key.length > bestMatchLength) {
      bestMatch = url;
      bestMatchLength = key.length;
    }
  }

  return bestMatch;
}

/**
 * Determines appropriate icon and visual styling for an ingredient group.
 */
export function getIngredientGroupVisual(group: string) {
  const normalized = normalizeIngredientGroup(group);
  const g = normalized.toLowerCase();
  if (g.includes('trái cây') || g.includes('hoa quả')) {
    return { icon: 'apple' as const, label: 'Trái cây', badgeClass: 'bg-amber-100/80 text-amber-900 border-amber-200/70' };
  }
  if (g.includes('hạt') || g.includes('các loại hạt')) {
    return { icon: 'nut' as const, label: 'Các loại hạt', badgeClass: 'bg-orange-100/80 text-orange-900 border-orange-200/70' };
  }
  if (g.includes('ngũ cốc') || g.includes('bột')) {
    return { icon: 'wheat' as const, label: 'Ngũ cốc', badgeClass: 'bg-yellow-100/80 text-yellow-900 border-yellow-200/70' };
  }
  if (g.includes('đậu') || g.includes('chế phẩm')) {
    return { icon: 'layers' as const, label: 'Đậu & Chế phẩm', badgeClass: 'bg-emerald-100/80 text-emerald-900 border-emerald-200/70' };
  }
  if (g.includes('gia vị')) {
    return { icon: 'sparkles' as const, label: 'Gia vị chay', badgeClass: 'bg-purple-100/80 text-purple-900 border-purple-200/70' };
  }
  return { icon: 'salad' as const, label: normalized || 'Rau củ', badgeClass: 'bg-brand-100/80 text-brand-900 border-brand-200/70' };
}
