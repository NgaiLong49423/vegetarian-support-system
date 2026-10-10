import { useState } from 'react';

const SIZES = {
  sm: 'h-9 w-9 text-sm',
  md: 'h-12 w-12 text-base',
  lg: 'h-24 w-24 text-3xl',
} as const;

/**
 * Member avatar (FR-23). Without an image, or when the image cannot load, it shows the app's
 * default avatar: the first letter of the name on the brand colour, as in the header (AC-23.3).
 */
export function MemberAvatar({ name, url, size = 'md' }: { name: string; url: string | null | undefined; size?: keyof typeof SIZES }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const classes = `${SIZES[size]} shrink-0 rounded-full ring-2 ring-brand-200`;
  if (url && failedUrl !== url) {
    return <img src={url} alt={`Ảnh đại diện của ${name}`} onError={() => setFailedUrl(url)} className={`${classes} object-cover`} />;
  }
  return (
    <span role="img" aria-label={`Ảnh đại diện mặc định của ${name}`} className={`${classes} flex items-center justify-center bg-brand-600 font-bold text-white`}>
      {name.trim().charAt(0).toUpperCase() || '?'}
    </span>
  );
}
