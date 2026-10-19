import { toneOf } from '../../theme/tokens';

/** Badge = coloured dot + text, tone from the theme tokens (never colour alone). */
export function ToneBadge({ tone, label, size = 'md' }: { tone: string; label: string; size?: 'sm' | 'md' | 'lg' }) {
  const t = toneOf(tone).toLowerCase();
  return (
    <span
      className={`tone-badge tone-${t}`}
      data-tone={t}
      style={{ height: `var(--badge-${size}-height)`, padding: `0 var(--badge-${size}-px)`, fontSize: `var(--badge-${size}-font)`, whiteSpace: 'nowrap' }}
    >
      <span aria-hidden style={{ width: 'var(--badge-dot)', height: 'var(--badge-dot)', borderRadius: '50%', background: `var(--tone-${t}-dot)` }} />
      {label}
    </span>
  );
}
