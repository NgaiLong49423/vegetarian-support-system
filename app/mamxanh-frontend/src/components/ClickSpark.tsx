import {
  useRef,
  useEffect,
  useCallback,
  type FC,
  type ReactNode,
  type MouseEvent as ReactMouseEvent,
  type CSSProperties,
} from 'react';

export interface ClickSparkProps {
  /** Color of each spark line (default: '#ea580c' - brand orange, or '#fff') */
  sparkColor?: string;
  /** Initial length of each spark line in pixels (default: 10) */
  sparkSize?: number;
  /** How far sparks travel from click center in pixels (default: 18) */
  sparkRadius?: number;
  /** Number of spark lines that appear on each click (default: 8) */
  sparkCount?: number;
  /** Animation duration in milliseconds (default: 400) */
  duration?: number;
  /** Easing function used for the spark animation (default: 'ease-out') */
  easing?: 'linear' | 'ease-in' | 'ease-out' | 'ease-in-out' | string;
  /** Additional multiplier for spark distance (default: 1.0) */
  extraScale?: number;
  /** Whether the canvas is fixed to the viewport (recommended for full-page apps) (default: true) */
  fixed?: boolean;
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

interface Spark {
  x: number;
  y: number;
  angle: number;
  startTime: number;
}

export const ClickSpark: FC<ClickSparkProps> = ({
  sparkColor = '#ea580c',
  sparkSize = 10,
  sparkRadius = 18,
  sparkCount = 8,
  duration = 400,
  easing = 'ease-out',
  extraScale = 1.0,
  fixed = true,
  className = '',
  style,
  children,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const sparksRef = useRef<Spark[]>([]);
  const animationIdRef = useRef<number | null>(null);
  const isAnimatingRef = useRef(false);

  // High-DPI canvas resizing
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const parent = canvas.parentElement;
    if (!parent && !fixed) return;

    let resizeTimeout: ReturnType<typeof setTimeout>;

    const resizeCanvas = () => {
      const dpr = window.devicePixelRatio || 1;
      let width = window.innerWidth;
      let height = window.innerHeight;

      if (!fixed && parent) {
        const rect = parent.getBoundingClientRect();
        width = rect.width;
        height = rect.height;
      }

      if (canvas.width !== Math.round(width * dpr) || canvas.height !== Math.round(height * dpr)) {
        canvas.width = Math.round(width * dpr);
        canvas.height = Math.round(height * dpr);
      }
    };

    const handleResize = () => {
      clearTimeout(resizeTimeout);
      resizeTimeout = setTimeout(resizeCanvas, 60);
    };

    window.addEventListener('resize', handleResize);
    resizeCanvas();

    let ro: ResizeObserver | null = null;
    if (!fixed && parent) {
      ro = new ResizeObserver(handleResize);
      ro.observe(parent);
    }

    return () => {
      window.removeEventListener('resize', handleResize);
      ro?.disconnect();
      clearTimeout(resizeTimeout);
      if (animationIdRef.current) {
        cancelAnimationFrame(animationIdRef.current);
      }
    };
  }, [fixed]);

  const easeFunc = useCallback(
    (t: number) => {
      switch (easing) {
        case 'linear':
          return t;
        case 'ease-in':
          return t * t;
        case 'ease-in-out':
          return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
        default:
          return t * (2 - t);
      }
    },
    [easing]
  );

  // Animation frame runner (activates only when sparks are present to save CPU/GPU)
  const runAnimation = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const draw = (timestamp: number) => {
      const dpr = window.devicePixelRatio || 1;
      ctx.resetTransform();
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.scale(dpr, dpr);

      sparksRef.current = sparksRef.current.filter((spark) => {
        const elapsed = timestamp - spark.startTime;
        if (elapsed >= duration) {
          return false;
        }

        const progress = elapsed / duration;
        const eased = easeFunc(progress);

        const distance = eased * sparkRadius * extraScale;
        const lineLength = sparkSize * (1 - eased);

        const x1 = spark.x + distance * Math.cos(spark.angle);
        const y1 = spark.y + distance * Math.sin(spark.angle);
        const x2 = spark.x + (distance + lineLength) * Math.cos(spark.angle);
        const y2 = spark.y + (distance + lineLength) * Math.sin(spark.angle);

        ctx.strokeStyle = sparkColor;
        ctx.lineWidth = 2;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.stroke();

        return true;
      });

      if (sparksRef.current.length > 0) {
        animationIdRef.current = requestAnimationFrame(draw);
      } else {
        isAnimatingRef.current = false;
        ctx.resetTransform();
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }
    };

    if (!isAnimatingRef.current) {
      isAnimatingRef.current = true;
      animationIdRef.current = requestAnimationFrame(draw);
    }
  }, [sparkColor, sparkSize, sparkRadius, duration, easeFunc, extraScale]);

  const handleClick = (e: ReactMouseEvent<HTMLDivElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    let x = e.clientX;
    let y = e.clientY;

    if (!fixed) {
      const rect = canvas.getBoundingClientRect();
      x = e.clientX - rect.left;
      y = e.clientY - rect.top;
    }

    const now = performance.now();
    const newSparks: Spark[] = Array.from({ length: sparkCount }, (_, i) => ({
      x,
      y,
      angle: (2 * Math.PI * i) / sparkCount,
      startTime: now,
    }));

    sparksRef.current.push(...newSparks);
    runAnimation();
  };

  return (
    <div
      className={className}
      style={{
        position: 'relative',
        width: '100%',
        minHeight: '100%',
        ...style,
      }}
      onClick={handleClick}
    >
      <canvas
        ref={canvasRef}
        style={
          fixed
            ? {
                position: 'fixed',
                top: 0,
                left: 0,
                width: '100vw',
                height: '100vh',
                pointerEvents: 'none',
                userSelect: 'none',
                zIndex: 9999,
              }
            : {
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                pointerEvents: 'none',
                userSelect: 'none',
                zIndex: 50,
              }
        }
      />
      {children}
    </div>
  );
};

export default ClickSpark;
