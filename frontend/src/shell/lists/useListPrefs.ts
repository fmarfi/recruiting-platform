// Per-user list preferences stored in this browser: density, visible columns and saved views (S3).
import { useCallback, useState } from 'react';
import { readStore, writeStore } from '../lib/browserStore';

export type Density = 'comfortable' | 'compact';

export interface SavedView<F> {
  name: string;
  filters: F;
}

/** Density of lists, one stored preference shared by all of them. */
export function useDensity(): [Density, (d: Density) => void] {
  const [density, setDensity] = useState<Density>(() => readStore<Density>('rp.density', 'comfortable'));
  const set = useCallback((d: Density) => {
    setDensity(d);
    writeStore('rp.density', d);
  }, []);
  return [density, set];
}

/** Visible column keys of a list; all columns by default, persisted per list. */
export function useVisibleColumns(listKey: string, all: string[]): [string[], (cols: string[]) => void] {
  const key = `rp.columns.${listKey}`;
  const [cols, setCols] = useState<string[]>(() => readStore<string[]>(key, all).filter((c) => all.includes(c)));
  const set = useCallback(
    (next: string[]) => {
      setCols(next);
      writeStore(key, next);
    },
    [key],
  );
  return [cols, set];
}

/** Saved filter combinations of one list, persisted per list. */
export function useSavedViews<F>(listKey: string) {
  const key = `rp.views.${listKey}`;
  const [views, setViews] = useState<SavedView<F>[]>(() => readStore<SavedView<F>[]>(key, []));

  const persist = (next: SavedView<F>[]) => {
    setViews(next);
    writeStore(key, next);
  };

  return {
    views,
    /** Adds a view, or replaces the one with the same name. */
    save: (name: string, filters: F) => persist([...views.filter((v) => v.name !== name), { name, filters }]),
    remove: (name: string) => persist(views.filter((v) => v.name !== name)),
  };
}
