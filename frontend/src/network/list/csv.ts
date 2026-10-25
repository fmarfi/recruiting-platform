// Re-export: the grid's CSV helper is identical.
export { toCsv, downloadCsv } from '../../shell/grid/csv';
import type { ColumnDef } from '../../shell/grid';

/** Header and rows of the visible columns (same data the grid's own CSV button writes, but for any set of rows). */
export function visibleCsv<T>(cols: ColumnDef<T>[], rows: T[]): { header: string[]; rows: unknown[][] } {
  return { header: cols.map((c) => c.header), rows: rows.map((r) => cols.map((c) => c.accessor?.(r) ?? '')) };
}
