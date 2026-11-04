// Longlist state (R65, S5): ranked rows with optimistic reordering; all edits go to the 1.3.0 contract endpoints.
// Every successful write calls the page's reload() so the board and the longlist share one source; writes are serialised via `pending`.
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText, type Schemas } from '../shared';

export type App = Schemas['Application'];

/** Field-level messages of a 409 TRANSITION_GUARD_FAILED / 400 body (`errors[].message`), else the problem text. */
export function guardMessages(error: unknown, fallback: string): string[] {
  const errs = (error as { errors?: { message?: string; field?: string; code?: string }[] } | undefined)?.errors;
  const msgs = (errs ?? []).map((e) => e.message ?? `${e.field ?? ''} ${e.code ?? ''}`.trim()).filter(Boolean);
  return msgs.length > 0 ? msgs : [problemText(error, fallback)];
}

export const byRank = (a: App, b: App) => (a.rank ?? Number.MAX_SAFE_INTEGER) - (b.rank ?? Number.MAX_SAFE_INTEGER);

const isConflict = (r: { error?: unknown; response?: { status?: number } }) => r.response?.status === 409 || (r.error as { status?: number } | undefined)?.status === 409;

export function useLonglist(jobId: string, apps: App[], reload: () => void) {
  const { t } = useTranslation();
  const [rows, setRows] = useState<App[]>([]);
  const [error, setError] = useState<string>();
  const [pending, setPending] = useState(false);
  const lock = useRef(false);
  useEffect(() => {
    setRows(apps.filter((a) => a.list === 'LONGLIST').sort(byRank));
  }, [apps]);

  /** One write at a time; on failure reload and tell the user (409 = stale view). */
  const write = async <R extends { error?: unknown; response?: { status?: number } }>(call: () => Promise<R>, fallback: string): Promise<R | undefined> => {
    if (lock.current) return undefined;
    lock.current = true;
    setPending(true);
    try {
      const r = await call();
      if (r.error) {
        setError(isConflict(r) ? t('pipeline.ll.conflict') : problemText(r.error, fallback));
        reload();
      } else {
        setError(undefined);
        reload();
      }
      return r;
    } catch {
      setError(fallback);
      reload();
      return undefined;
    } finally {
      lock.current = false;
      setPending(false);
    }
  };

  /** Move one row up (-1) or down (+1); the new order is sent as a whole (PUT ranking). */
  const move = async (id: string, dir: -1 | 1) => {
    const i = rows.findIndex((r) => r.id === id);
    const j = i + dir;
    if (i < 0 || j < 0 || j >= rows.length) return;
    const next = rows.slice();
    const a = next[i]!;
    next[i] = next[j]!;
    next[j] = a;
    const before = rows;
    setRows(next);
    const r = await write(() => api.PUT('/job-orders/{id}/ranking', { params: { path: { id: jobId } }, body: { list: 'LONGLIST', applicationIds: next.map((x) => x.id) } }), t('pipeline.ll.writeFailed'));
    if (!r || r.error) setRows(before);
  };

  const patch = async (app: App, body: Schemas['ApplicationUpdateRequest']) => {
    const r = await write(() => api.PATCH('/applications/{id}', { params: { path: { id: app.id } }, body: { ...body, version: app.version } }), t('pipeline.ll.writeFailed'));
    return !!r && !r.error;
  };

  /** Longlist to shortlist (first client-facing stage). Resolves with inline messages when the contract refuses. */
  const toShortlist = async (app: App, rating: number | null, rationale: string): Promise<string[] | undefined> => {
    const r = await api.PATCH('/applications/{id}/stage', {
      params: { path: { id: app.id } },
      body: { stage: 'PRESENTED', ...(rating ? { rating } : {}), rationale, version: app.version },
    });
    if (r.error) return guardMessages(r.error, 'shortlist');
    reload();
    return undefined;
  };

  return { rows, error, pending, move, patch, toShortlist };
}
