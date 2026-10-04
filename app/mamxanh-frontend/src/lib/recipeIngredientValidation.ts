export const RECIPE_INGREDIENT_UNITS = [
  { code: 'g', label: 'g', dimension: 'MASS' },
  { code: 'kg', label: 'kg', dimension: 'MASS' },
  { code: 'ml', label: 'ml', dimension: 'VOLUME' },
  { code: 'L', label: 'l', dimension: 'VOLUME' },
  { code: 'tbsp', label: 'muỗng canh', dimension: 'VOLUME' },
  { code: 'tsp', label: 'thìa cà phê', dimension: 'VOLUME' },
  { code: 'cup', label: 'chén/cốc', dimension: 'VOLUME' },
  { code: 'quả', label: 'quả', dimension: 'COUNT' },
  { code: 'củ', label: 'củ', dimension: 'COUNT' },
  { code: 'bìa', label: 'bìa', dimension: 'COUNT' },
  { code: 'lá', label: 'lá', dimension: 'COUNT' },
  { code: 'nhánh', label: 'nhánh', dimension: 'COUNT' },
  { code: 'trái', label: 'trái', dimension: 'COUNT' },
  { code: 'miếng', label: 'miếng', dimension: 'COUNT' },
  { code: 'bó', label: 'bó', dimension: 'COUNT' },
] as const;

export interface IngredientQuantityInput {
  quantity: string;
  unit: string;
}

export function parsePositiveQuantity(value: string): number | null {
  const normalized = value.trim().replace(',', '.');
  if (!/^\d+(?:\.\d+)?$/.test(normalized)) return null;

  const quantity = Number(normalized);
  return Number.isFinite(quantity) && quantity > 0 ? quantity : null;
}

/**
 * Applies the agreed direct-mass rule in normalized grams. Other dimensions are
 * checked for a positive numeric quantity here; ingredient-specific conversion
 * remains a backend responsibility once the recipe API is integrated.
 */
export function validateIngredientQuantity({ quantity, unit }: IngredientQuantityInput): string | null {
  const parsedQuantity = parsePositiveQuantity(quantity);
  if (parsedQuantity === null) return 'Nhập số lượng lớn hơn 0.';

  if (unit === 'g' || unit === 'kg') {
    const grams = parsedQuantity * (unit === 'kg' ? 1000 : 1);
    if (!Number.isInteger(grams) || grams < 100 || grams % 100 !== 0) {
      return 'Khối lượng g/kg phải tối thiểu 100g và theo bước 100g (ví dụ 100g, 200g, 1kg).';
    }
  }

  if (!RECIPE_INGREDIENT_UNITS.some((option) => option.code === unit)) {
    return 'Chọn đơn vị đang có trong danh mục UNIT.';
  }

  return null;
}
