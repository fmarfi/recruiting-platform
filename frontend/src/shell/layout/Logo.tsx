import type { CSSProperties } from 'react';

/** Mihenk monogram (docs/product/brand/logo.svg): midnight touchstone, white M, brass streak. */
function Mark({ size, outline }: { size: number; outline?: boolean }) {
  return (
    <svg width={size} height={size} viewBox="4 8 80 80" aria-hidden focusable="false" style={{ flex: 'none' }}>
      <rect x="4.5" y="8.5" width="79" height="79" rx="14" fill="#14233D" stroke={outline ? '#B9C3D6' : 'none'} strokeWidth={outline ? 1.5 : 0} />
      <polyline points="24,63 24,29 44,51 64,29 64,63" fill="none" stroke="#FFFFFF" strokeWidth="6" strokeLinejoin="miter" strokeLinecap="square" />
      <line x1="25" y1="76" x2="63" y2="72" stroke="#C49A4E" strokeWidth="4" strokeLinecap="round" />
    </svg>
  );
}

/**
 * The Mihenk logo. `compact` shows the monogram only (collapsed rail, phone); `onDark` is the header variant
 * (outlined monogram, white wordmark). The accessible name is always "Mihenk".
 */
export function Logo({ compact = false, onDark = false, size = 32, style }: { compact?: boolean; onDark?: boolean; size?: number; style?: CSSProperties }) {
  return (
    <span role="img" aria-label="Mihenk" style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--space-2)', ...style }}>
      <Mark size={size} outline={onDark} />
      {!compact && (
        <span
          aria-hidden
          style={{
            fontFamily: 'var(--font-display)',
            fontWeight: 600,
            fontSize: size * 0.72,
            letterSpacing: '0.005em',
            lineHeight: 1,
            color: onDark ? 'var(--color-chromeInk)' : 'var(--color-accent)',
          }}
        >
          Mihenk
        </span>
      )}
    </span>
  );
}
