import { useReference, type LifecycleKind } from '../refdata/useReference';
import { ToneBadge } from './ToneBadge';

/** Status label and tone come from /reference-data. */
export function StatusBadge({ kind, value, size }: { kind: LifecycleKind; value: string; size?: 'sm' | 'md' | 'lg' }) {
  const ref = useReference();
  return <ToneBadge tone={ref.tone(kind, value)} label={ref.label(kind, value)} size={size} />;
}
