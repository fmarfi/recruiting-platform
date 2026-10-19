import { toneOf } from '../../theme/tokens';
import { useLabel } from './useLabel';
import type { LifecycleKey } from './types';

/** Badge coloured by the theme tone tokens; label and tone come from /reference-data. */
export function StatusBadge({ lifecycle, value, testId }: { lifecycle: LifecycleKey; value: string; testId?: string }) {
  const { def, label } = useLabel();
  const tone = toneOf(def(lifecycle, value)?.tone).toLowerCase();
  return (
    <span
      className={`tone-badge tone-${tone}`}
      data-testid={testId}
      data-tone={tone}
      style={{
        height: 'var(--badge-md-height, 24px)',
        padding: '0 var(--badge-md-px, 10px)',
        fontSize: 'var(--badge-md-font, 12px)',
        whiteSpace: 'nowrap',
      }}
    >
      <span
        aria-hidden
        style={{ width: 'var(--badge-dot)', height: 'var(--badge-dot)', borderRadius: '50%', background: `var(--tone-${tone}-dot)` }}
      />
      {label(lifecycle, value)}
    </span>
  );
}

