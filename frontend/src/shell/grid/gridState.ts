import type { ColumnDef, ColumnState, SortState } from './types';

const key = (gridId: string, what: string) => `grid.${gridId}.${what}`;

function read(k: string): unknown {
  try {
    const raw = localStorage.getItem(k);
    return raw == null ? null : JSON.parse(raw);
  } catch {
    return null;
  }
}

function write(k: string, v: unknown): void {
  try {
    localStorage.setItem(k, JSON.stringify(v));
  } catch {
    /* storage unavailable */
  }
}

const strings = (v: unknown): string[] => (Array.isArray(v) ? v.filter((x): x is string => typeof x === 'string') : []);

/** Default state: definition order, default-visible columns. */
export function defaultColumnState<T>(defs: ColumnDef<T>[]): ColumnState {
  return { order: defs.map((d) => d.id), visible: defs.filter((d) => d.defaultVisible !== false).map((d) => d.id) };
}

/** Drops unknown ids, appends new columns (at their default visibility) and force-shows locked ones. */
export function normalizeColumnState<T>(defs: ColumnDef<T>[], saved: ColumnState | null | undefined): ColumnState {
  const base = defaultColumnState(defs);
  if (!saved) return base;
  const known = new Set(defs.map((d) => d.id));
  const order = [...new Set(saved.order.filter((id) => known.has(id)))];
  const seen = new Set(order);
  const fresh = defs.filter((d) => !seen.has(d.id));
  const visible = new Set([
    ...saved.visible.filter((id) => seen.has(id)),
    ...fresh.filter((d) => d.defaultVisible !== false).map((d) => d.id),
    ...defs.filter((d) => d.locked).map((d) => d.id),
  ]);
  return { order: [...order, ...fresh.map((d) => d.id)], visible: [...visible] };
}

export function loadColumnState<T>(gridId: string, defs: ColumnDef<T>[]): ColumnState {
  const v = read(key(gridId, 'columns')) as Partial<ColumnState> | null;
  return normalizeColumnState(defs, v && typeof v === 'object' ? { order: strings(v.order), visible: strings(v.visible) } : null);
}

export function saveColumnState(gridId: string, state: ColumnState): void {
  write(key(gridId, 'columns'), state);
}

/** Visible definitions in the saved order. */
export function visibleColumns<T>(defs: ColumnDef<T>[], state: ColumnState): ColumnDef<T>[] {
  const byId = new Map(defs.map((d) => [d.id, d]));
  const vis = new Set(state.visible);
  return state.order.flatMap((id) => {
    const d = byId.get(id);
    return d && vis.has(id) ? [d] : [];
  });
}

export function toggleColumn<T>(defs: ColumnDef<T>[], state: ColumnState, id: string): ColumnState {
  if (defs.find((d) => d.id === id)?.locked) return state;
  const on = state.visible.includes(id);
  // never hide the last visible column
  if (on && state.visible.length <= 1) return state;
  return { ...state, visible: on ? state.visible.filter((x) => x !== id) : [...state.visible, id] };
}

export function moveColumn(state: ColumnState, id: string, delta: -1 | 1): ColumnState {
  const i = state.order.indexOf(id);
  const j = i + delta;
  if (i < 0 || j < 0 || j >= state.order.length) return state;
  const order = [...state.order];
  [order[i], order[j]] = [order[j]!, order[i]!];
  return { ...state, order };
}

export type Density = 'compact' | 'comfortable';

export function loadDensity(gridId: string): Density {
  return read(key(gridId, 'density')) === 'compact' ? 'compact' : 'comfortable';
}

export function saveDensity(gridId: string, d: Density): void {
  write(key(gridId, 'density'), d);
}

/** Click on a sortable header: new column starts at its first direction, same column flips. */
export function nextSort(current: SortState | null, field: string, firstDir: 'asc' | 'desc' = 'asc'): SortState {
  if (current?.field === field) return { field, dir: current.dir === 'asc' ? 'desc' : 'asc' };
  return { field, dir: firstDir };
}

export function parseSort(s: string | null | undefined): SortState | null {
  if (!s) return null;
  const [field, dir] = s.split(',');
  return field ? { field, dir: dir === 'desc' ? 'desc' : 'asc' } : null;
}

export const formatSort = (s: SortState): string => `${s.field},${s.dir}`;

/** Selection holds ids across pages; "select all" acts on the current page only. */
export function toggleId(sel: ReadonlySet<string>, id: string): Set<string> {
  const n = new Set(sel);
  if (!n.delete(id)) n.add(id);
  return n;
}

export function pageSelection(sel: ReadonlySet<string>, pageIds: string[]): 'none' | 'some' | 'all' {
  const c = pageIds.filter((id) => sel.has(id)).length;
  return c === 0 ? 'none' : c === pageIds.length ? 'all' : 'some';
}

export function togglePage(sel: ReadonlySet<string>, pageIds: string[]): Set<string> {
  const n = new Set(sel);
  if (pageSelection(sel, pageIds) === 'all') pageIds.forEach((id) => n.delete(id));
  else pageIds.forEach((id) => n.add(id));
  return n;
}
