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

/**
 * Normalizes an ingredient group string, repairing mojibake and encoding corruptions.
 */
export function normalizeIngredientGroup(group: string | null | undefined): string {
  if (!group) return 'Rau củ';
  const trimmed = group.trim().normalize('NFC');
  const lower = trimmed.toLowerCase();

  // Đậu & Chế phẩm
  if (
    lower.includes('đậu') ||
    lower.includes('chế phẩm') ||
    lower.includes('che pham') ||
    lower.includes('cháº¿') ||
    lower.includes('pháº©m') ||
    lower.includes('áº­u') ||
    lower.includes('ä') ||
    lower.includes('ch? ph?m') ||
    lower.includes('?u &')
  ) {
    return 'Đậu & Chế phẩm';
  }

  // Các loại hạt
  if (
    lower.includes('hạt') ||
    lower.includes('háº¡t') ||
    lower.includes('loại hạt') ||
    lower.includes('loáº¡i') ||
    lower.includes('cã¡c') ||
    lower.includes('lo?i h?t')
  ) {
    return 'Các loại hạt';
  }

  // Ngũ cốc
  if (
    lower.includes('ngũ cốc') ||
    lower.includes('ngũ') ||
    lower.includes('ngu coc') ||
    lower.includes('ngå©') ||
    lower.includes('cá»') ||
    lower.includes('ng? c?c')
  ) {
    return 'Ngũ cốc';
  }

  // Trái cây
  if (
    lower.includes('trái cây') ||
    lower.includes('trái') ||
    lower.includes('trã¡i') ||
    lower.includes('cã¢y') ||
    lower.includes('hoa quả') ||
    lower.includes('tri cy')
  ) {
    return 'Trái cây';
  }

  // Gia vị chay
  if (
    lower.includes('gia vị') ||
    lower.includes('gia vá') ||
    lower.includes('gia vi') ||
    lower.includes('gia v?')
  ) {
    return 'Gia vị chay';
  }

  // Rau củ
  if (
    lower.includes('rau') ||
    lower.includes('củ') ||
    lower.includes('cá»§') ||
    lower.includes('rau c?')
  ) {
    return 'Rau củ';
  }

  // Khác
  if (lower.includes('khác') || lower.includes('khc')) {
    return 'Khác';
  }

  return trimmed;
}

/**
 * Normalizes a nutrition data source name, repairing mojibake and encoding corruptions.
 */
export function normalizeSourceName(source: string | null | undefined): string {
  if (!source) return 'USDA FoodData Central';
  const trimmed = source.trim().normalize('NFC');
  const lower = trimmed.toLowerCase();

  // NIH/Viện Dinh dưỡng
  if (
    lower.includes('nih') ||
    lower.includes('viện dinh dưỡng') ||
    lower.includes('viá»‡n') ||
    lower.includes('dæ°á»¡ng') ||
    lower.includes('dinh dưỡng') ||
    lower.includes('dinh duong') ||
    lower.includes('vi?n')
  ) {
    return 'NIH/Viện Dinh dưỡng';
  }

  // USDA FoodData Central
  if (lower.includes('usda') || lower.includes('fooddata')) {
    return 'USDA FoodData Central';
  }

  return trimmed;
}
