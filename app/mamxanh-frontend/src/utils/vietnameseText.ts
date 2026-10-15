/**
 * Vietnamese text normalization and Mojibake repair utility.
 * Fixes character encoding corruptions (e.g. UTF-8 read as ISO-8859-1/Windows-1252)
 * for ingredient groups and reference data source names.
 */

export const STANDARD_INGREDIENT_GROUPS = [
  'Rau củ',
  'Đậu & Chế phẩm',
  'Ngũ cốc',
  'Các loại hạt',
  'Trái cây',
  'Gia vị chay',
] as const;

export const STANDARD_DATA_SOURCES = [
  'USDA FoodData Central',
  'NIH/Viện Dinh dưỡng',
] as const;

const RE_DAU_CHE_PHAM = /đậu|chế phẩm|che pham|cháº¿|pháº©m|áº­u|ä|ch\?\s*ph\?m|\?u\s*&/i;
const RE_CAC_LOAI_HAT = /hạt|háº¡t|loại hạt|loáº¡i|cã¡c|lo\?\s*i\s*h\?t/i;
const RE_NGU_COC = /ngũ cốc|ngũ|ngu coc|ngå©|cá»|ng\?\s*c\?c/i;
const RE_TRAI_CAY = /trái cây|trái|trã¡i|cã¢y|hoa quả|tri cy/i;
const RE_GIA_VI = /gia vị|gia vá|gia vi|gia v\?/i;
const RE_RAU_CU = /rau|củ|cá»§|rau\s*c\?/i;
const RE_KHAC = /khác|khc/i;

/**
 * Normalizes an ingredient group string, repairing mojibake and encoding corruptions.
 */
export function normalizeIngredientGroup(group: string | null | undefined): string {
  if (!group) return 'Rau củ';
  const trimmed = group.trim().normalize('NFC');

  if (RE_DAU_CHE_PHAM.test(trimmed)) return 'Đậu & Chế phẩm';
  if (RE_CAC_LOAI_HAT.test(trimmed)) return 'Các loại hạt';
  if (RE_NGU_COC.test(trimmed)) return 'Ngũ cốc';
  if (RE_TRAI_CAY.test(trimmed)) return 'Trái cây';
  if (RE_GIA_VI.test(trimmed)) return 'Gia vị chay';
  if (RE_RAU_CU.test(trimmed)) return 'Rau củ';
  if (RE_KHAC.test(trimmed)) return 'Khác';

  return trimmed;
}

const RE_NIH = /nih|viện dinh dưỡng|viá»‡n|dæ°á»¡ng|dinh dưỡng|dinh duong|vi\?n/i;
const RE_USDA = /usda|fooddata/i;

/**
 * Normalizes a nutrition data source name, repairing mojibake and encoding corruptions.
 */
export function normalizeSourceName(source: string | null | undefined): string {
  if (!source) return 'USDA FoodData Central';
  const trimmed = source.trim().normalize('NFC');

  if (RE_NIH.test(trimmed)) return 'NIH/Viện Dinh dưỡng';
  if (RE_USDA.test(trimmed)) return 'USDA FoodData Central';

  return trimmed;
}
