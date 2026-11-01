import Markdown, { defaultUrlTransform } from 'react-markdown';
import remarkBreaks from 'remark-breaks';

const elements = ['p', 'br', 'strong', 'em', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'ul', 'ol', 'li', 'a', 'blockquote', 'pre', 'code', 'hr'];

function safeUrl(value: string): string | undefined {
  const url = defaultUrlTransform(value);
  if (!url || url.startsWith('//') || url.includes('\\')) return undefined;
  try {
    return ['http:', 'https:', 'mailto:'].includes(new URL(url, 'https://mamxanh.invalid').protocol)
      ? url : undefined;
  } catch { return undefined; }
}

/** Render only assistant prose. Interactive Backend results stay outside this boundary. */
export function AiMessageContent({ content }: { content: string }) {
  return (
    <div className="ai-message-markdown min-w-0 [overflow-wrap:anywhere]">
      <Markdown
        skipHtml
        allowedElements={elements}
        remarkPlugins={[remarkBreaks]}
        urlTransform={safeUrl}
        components={{
          a: ({ href, children }) => href
            ? <a href={href} target={/^https?:/i.test(href) ? '_blank' : undefined}
                rel="noopener noreferrer" className="rounded-sm text-brand-700 underline underline-offset-2 focus-visible:outline-2 focus-visible:outline-offset-2">{children}</a>
            : <span>{children}</span>,
        }}
      >{content}</Markdown>
    </div>
  );
}
