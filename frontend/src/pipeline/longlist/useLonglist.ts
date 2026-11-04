// Longlist state (R65, S5): ranked rows with optimistic reordering; all edits go to the 1.3.0 contract endpoints.
import { useEffect, useState } from 'react';
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

export function useLonglist(jobId: string, apps: App[], reload: () => void) {
  const [rows, setRows] = useState<App[]>([]);
  const [error, setError] = useState<string>();
  useEffect(() => {
    setRows(apps.filter((a) => a.list === 'LONGLIST').sort(byRank));
  }, [apps]);

  /** Move one row up (-1) or down (+1); the new order is sent as a whole (PUT ranking). */
  const move = async (id: string, dir: -1 | 1) => {
    const i = rows.findIndex((r) => r.id === id);
    const j = i + dir;
    if (i < 0 || j < 0 || j >= rows.length) return;
    const next = rows.slice();
    const a = next[i]!;
    next[i] = next[j]!;
    next[j] = a;
    setRows(next);
    const r = await api.PUT('/job-orders/{id}/ranking', { params: { path: { id: jobId } }, body: { list: 'LONGLIST', applicationIds: next.map((a) => a.id) } });
    if (r.error) {
      setError(problemText(r.error, 'ranking'));
      reload();
    } else {
      setError(undefined);
      setRows(r.data ?? next);
    }
  };

  const patch = async (app: App, body: Schemas['ApplicationUpdateRequest']) => {
    const r = await api.PATCH('/applications/{id}', { params: { path: { id: app.id } }, body: { ...body, version: app.version } });
    if (r.error) {
      setError(problemText(r.error, 'update'));
      reload();
      return false;
    }
    setError(undefined);
    if (r.data) setRows((rs) => rs.map((x) => (x.id === app.id ? r.data! : x)));
    return true;
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

  return { rows, error, move, patch, toShortlist };
}
