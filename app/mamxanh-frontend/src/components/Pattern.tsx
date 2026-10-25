import type { ReactNode, CSSProperties } from 'react';
import './Pattern.css';

export interface PatternProps {
  children?: ReactNode;
  className?: string;
  style?: CSSProperties;
}

export function Pattern({ children, className = '', style }: PatternProps) {
  return (
    <div
      className={`pattern-wrapper ${className}`.trim()}
      style={style}
      aria-hidden="true"
    >
      <div className="pattern-container" />
      <div className="pattern-paper">
        <span className="orbit-overlay" />
        <svg className="texture-filter" aria-hidden="true">
          <filter id="void-texture" x="0%" y="0%" width="100%" height="100%">
            <feTurbulence
              seed={5}
              result="noise1"
              numOctaves={8}
              baseFrequency="0.015"
              type="fractalNoise"
            />
            <feTurbulence
              seed={2}
              result="noise2"
              numOctaves={8}
              baseFrequency="0.015"
              type="fractalNoise"
            />
            <feMerge result="noise">
              <feMergeNode in="noise1" />
              <feMergeNode in="noise1" />
              <feMergeNode in="noise2" />
            </feMerge>
            <feGaussianBlur result="blur" stdDeviation={0} in="noise" />
            <feSpecularLighting
              result="specular"
              lightingColor="#ffffff"
              specularExponent={20}
              specularConstant={1}
              surfaceScale={3}
              in="blur"
            >
              <feDistantLight elevation={45} azimuth={90} />
            </feSpecularLighting>
            <feComposite result="lit" operator="over" in2="SourceGraphic" in="specular" />
            <feBlend mode="screen" in2="lit" in="SourceGraphic" />
          </filter>
          <filter id="alt-void-texture" x="0%" y="0%" width="100%" height="100%">
            <feTurbulence
              repeatCount="indefinite"
              dur="6s"
              values="0.015; 0.02; 0.015"
              attributeName="baseFrequency"
              seed={2}
              result="noise1"
              numOctaves={8}
              baseFrequency="0.015"
              type="fractalNoise"
            />
            <feTurbulence
              repeatCount="indefinite"
              dur="6s"
              values="0.015; 0.02; 0.015"
              attributeName="baseFrequency"
              seed={1}
              result="noise2"
              numOctaves={8}
              baseFrequency="0.015"
              type="fractalNoise"
            />
            <feMerge result="noise">
              <feMergeNode in="noise1" />
              <feMergeNode in="noise1" />
              <feMergeNode in="noise2" />
            </feMerge>
            <feGaussianBlur result="blur" stdDeviation={0} in="noise" />
            <feSpecularLighting
              result="specular"
              lightingColor="#ffeeff"
              specularExponent={20}
              specularConstant={1}
              surfaceScale={3}
              in="blur"
            >
              <feDistantLight elevation={45} azimuth={90} />
            </feSpecularLighting>
            <feComposite result="lit" operator="over" in2="SourceGraphic" in="specular" />
            <feBlend mode="screen" in2="lit" in="SourceGraphic" />
          </filter>
        </svg>
      </div>
      {children}
    </div>
  );
}

export default Pattern;
