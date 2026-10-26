import { apiViewStore, type ApiViewStoreOptions } from './apiViewStore';
import type { ViewStore } from './types';
import { localViewStore } from './viewStore';
import { viewErrorCode } from './viewErrors';

/** Things the screen should tell the user about the store. */
export type ViewNotice =
  | { type: 'fallback' } // API unavailable: views are local to this browser for now
  | { type: 'migrated'; moved: number; left: number } // local views were copied to the server once
  | { type: 'persist' }; // local storage is blocked or full

export interface ViewStoreOptions<F> extends ApiViewStoreOptions<F> {
  /** Grid id: the localStorage key of the old local views and the migration flag. */
  gridId: string;
  onNotice?: (n: ViewNotice) => void;
}

const flagKey = (gridId: string) => `grid.${gridId}.views.migrated`;
const localKey = (gridId: string) => `grid.${gridId}.views`;

function safe<T>(run: () => T, fallback: T): T {
  try {
    return run();
  } catch {
    return fallback;
  }
}

/**
 * The store screens should use: server-backed (/views) with localViewStore as the fallback when the API is
 * unavailable (network error, 5xx). The first successful list copies local views of this grid to the server
 * once (private, duplicates skipped) and reports it through `onNotice`. Mode is decided by `list()`; writes in
 * API mode surface their error (DUPLICATE, LIMIT, CONFLICT...) to the caller.
 */
export function createViewStore<F = unknown>(opts: ViewStoreOptions<F>): ViewStore<F> {
  const local = localViewStore<F>(opts.gridId, () => opts.onNotice?.({ type: 'persist' }));
  const remote = apiViewStore<F>(opts);
  let mode: 'unknown' | 'api' | 'local' = 'unknown';

  async function migrate(): Promise<number> {
    if (safe(() => localStorage.getItem(flagKey(opts.gridId)), '1')) return 0;
    const old = await local.list();
    if (old.length === 0) {
      safe(() => localStorage.setItem(flagKey(opts.gridId), '1'), undefined);
      return 0;
    }
    let moved = 0;
    let stopped = false;
    for (const v of old) {
      try {
        await remote.save({ name: v.name, filters: v.filters, columns: v.columns, sort: v.sort, visibility: 'PRIVATE' });
        moved++;
      } catch (e) {
        const code = viewErrorCode(e);
        if (code === 'DUPLICATE') moved++; // already on the server under this name
        else if (code === 'VALIDATION') continue; // this one cannot be stored; keep it local
        else {
          stopped = true; // LIMIT, unavailable...: stop, retry the rest next time
          break;
        }
      }
    }
    const left = old.length - moved;
    if (!stopped) safe(() => localStorage.setItem(flagKey(opts.gridId), '1'), undefined);
    if (left === 0) safe(() => localStorage.removeItem(localKey(opts.gridId)), undefined);
    if (moved > 0 || left > 0) opts.onNotice?.({ type: 'migrated', moved, left });
    return moved;
  }

  return {
    get canShare() {
      return mode !== 'local' && !!opts.canShare;
    },
    warnsOnShare: (f) => !!remote.warnsOnShare?.(f),
    async list() {
      if (mode === 'local') return local.list();
      try {
        let views = await remote.list();
        mode = 'api';
        if ((await migrate()) > 0) views = await remote.list();
        return views;
      } catch (e) {
        if (viewErrorCode(e) !== 'UNAVAILABLE' && viewErrorCode(e) !== 'NOT_FOUND') throw e;
        if (mode === 'api') throw e; // was working: do not silently swap stores mid-session
        mode = 'local';
        opts.onNotice?.({ type: 'fallback' });
        return local.list();
      }
    },
    save: (v) => (mode === 'local' ? local.save({ ...v, visibility: undefined }) : remote.save(v)),
    rename: (id, name) => (mode === 'local' ? local.list().then((all) => local.save({ ...all.find((x) => x.id === id)!, name })) : remote.rename!(id, name)),
    delete: (id) => (mode === 'local' ? local.delete(id) : remote.delete(id)),
  };
}
