import { QuickLog } from '../../shell/quicklog';
import type { DialogProps } from './dialogProps';

/** Kept for existing callers (account page); the composer itself lives in shell/quicklog. */
export function LogActivityDialog({ opened, onClose, accountId, onDone }: DialogProps) {
  return <QuickLog opened={opened} onClose={onClose} accountId={accountId} onDone={onDone} />;
}
