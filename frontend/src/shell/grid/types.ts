import type { ReactNode } from 'react';

/** Sort as the server expects it ("field,dir"). */
export interface SortState {
  field: string;
  dir: 'asc' | 'desc';
}

/** Order and visibility of columns, persisted per grid id. `visible` holds ids; ids missing from `order` follow their default. */
export interface ColumnState {
  order: string[];
  visible: string[];
}

export interface ColumnDef<T> {
  id: string;
  /** Already translated header text; also the CSV header and the card label on phones. */
  header: string;
  /** Rendered cell; falls back to `accessor`. */
  cell?: (row: T) => ReactNode;
  /** Plain value, used for the default cell and for CSV export. */
  accessor?: (row: T) => unknown;
  /** Server-side sortable; the sort field sent to the callback is `sortKey ?? id`. */
  sortable?: boolean;
  sortKey?: string;
  /** Direction applied the first time the column is sorted (default asc). */
  firstSortDir?: 'asc' | 'desc';
  /** Default true. */
  defaultVisible?: boolean;
  /** Pixels. */
  minWidth?: number;
  /** Columns that cannot be hidden in the chooser (e.g. the name). */
  locked?: boolean;
}

/** A named, saved screen configuration. `filters` is opaque to the grid; the screen owns its shape. */
export interface SavedView<F = unknown> {
  id: string;
  name: string;
  filters: F;
  columns?: ColumnState;
  sort?: SortState | null;
}

export type NewView<F = unknown> = Omit<SavedView<F>, 'id'> & { id?: string };

/**
 * Where saved views live. Methods are async so a server-backed store (the /views endpoints) can replace
 * the localStorage one without touching screens.
 */
export interface ViewStore<F = unknown> {
  list(): Promise<SavedView<F>[]>;
  /** Creates a view, or replaces the one with the same id. */
  save(view: NewView<F>): Promise<SavedView<F>>;
  delete(id: string): Promise<void>;
}

/**
 * Extension point for group-by (later): a screen may pass a function that splits the page rows into labelled
 * groups; the grid renders a header row per group.
 */
export interface RowGroup<T> {
  key: string;
  label: string;
  rows: T[];
}
export type GroupRows<T> = (rows: T[]) => RowGroup<T>[];
