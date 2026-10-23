export type CookingDifficultyCode = 'EASY' | 'MEDIUM' | 'HARD';

export interface DifficultyAssessment {
  code: CookingDifficultyCode;
  label: string;
  badgeTone: 'leaf' | 'brand' | 'amber';
  reasons: string[];
  summary: string;
}

/**
 * Đánh giá độ khó của món ăn dựa trên các yếu tố khách quan:
 * 1. Số lượng nguyên liệu: < 6 (Dễ), 6-9 (Trung bình), >= 10 (Khó - nhiều nguyên liệu cần sơ chế).
 * 2. Tổng thời gian chế biến (chuẩn bị + nấu): <= 25 phút (Dễ), 26-50 phút (Trung bình), > 50 phút (Khó).
 * 3. Độ phức tạp các bước thực hiện: số bước hướng dẫn.
 */
export function assessRecipeDifficulty(params: {
  ingredientCount: number;
  prepTimeMinutes?: number;
  cookTimeMinutes?: number;
  instructions?: string;
}): DifficultyAssessment {
  const { ingredientCount, prepTimeMinutes = 0, cookTimeMinutes = 0, instructions = '' } = params;
  const totalMinutes = Math.max(0, prepTimeMinutes) + Math.max(0, cookTimeMinutes);

  const stepCount = instructions
    ? instructions
        .split('\n')
        .map((s) => s.trim())
        .filter((s) => s.length > 0 && !/^(\d+\.|\-|\*)\s*$/.test(s)).length
    : 0;

  const reasons: string[] = [];
  let score = 0; // 0-1: Dễ, 2-3: Trung bình, >= 4: Khó

  // Yếu tố 1: Số lượng nguyên liệu (nguyên liệu nhiều làm tăng độ khó chuẩn bị)
  if (ingredientCount >= 10) {
    score += 3;
    reasons.push(`${ingredientCount} nguyên liệu (nhiều thành phần cần chuẩn bị)`);
  } else if (ingredientCount >= 6) {
    score += 1;
    reasons.push(`${ingredientCount} nguyên liệu`);
  } else if (ingredientCount > 0) {
    reasons.push(`${ingredientCount} nguyên liệu gọn nhẹ`);
  }

  // Yếu tố 2: Tổng thời gian chế biến
  if (totalMinutes > 50) {
    score += 2;
    reasons.push(`thời gian nấu ${totalMinutes} phút`);
  } else if (totalMinutes > 25) {
    score += 1;
    reasons.push(`thời gian ${totalMinutes} phút`);
  } else if (totalMinutes > 0) {
    reasons.push(`nấu nhanh ${totalMinutes} phút`);
  }

  // Yếu tố 3: Độ dài và số bước hướng dẫn
  if (stepCount >= 7) {
    score += 1;
    reasons.push(`${stepCount} bước thực hiện`);
  }

  if (score >= 3) {
    return {
      code: 'HARD',
      label: 'Khó',
      badgeTone: 'amber',
      reasons,
      summary: reasons.length > 0 ? reasons.join(' · ') : 'Nhiều nguyên liệu hoặc thời gian chế biến dài',
    };
  }

  if (score >= 1) {
    return {
      code: 'MEDIUM',
      label: 'Trung bình',
      badgeTone: 'brand',
      reasons,
      summary: reasons.length > 0 ? reasons.join(' · ') : 'Thời gian và số lượng nguyên liệu vừa phải',
    };
  }

  return {
    code: 'EASY',
    label: 'Dễ',
    badgeTone: 'leaf',
    reasons,
    summary: reasons.length > 0 ? reasons.join(' · ') : 'Ít nguyên liệu, thao tác đơn giản và nhanh gọn',
  };
}
