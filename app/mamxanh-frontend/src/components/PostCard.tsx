import { Link } from 'react-router-dom';
import { Heart, MessageCircle, Clock } from 'lucide-react';
import type { Post } from '../types';
import { Badge } from './ui';

export function PostCard({ post, horizontal = false }: { post: Post; horizontal?: boolean }) {
  return (
    <Link
      to={`/bai-viet/${post.slug}`}
      className={`group flex overflow-hidden rounded-2xl border border-brand-100 bg-white transition-all hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl hover:shadow-brand-900/5 ${
        horizontal ? 'flex-col sm:flex-row' : 'flex-col'
      }`}
    >
      <div className={`relative overflow-hidden ${horizontal ? 'sm:w-56 sm:shrink-0' : ''}`}>
        <img
          src={post.image}
          alt={post.title}
          className={`w-full object-cover transition-transform duration-500 group-hover:scale-105 ${
            horizontal ? 'h-44 sm:h-full' : 'aspect-[16/9]'
          }`}
        />
      </div>
      <div className="flex flex-1 flex-col p-5">
        <div className="mb-2 flex flex-wrap gap-1.5">
          {post.tags.map((t) => (
            <Badge key={t} tone="brand">
              {t}
            </Badge>
          ))}
        </div>
        <h3 className="mb-2 line-clamp-2 font-bold leading-snug text-ink transition-colors group-hover:text-brand-700">
          {post.title}
        </h3>
        <p className="mb-4 line-clamp-2 text-sm text-ink-muted">{post.excerpt}</p>
        <div className="mt-auto flex items-center gap-2 border-t border-brand-50 pt-3 text-xs text-ink-muted">
          <img src={post.author.avatar} alt="" className="h-6 w-6 rounded-full object-cover" />
          <span className="font-medium text-ink-soft">{post.author.name}</span>
          <span className="ml-auto inline-flex items-center gap-1">
            <Clock className="h-3.5 w-3.5" /> {post.readTime}′
          </span>
          <span className="inline-flex items-center gap-1">
            <Heart className="h-3.5 w-3.5" /> {post.likes}
          </span>
          <span className="inline-flex items-center gap-1">
            <MessageCircle className="h-3.5 w-3.5" /> {post.comments}
          </span>
        </div>
      </div>
    </Link>
  );
}
