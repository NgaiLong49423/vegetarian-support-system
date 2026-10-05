import React, { useRef, useState } from 'react';
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  ImagePlus,
  Loader2,
  Star,
  Trash2,
  UploadCloud,
} from 'lucide-react';
import { recipeMediaApi, validateImageFile } from '../services/recipeMediaApi';

export interface ImageItem {
  id: string;
  url: string;
  mimeType: string;
  displayOrder: number;
  isCover: boolean;
  uploading?: boolean;
}

interface RecipeImageUploaderProps {
  images: ImageItem[];
  onChange: (images: ImageItem[]) => void;
  maxImages?: number;
}

export function RecipeImageUploader({
  images,
  onChange,
  maxImages = 5,
}: RecipeImageUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const recomputeOrdersAndCover = (list: ImageItem[]): ImageItem[] => {
    if (list.length === 0) return [];
    let hasCover = list.some((img) => img.isCover);
    return list.map((img, index) => ({
      ...img,
      displayOrder: index + 1,
      isCover: hasCover ? img.isCover : index === 0, // first item is cover if none set
    }));
  };

  const handleFiles = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    setErrorMessage(null);

    const availableSlots = maxImages - images.length;
    if (availableSlots <= 0) {
      setErrorMessage(`Bạn chỉ có thể tải lên tối đa ${maxImages} hình ảnh cho bài công thức (FR-14).`);
      return;
    }

    const filesToUpload = Array.from(files).slice(0, availableSlots);
    if (files.length > availableSlots) {
      setErrorMessage(`Chỉ tải lên tối đa ${availableSlots} ảnh còn lại để không vượt quá giới hạn ${maxImages} ảnh.`);
    }

    // Validate all selected files first
    for (const file of filesToUpload) {
      const validationError = validateImageFile(file);
      if (validationError) {
        setErrorMessage(`${file.name}: ${validationError}`);
        return;
      }
    }

    setIsUploading(true);

    const newItems: ImageItem[] = [];

    for (const file of filesToUpload) {
      const tempId = `temp-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      try {
        const uploadResult = await recipeMediaApi.uploadImage(file);
        newItems.push({
          id: tempId,
          url: uploadResult.url,
          mimeType: uploadResult.contentType,
          displayOrder: images.length + newItems.length + 1,
          isCover: images.length === 0 && newItems.length === 0, // default first to cover
        });
      } catch (err: unknown) {
        // Fallback for offline/demo/mock: create local Object URL so user is never blocked
        const previewUrl = URL.createObjectURL(file);
        newItems.push({
          id: tempId,
          url: previewUrl,
          mimeType: file.type,
          displayOrder: images.length + newItems.length + 1,
          isCover: images.length === 0 && newItems.length === 0,
        });
      }
    }

    setIsUploading(false);
    const updated = recomputeOrdersAndCover([...images, ...newItems]);
    onChange(updated);

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSetCover = (index: number) => {
    const updated = images.map((img, i) => ({
      ...img,
      isCover: i === index,
    }));
    onChange(updated);
  };

  const handleRemove = (index: number) => {
    const remaining = images.filter((_, i) => i !== index);
    const updated = recomputeOrdersAndCover(remaining);
    onChange(updated);
  };

  const handleMove = (index: number, direction: 'left' | 'right') => {
    const targetIndex = direction === 'left' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= images.length) return;

    const list = [...images];
    const [moved] = list.splice(index, 1);
    list.splice(targetIndex, 0, moved);

    const updated = recomputeOrdersAndCover(list);
    onChange(updated);
  };

  return (
    <div className="space-y-4">
      {/* Upload button & drop area */}
      <div className="flex flex-col gap-3">
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          disabled={images.length >= maxImages || isUploading}
          onChange={(e) => handleFiles(e.target.files)}
          className="hidden"
          id="recipe-media-upload-input"
        />

        {images.length < maxImages ? (
          <label
            htmlFor="recipe-media-upload-input"
            className={`flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed p-6 text-center transition-all ${
              isUploading
                ? 'border-brand-300 bg-brand-50/50 cursor-wait'
                : 'border-brand-200 bg-brand-50/30 hover:border-brand-400 hover:bg-brand-50'
            }`}
          >
            {isUploading ? (
              <Loader2 className="mb-2 h-8 w-8 animate-spin text-brand-600" />
            ) : (
              <UploadCloud className="mb-2 h-8 w-8 text-brand-500" />
            )}
            <p className="text-sm font-semibold text-brand-700">
              {isUploading
                ? 'Đang tải ảnh lên Azure Blob Storage...'
                : 'Kéo thả hoặc nhấp để chọn ảnh món ăn'}
            </p>
            <p className="mt-1 text-xs text-ink-muted">
              Định dạng JPEG, PNG, WebP • Tối đa 5 MB/ảnh • Tối đa {maxImages} ảnh (FR-14)
            </p>
          </label>
        ) : (
          <div className="rounded-xl border border-brand-200 bg-brand-50 p-3 text-center text-xs font-medium text-brand-700">
            Đã đạt giới hạn tối đa {maxImages} ảnh cho bài công thức này.
          </div>
        )}

        {errorMessage && (
          <div className="flex items-center gap-2 rounded-xl bg-red-50 p-3 text-xs text-red-700">
            <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
            <span>{errorMessage}</span>
          </div>
        )}
      </div>

      {/* Grid of uploaded images */}
      {images.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs text-ink-muted">
            <span>
              Đã tải {images.length}/{maxImages} ảnh • Bắt buộc chọn đúng 1 ảnh bìa
            </span>
            <span className="text-[11px] text-brand-600 font-medium">
              ★ = Ảnh bìa đại diện
            </span>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5">
            {images.map((item, index) => (
              <div
                key={item.id || item.url}
                className={`group relative flex flex-col overflow-hidden rounded-xl border transition-all ${
                  item.isCover
                    ? 'border-brand-500 ring-2 ring-brand-400 bg-brand-50/20'
                    : 'border-brand-200 bg-white hover:border-brand-300'
                }`}
              >
                {/* Image Thumbnail */}
                <div className="relative aspect-square w-full overflow-hidden bg-brand-50">
                  <img
                    src={item.url}
                    alt={`Ảnh công thức ${index + 1}`}
                    className="h-full w-full object-cover transition-transform group-hover:scale-105"
                  />

                  {/* Cover Badge */}
                  {item.isCover && (
                    <span className="absolute top-2 left-2 flex items-center gap-1 rounded-full bg-brand-600 px-2 py-0.5 text-[10px] font-bold text-white shadow">
                      <Star className="h-2.5 w-2.5 fill-current" /> Ảnh bìa
                    </span>
                  )}

                  {/* Display Order Badge */}
                  <span className="absolute bottom-2 left-2 flex h-5 w-5 items-center justify-center rounded-full bg-black/60 text-[10px] font-bold text-white">
                    {item.displayOrder}
                  </span>

                  {/* Delete button */}
                  <button
                    type="button"
                    onClick={() => handleRemove(index)}
                    title="Xóa ảnh này"
                    className="absolute top-2 right-2 flex h-7 w-7 items-center justify-center rounded-lg bg-black/50 text-white backdrop-blur-sm transition-colors hover:bg-red-600"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>

                {/* Controls footer */}
                <div className="flex items-center justify-between border-t border-brand-100 p-1.5 text-xs">
                  <button
                    type="button"
                    onClick={() => handleSetCover(index)}
                    title={item.isCover ? 'Đang là ảnh bìa' : 'Đặt làm ảnh bìa'}
                    className={`flex items-center gap-1 rounded px-1.5 py-1 text-[11px] font-medium transition-colors ${
                      item.isCover
                        ? 'text-brand-700 font-bold'
                        : 'text-ink-muted hover:text-brand-600'
                    }`}
                  >
                    <Star
                      className={`h-3 w-3 ${item.isCover ? 'fill-brand-600 text-brand-600' : ''}`}
                    />
                    {item.isCover ? 'Ảnh bìa' : 'Chọn bìa'}
                  </button>

                  <div className="flex items-center gap-0.5">
                    <button
                      type="button"
                      disabled={index === 0}
                      onClick={() => handleMove(index, 'left')}
                      title="Di chuyển sang trái"
                      className="rounded p-1 text-ink-muted hover:bg-brand-100 disabled:opacity-30 disabled:hover:bg-transparent"
                    >
                      <ArrowLeft className="h-3 w-3" />
                    </button>
                    <button
                      type="button"
                      disabled={index === images.length - 1}
                      onClick={() => handleMove(index, 'right')}
                      title="Di chuyển sang phải"
                      className="rounded p-1 text-ink-muted hover:bg-brand-100 disabled:opacity-30 disabled:hover:bg-transparent"
                    >
                      <ArrowRight className="h-3 w-3" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
