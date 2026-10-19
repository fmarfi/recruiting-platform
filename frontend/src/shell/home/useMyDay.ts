import { useState } from 'react';
import { api, type components } from '../../api';
import { useAsync } from '../hooks/useAsync';

export type NextAction = components['schemas']['NextAction'];

/** GET /home plus the two next-action edits (tick done with Undo, snooze). */
export function useMyDay() {
  const home = useAsync(() => api.GET('/home'), []);
  const [undo, setUndo] = useState<NextAction | null>(null);
  const [hidden, setHidden] = useState<Set<string>>(new Set());

  async function setDone(a: NextAction, done: boolean) {
    const { error } = await api.PATCH('/next-actions/{id}', { params: { path: { id: a.id } }, body: { done, version: a.version } });
    if (error) return;
    if (done) {
      setHidden((h) => new Set(h).add(a.id));
      setUndo(a);
    } else {
      setUndo(null);
      setHidden(new Set());
      home.reload();
    }
  }

  async function snooze(a: NextAction, dueOn: string) {
    await api.PATCH('/next-actions/{id}', { params: { path: { id: a.id } }, body: { dueOn, version: a.version } });
    home.reload();
  }

  return { ...home, undo, hidden, setDone, snooze };
}
