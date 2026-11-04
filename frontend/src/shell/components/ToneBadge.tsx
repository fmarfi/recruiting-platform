import type { ReactNode } from 'react';
import { toneOf } from '../../theme/tokens';

/**
 * Visual system v3 badge: soft tone background, tone text, 12/500, height 20, radius 4, no dot. `size` is accepted for
 * old callers and ignored. Pass `icon` (a 14 px Tabler icon) to show icon plus words for consent and off-limits problems.
 */
export function ToneBadge({ tone, label, icon }: { tone: string; label: string; size?: 'sm' | 'md' | 'lg'; icon?: ReactNode }) {
  const t = toneOf(tone).toLowerCase();
  return (
    <span
      className={`tone-badge tone-${t}`}
      data-tone={t}
      style={{ minHeight: 20, padding: '0 6px', fontSize: '0.75rem', fontWeight: 500, whiteSpace: 'normal', maxWidth: '100%', gap: 4 }}
    >
      {icon && (
        <span aria-hidden style={{ display: 'inline-flex', flex: 'none' }}>
          {icon}
        </span>
      )}
      {label}
    </span>
  );
}
