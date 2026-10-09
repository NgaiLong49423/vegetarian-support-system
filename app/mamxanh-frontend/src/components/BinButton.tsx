import { useId, type ButtonHTMLAttributes, type CSSProperties, type ReactNode } from 'react';
import './BinButton.css';

export interface BinButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  size?: number | 'sm' | 'md' | 'lg';
  label?: ReactNode;
  title?: string;
  className?: string;
  style?: CSSProperties;
}

const SIZE_MAP = { sm: 28, md: 38, lg: 55 } as const;

export function BinButton({
  size = 40,
  label,
  title = 'Xóa',
  className = '',
  style,
  type = 'button',
  disabled = false,
  ...props
}: BinButtonProps) {
  const rawId = useId();
  const maskId = `bin-mask-${rawId.replace(/[^a-zA-Z0-9_-]/g, '')}`;

  const pixelSize = typeof size === 'number' ? size : (SIZE_MAP[size] ?? 38);

  const svgs = (
    <>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 39 7"
        className="bin-top"
        aria-hidden="true"
      >
        <line strokeWidth={4} stroke="white" y2={5} x2={39} y1={5} />
        <line strokeWidth={3} stroke="white" y2={1.5} x2={26.0357} y1={1.5} x1={12} />
      </svg>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 33 39"
        className="bin-bottom"
        aria-hidden="true"
      >
        <mask fill="white" id={maskId}>
          <path d="M0 0H33V35C33 37.2091 31.2091 39 29 39H4C1.79086 39 0 37.2091 0 35V0Z" />
        </mask>
        <path
          mask={`url(#${maskId})`}
          fill="white"
          d="M0 0H33H0ZM37 35C37 39.4183 33.4183 43 29 43H4C-0.418278 43 -4 39.4183 -4 35H4H29H37ZM4 43C-0.418278 43 -4 39.4183 -4 35V0H4V35V43ZM37 0V35C37 39.4183 33.4183 43 29 43V35V0H37Z"
        />
        <line strokeWidth={4} stroke="white" x1={12} y1={6} x2={12} y2={29} />
        <line strokeWidth={4} stroke="white" x1={21} y1={6} x2={21} y2={29} />
      </svg>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 89 80"
        className="garbage"
        aria-hidden="true"
      >
        <path
          fill="white"
          d="M20.5 10.5L37.5 15.5L42.5 11.5L51.5 12.5L68.75 0L72 11.5L79.5 12.5H88.5L87 22L68.75 31.5L75.5066 25L86 26L87 35.5L77.5 48L70.5 49.5L80 50L77.5 71.5L63.5 58.5L53.5 68.5L65.5 70.5L45.5 73L35.5 79.5L28 67L16 63L12 51.5L0 48L16 25L22.5 17L20.5 10.5Z"
        />
      </svg>
    </>
  );

  const customStyle = {
    '--bin-size': `${pixelSize}px`,
    ...style,
  } as CSSProperties;

  return (
    <button
      type={type}
      disabled={disabled}
      title={title}
      aria-label={props['aria-label'] || (typeof label === 'string' ? label : title)}
      className={`bin-button ${label ? 'bin-button--with-label' : ''} ${className}`}
      style={customStyle}
      {...props}
    >
      {label ? (
        <>
          <div className="bin-button__icon-wrap">{svgs}</div>
          <span className="bin-button__text">{label}</span>
        </>
      ) : (
        svgs
      )}
    </button>
  );
}

export default BinButton;
