import { useRef, useCallback, type ReactNode, type CSSProperties } from 'react';
import './BorderGlow.css';

export interface BorderGlowProps extends React.HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  className?: string;
  backgroundColor?: string;
  borderRadius?: number;
  orbitSpeed?: number; // seconds for 1 full revolution, default 8s
  style?: CSSProperties;
}

export function BorderGlow({
  children,
  className = '',
  backgroundColor = 'rgba(255, 255, 255, 0.65)',
  borderRadius = 24,
  orbitSpeed = 8,
  style = {},
  ...props
}: BorderGlowProps) {
  const cardRef = useRef<HTMLDivElement>(null);

  const handlePointerMove = useCallback((e: React.PointerEvent<HTMLDivElement>) => {
    const card = cardRef.current;
    if (!card) return;

    const rect = card.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    card.style.setProperty('--mouse-x', `${x}px`);
    card.style.setProperty('--mouse-y', `${y}px`);
    card.style.setProperty('--hover-opacity', '1');
  }, []);

  const handlePointerLeave = useCallback(() => {
    const card = cardRef.current;
    if (!card) return;
    card.style.setProperty('--hover-opacity', '0');
  }, []);

  return (
    <div
      ref={cardRef}
      onPointerMove={handlePointerMove}
      onPointerLeave={handlePointerLeave}
      className={`border-glow-card ${className}`}
      style={
        {
          '--card-bg': backgroundColor,
          '--border-radius': `${borderRadius}px`,
          '--orbit-speed': `${orbitSpeed}s`,
          ...style,
        } as CSSProperties
      }
      {...props}
    >
      <div className="border-track" aria-hidden="true" />
      <div className="border-glow-inner">{children}</div>
    </div>
  );
}

export default BorderGlow;
