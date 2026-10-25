import { useCallback, useState } from 'react';
import { loadColumnState, moveColumn, normalizeColumnState, saveColumnState, toggleColumn, defaultColumnState, visibleColumns } from './gridState';
import type { ColumnDef, ColumnState } from './types';

/** Column order/visibility for one grid, persisted per grid id. Screens may call it to feed saved views. */
export function useGridColumns<T>(gridId: string, defs: ColumnDef<T>[]) {
  const [state, setRaw] = useState<ColumnState>(() => loadColumnState(gridId, defs));
  const normalized = normalizeColumnState(defs, state);
  const set = useCallback(
    (next: ColumnState) => {
      const n = normalizeColumnState(defs, next);
      setRaw(n);
      saveColumnState(gridId, n);
    },
    [defs, gridId],
  );
  return {
    state: normalized,
    set,
    visible: visibleColumns(defs, normalized),
    toggle: (id: string) => set(toggleColumn(defs, normalized, id)),
    move: (id: string, delta: -1 | 1) => set(moveColumn(normalized, id, delta)),
    reset: () => set(defaultColumnState(defs)),
  };
}

export type GridColumns<T> = ReturnType<typeof useGridColumns<T>>;
