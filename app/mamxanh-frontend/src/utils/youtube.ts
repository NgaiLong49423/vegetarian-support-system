/**
 * Tiện ích kiểm tra và trích xuất YouTube Video ID theo FR-15 / BR-10.
 * Hỗ trợ các định dạng:
 * - https://youtu.be/{id}
 * - https://www.youtube.com/watch?v={id}
 * - https://youtube.com/watch?v={id}
 * - https://www.youtube.com/embed/{id}
 * - https://www.youtube.com/shorts/{id}
 */

export function extractYouTubeVideoId(rawUrl: string): string | null {
  if (!rawUrl || !rawUrl.trim()) return null;
  const trimmed = rawUrl.trim();

  try {
    const parsed = new URL(trimmed);
    const hostname = parsed.hostname.toLowerCase();

    // Chỉ chấp nhận HTTP/HTTPS
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return null;
    }

    // Định dạng rút gọn: https://youtu.be/{id}
    if (hostname === 'youtu.be') {
      const id = parsed.pathname.slice(1).split('/')[0].split('?')[0];
      return id && /^[\w-]{6,15}$/.test(id) ? id : null;
    }

    // Định dạng đầy đủ: youtube.com, www.youtube.com, m.youtube.com
    if (
      hostname === 'youtube.com' ||
      hostname === 'www.youtube.com' ||
      hostname === 'm.youtube.com'
    ) {
      if (parsed.pathname === '/watch') {
        const id = parsed.searchParams.get('v');
        return id && /^[\w-]{6,15}$/.test(id) ? id : null;
      }
      if (parsed.pathname.startsWith('/embed/')) {
        const id = parsed.pathname.slice(7).split('/')[0].split('?')[0];
        return id && /^[\w-]{6,15}$/.test(id) ? id : null;
      }
      if (parsed.pathname.startsWith('/shorts/')) {
        const id = parsed.pathname.slice(8).split('/')[0].split('?')[0];
        return id && /^[\w-]{6,15}$/.test(id) ? id : null;
      }
    }

    return null;
  } catch {
    return null;
  }
}

export interface YouTubeValidationResult {
  valid: boolean;
  videoId: string | null;
  error?: string;
}

export function validateYouTubeUrl(rawUrl: string): YouTubeValidationResult {
  const trimmed = rawUrl.trim();
  // Tùy chọn (AC-15.5): để trống là hợp lệ
  if (!trimmed) {
    return { valid: true, videoId: null };
  }

  try {
    const parsed = new URL(trimmed);
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return {
        valid: false,
        videoId: null,
        error: 'Vui lòng nhập đường dẫn URL hợp lệ bắt đầu bằng https://',
      };
    }
    const hostname = parsed.hostname.toLowerCase();
    const isYouTube =
      hostname === 'youtu.be' ||
      hostname === 'youtube.com' ||
      hostname === 'www.youtube.com' ||
      hostname === 'm.youtube.com';

    // AC-15.2: Từ chối liên kết ngoài YouTube
    if (!isYouTube) {
      return {
        valid: false,
        videoId: null,
        error: 'Hệ thống chỉ hỗ trợ video từ YouTube. Vui lòng không sử dụng nền tảng khác.',
      };
    }

    const videoId = extractYouTubeVideoId(trimmed);
    // AC-15.1: Kiểm tra tính hợp lệ của Video ID
    if (!videoId) {
      return {
        valid: false,
        videoId: null,
        error: 'Đường dẫn YouTube không hợp lệ hoặc thiếu mã video. Ví dụ hợp lệ: https://youtu.be/dQw4w9WgXcQ',
      };
    }

    return { valid: true, videoId };
  } catch {
    return {
      valid: false,
      videoId: null,
      error: 'Vui lòng nhập đường dẫn URL hợp lệ bắt đầu bằng https://',
    };
  }
}
