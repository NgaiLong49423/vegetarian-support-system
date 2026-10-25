import { useState } from 'react';
import { ImageOff } from 'lucide-react';

interface IngredientImageProps {
  imageUrl: string | null;
  name: string;
  containerClassName: string;
  imageClassName?: string;
  loading?: 'eager' | 'lazy';
  placeholderSize?: 'compact' | 'large';
}

export function IngredientImage({ imageUrl, name, containerClassName, imageClassName = 'h-full w-full object-cover', loading = 'lazy', placeholderSize = 'compact' }: IngredientImageProps) {
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null);
  const showImage = Boolean(imageUrl) && failedImageUrl !== imageUrl;

  return <div role="img" aria-label={showImage ? `Ảnh minh họa ${name}` : `Chưa có ảnh minh họa cho ${name}`} className={containerClassName}>
    {showImage ? <img src={imageUrl!} alt="" loading={loading} onError={() => setFailedImageUrl(imageUrl)} className={imageClassName} /> : <span className="flex h-full w-full flex-col items-center justify-center gap-2 px-2 text-center text-ink-muted"><ImageOff aria-hidden="true" className={`${placeholderSize === 'large' ? 'h-8 w-8' : 'h-5 w-5'} text-brand-500`} /><span className={`${placeholderSize === 'large' ? 'text-sm' : 'text-[9px]'} font-medium`}>{placeholderSize === 'large' ? 'Chưa có ảnh minh họa' : 'Chưa có ảnh'}</span></span>}
  </div>;
}
