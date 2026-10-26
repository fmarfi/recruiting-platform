import { apiViewStore, type ApiViewStoreOptions } from './apiViewStore';
import type { SavedView, ViewStore } from './types';
import { localViewStore } from './viewStore';
import { viewErrorCode } from './viewErrors';

/** Things the screen should tell the user about the store. */
export type ViewNotice =
  | { type: 'fallback' } // API unavailable: views are local to this browser for now
  | { type: 'migrated'; moved: number; left: number; dropped?: number } // local views were copied to the server once
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
  let inflight: Promise<SavedView<F>[]> | null = null;

  async function migrate(): Promise<number> {
    if (safe(() => localStorage.getItem(flagKey(opts.gridId)), '1')) return 0;
    const old = await local.list();
    if (old.length === 0) {
      safe(() => localStorage.setItem(flagKey(opts.gridId), '1'), undefined);
      return 0;
    }
    let moved = 0;
    let dropped = 0;
    let stopped = false;
    const keep: typeof old = [];
    for (const [i, v] of old.entries()) {
      try {
        await remote.save({ name: v.name, filters: v.filters, columns: v.columns, sort: v.sort, visibility: 'PRIVATE' });
        moved++;
      } catch (e) {
        const code = viewErrorCode(e);
        if (code === 'DUPLICATE') moved++; // already on the server under this name
        else if (code === 'VALIDATION') dropped++; // can never be stored (names may hold personal data): drop it and tell the user
        else {
          stopped = true; // LIMIT, unavailable...: stop, keep this and the rest, retry next time
          keep.push(...old.slice(i));
          break;
        }
      }
    }
    const left = keep.length;
    if (!stopped) safe(() => localStorage.setItem(flagKey(opts.gridId), '1'), undefined);
    // write back only what is still to be moved
    safe(() => (left === 0 ? localStorage.removeItem(localKey(opts.gridId)) : localStorage.setItem(localKey(opts.gridId), JSON.stringify(keep))), undefined);
    if (moved > 0 || left > 0 || dropped > 0) opts.onNotice?.({ type: 'migrated', moved, left, dropped });
    return moved;
  }

  async function doList(): Promise<SavedView<F>[]> {
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
      // views saved from now on live in the local list: let the next API session migrate them
      safe(() => localStorage.removeItem(flagKey(opts.gridId)), undefined);
      opts.onNotice?.({ type: 'fallback' });
      return local.list();
    }
  }

  return {
    get canShare() {
      return mode !== 'local' && !!opts.canShare;
    },
    warnsOnShare: (f) => !!remote.warnsOnShare?.(f),
    list() {
      // one run at a time (StrictMode runs effects twice): a second caller shares the promise, so migration and notice happen once
      inflight ??= doList().finally(() => (inflight = null));
      return inflight;
    },
    save: (v) => (mode === 'local' ? local.save({ ...v, visibility: undefined }) : remote.save(v)),
    rename: (id, name) => (mode === 'local' ? local.list().then((all) => local.save({ ...all.find((x) => x.id === id)!, name })) : remote.rename!(id, name)),
    delete: (id) => (mode === 'local' ? local.delete(id) : remote.delete(id)),
  };
}
