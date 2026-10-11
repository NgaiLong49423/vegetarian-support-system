import { useMemo } from 'react';
import { extractYouTubeVideoId } from '../utils/youtube';

interface YouTubeEmbedProps {
  urlOrId: string;
  title?: string;
  className?: string;
}

export function YouTubeEmbed({
  urlOrId,
  title = 'Video hướng dẫn nấu ăn trên YouTube',
  className = '',
}: YouTubeEmbedProps) {
  const videoId = useMemo(() => {
    if (!urlOrId) return null;
    // Nếu truyền thẳng ID (6-15 ký tự word/hyphen)
    if (/^[\w-]{6,15}$/.test(urlOrId.trim())) {
      return urlOrId.trim();
    }
    return extractYouTubeVideoId(urlOrId);
  }, [urlOrId]);

  if (!videoId) {
    return null;
  }

  // Sử dụng youtube-nocookie để tăng cường bảo vệ quyền riêng tư người dùng
  const embedUrl = `https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}?rel=0`;

  return (
    <div
      className={`relative w-full overflow-hidden rounded-2xl border border-brand-100 bg-black shadow-sm aspect-video ${className}`}
      data-testid="youtube-embed-container"
    >
      <iframe
        src={embedUrl}
        title={title}
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
        allowFullScreen
        loading="lazy"
        className="absolute inset-0 h-full w-full border-0"
        data-testid="youtube-embed-iframe"
      />
    </div>
  );
}
