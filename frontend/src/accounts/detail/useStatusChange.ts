import { useState } from 'react';
import type { MoveRequest } from '../../shell/components/StatusMenu';

export interface BlockingItem {
  id: string;
  title: string;
  type: string;
}
export interface StatusFailure {
  detail: string;
  blocking: BlockingItem[];
}
type Send = (status: string, reason?: string) => Promise<{ error?: unknown }>;

/** Status badge flow: a move either goes straight through or asks for a reason first; server refusals (409) are kept for display. */
export function useStatusChange(send: Send, onChanged: () => void, fallbackError: string) {
  const [pending, setPending] = useState<MoveRequest | null>(null);
  const [failure, setFailure] = useState<StatusFailure | null>(null);

  async function run(to: string, reason?: string) {
    const { error } = await send(to, reason);
    if (error) {
      const e = error as { detail?: string; blockingItems?: BlockingItem[] };
      setFailure({ detail: e.detail ?? fallbackError, blocking: e.blockingItems ?? [] });
      return;
    }
    setFailure(null);
    onChanged();
  }

  return {
    pending,
    failure,
    move: (m: MoveRequest) => {
      setFailure(null);
      if (m.reasonRequired) setPending(m);
      else void run(m.to);
    },
    confirm: (reason: string) => {
      const m = pending;
      setPending(null);
      if (m) void run(m.to, reason);
    },
    cancel: () => setPending(null),
  };
}
