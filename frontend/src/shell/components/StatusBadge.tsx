import type { ReactNode } from 'react';
import { useReference, type LifecycleKind } from '../refdata/useReference';
import { ToneBadge } from './ToneBadge';

/** Status label and tone come from /reference-data. */
export function StatusBadge({ kind, value, size, icon }: { kind: LifecycleKind; value: string; size?: 'sm' | 'md' | 'lg'; icon?: ReactNode }) {
  const ref = useReference();
  return <ToneBadge tone={ref.tone(kind, value)} label={ref.label(kind, value)} size={size} icon={icon} />;
}
