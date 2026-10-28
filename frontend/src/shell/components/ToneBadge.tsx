import { toneOf } from '../../theme/tokens';

/** Badge = coloured dot + text, tone from the theme tokens (never colour alone). */
export function ToneBadge({ tone, label, size = 'md' }: { tone: string; label: string; size?: 'sm' | 'md' | 'lg' }) {
  const t = toneOf(tone).toLowerCase();
  return (
    <span
      className={`tone-badge tone-${t}`}
      data-tone={t}
      style={{ minHeight: `var(--badge-${size}-height)`, padding: `2px var(--badge-${size}-px)`, fontSize: `var(--badge-${size}-font)`, whiteSpace: 'normal', maxWidth: '100%' }}
    >
      <span aria-hidden style={{ width: 'var(--badge-dot)', height: 'var(--badge-dot)', borderRadius: '50%', background: `var(--tone-${t}-dot)` }} />
      {label}
    </span>
  );
}
