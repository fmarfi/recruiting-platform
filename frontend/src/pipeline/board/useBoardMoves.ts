// Optimistic stage moves for the board: the card moves at once, a 409 or network error snaps it back
// and exposes the server detail as a toast (UX-04). Undo is offered when the reverse move is legal.
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemCode, problemText, useLabel } from '../shared';
import { classifyStageError, overrideBody, warningLines, type Guard } from './stageGuard';
import { canMove, withStage, type Application, type Stage } from './boardRules';

export type Toast = { text: string; kind: 'info' | 'error'; undo?: () => void; refresh?: boolean; warnings?: ReturnType<typeof warningLines> };
/** A stage move the server refused with CONSENT_REQUIRED or OFF_LIMITS; the dialog decides what to do next. */
export type PendingGuard = Guard & { app: Application; to: Stage; reason?: string };

export function useBoardMoves(initial: Application[] | undefined, onChanged: () => void) {
  const { t } = useTranslation();
  const { ref, label } = useLabel();
  const [apps, setApps] = useState<Application[]>([]);
  const appsRef = useRef<Application[]>([]);
  const [toast, setToast] = useState<Toast>();
  const [suggestFilled, setSuggestFilled] = useState(false);
  const [guard, setGuard] = useState<PendingGuard>();
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
    if (tst.kind === 'info') timer.current = setTimeout(() => setToast(undefined), tst.warnings?.length ? 15000 : 8000);
  }, []);

  const move = async (app: Application, to: Stage, reason?: string, isUndo = false, offLimitsOverrideReason?: string): Promise<void> => {
    const before = appsRef.current;
    const from = app.stage;
    update(withStage(before, app.id, to, reason));
    let failure: unknown;
    let saved: Application | undefined;
    try {
      const r = await api.PATCH('/applications/{id}/stage', {
        params: { path: { id: app.id } },
        body: { stage: to, ...(reason ? { reason } : {}), version: app.version, ...(offLimitsOverrideReason ? overrideBody(offLimitsOverrideReason) : {}) },
      });
      saved = r.data;
      failure = r.error;
    } catch (e) {
      failure = e ?? new Error('network');
    }
    if (failure || !saved) {
      update(before);
      const g = classifyStageError(failure);
      if (g) {
        setToast(undefined);
        setGuard({ ...g, app, to, reason });
        return;
      }
      show({ kind: 'error', text: problemText(failure, t('pipeline.moveFailed')), refresh: problemCode(failure) === 'VERSION_CONFLICT' });
      return;
    }
    const done = saved;
    setGuard(undefined);
    update(appsRef.current.map((a) => (a.id === done.id ? done : a)));
    const back = canMove(ref, to, from);
    show({
      kind: 'info',
      text: t('pipeline.moved', { name: app.person.name, stage: label('pipelineStage', to) }),
      warnings: warningLines(done.warnings),
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

  return { guard, clearGuard: () => setGuard(undefined), apps, move, remove, toast, setToast, show, suggestFilled, setSuggestFilled };
}
