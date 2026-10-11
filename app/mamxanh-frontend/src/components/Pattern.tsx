import { useEffect, useRef, type ReactNode, type CSSProperties } from 'react';
import './Pattern.css';

export interface PatternProps {
  children?: ReactNode;
  className?: string;
  style?: CSSProperties;
}

export function Pattern({ children, className = '', style }: PatternProps) {
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Pause animations when the background is not visible (no React re-render needed).
  useEffect(() => {
    const el = wrapperRef.current;
    if (!el || typeof IntersectionObserver === 'undefined') return;
    const io = new IntersectionObserver(([entry]) => {
      el.classList.toggle('is-paused', !entry.isIntersecting);
    });
    io.observe(el);
    return () => io.disconnect();
  }, []);

  return (
    <div ref={wrapperRef} className={`pattern-wrapper ${className}`.trim()} style={style}>
      <span className="pattern-blob pattern-blob--1" aria-hidden="true" />
      <span className="pattern-blob pattern-blob--2" aria-hidden="true" />
      <span className="pattern-blob pattern-blob--3" aria-hidden="true" />
      <span className="pattern-blob pattern-blob--4" aria-hidden="true" />
      <span className="pattern-blob pattern-blob--5" aria-hidden="true" />
      {children}
    </div>
  );
}

export default Pattern;
