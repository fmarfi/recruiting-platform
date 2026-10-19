// Optimistic stage moves for the board: the card moves at once, a 409 or network error snaps it back
// and exposes the server detail as a toast (UX-04). Undo is offered when the reverse move is legal.
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemCode, problemText, useLabel } from '../shared';
import { canMove, withStage, type Application, type Stage } from './boardRules';

export type Toast = { text: string; kind: 'info' | 'error'; undo?: () => void; refresh?: boolean };

export function useBoardMoves(initial: Application[] | undefined, onChanged: () => void) {
  const { t } = useTranslation();
  const { ref, label } = useLabel();
  const [apps, setApps] = useState<Application[]>([]);
  const appsRef = useRef<Application[]>([]);
  const [toast, setToast] = useState<Toast>();
  const [suggestFilled, setSuggestFilled] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const update = useCallback((next: Application[]) => {
    appsRef.current = next;
    setApps(next);
  }, []);
  useEffect(() => {
    if (initial) update(initial);
  }, [initial, update]);
  useEffect(() => () => clearTimeout(timer.current), []);

  const show = useCallback((tst: Toast) => {
    setToast(tst);
    clearTimeout(timer.current);
    if (tst.kind === 'info') timer.current = setTimeout(() => setToast(undefined), 8000);
  }, []);

  const move = async (app: Application, to: Stage, reason?: string, isUndo = false): Promise<void> => {
    const before = appsRef.current;
    const from = app.stage;
    update(withStage(before, app.id, to, reason));
    let failure: unknown;
    let saved: Application | undefined;
    try {
      const r = await api.PATCH('/applications/{id}/stage', {
        params: { path: { id: app.id } },
        body: { stage: to, ...(reason ? { reason } : {}), version: app.version },
      });
      saved = r.data;
      failure = r.error;
    } catch (e) {
      failure = e ?? new Error('network');
    }
    if (failure || !saved) {
      update(before);
      show({ kind: 'error', text: problemText(failure, t('pipeline.moveFailed')), refresh: problemCode(failure) === 'VERSION_CONFLICT' });
      return;
    }
    const done = saved;
    update(appsRef.current.map((a) => (a.id === done.id ? done : a)));
    const back = canMove(ref, to, from);
    show({
      kind: 'info',
      text: t('pipeline.moved', { name: app.person.name, stage: label('pipelineStage', to) }),
      undo: !isUndo && back && !back.reasonRequired ? () => void move(done, from, undefined, true) : undefined,
    });
    setSuggestFilled(to === 'PLACED');
    onChanged();
  };

  const remove = async (app: Application) => {
    const r = await api.DELETE('/applications/{id}', { params: { path: { id: app.id } } });
    if (r.error) show({ kind: 'error', text: problemText(r.error, t('pipeline.moveFailed')) });
    else update(appsRef.current.filter((a) => a.id !== app.id));
  };

  return { apps, move, remove, toast, setToast, show, suggestFilled, setSuggestFilled };
}
