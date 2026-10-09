import { useId } from 'react';

interface StarRatingProps {
  value: number | null;
  onChange?: (score: number) => void;
  disabled?: boolean;
  name?: string;
}

export function StarRating({ value, onChange, disabled = false, name: customName }: StarRatingProps) {
  const generatedId = useId();
  const groupName = customName || `star-rating-${generatedId}`;

  // Đặt theo thứ tự 5 -> 1 trong DOM để flex-direction: row-reverse hiển thị từ 1 -> 5
  const scores = [5, 4, 3, 2, 1];

  return (
    <div className="animated-rating" role="radiogroup" aria-label="Đánh giá sao">
      {scores.map((score) => {
        const inputId = `${groupName}-star-${score}`;
        const isChecked = value === score;

        return (
          <span key={score} className="inline-flex">
            <input
              type="radio"
              id={inputId}
              name={groupName}
              value={score}
              checked={isChecked}
              disabled={disabled}
              onChange={() => {
                if (!disabled && onChange) {
                  onChange(score);
                }
              }}
              aria-label={`${score} sao`}
            />
            <label htmlFor={inputId} title={`${score} sao`}>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                <path
                  pathLength="360"
                  d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"
                />
              </svg>
            </label>
          </span>
        );
      })}
    </div>
  );
}
