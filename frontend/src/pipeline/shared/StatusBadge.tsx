import { toneOf } from '../../theme/tokens';
import { useLabel } from './useLabel';
import type { LifecycleKey } from './types';

/** Status badge in the v3 style (section 6): tone soft background, 12/500, height 20, radius 4, no dot. */
export function StatusBadge({ lifecycle, value, testId }: { lifecycle: LifecycleKey; value: string; testId?: string }) {
  const { def, label } = useLabel();
  const tone = toneOf(def(lifecycle, value)?.tone).toLowerCase();
  return (
    <span
      className={`tone-badge tone-${tone}`}
      data-testid={testId}
      data-tone={tone}
      style={{ minHeight: '1.25rem', padding: '0 0.375rem', fontSize: '0.75rem', fontWeight: 500, whiteSpace: 'nowrap' }}
    >
      {label(lifecycle, value)}
    </span>
  );
}
