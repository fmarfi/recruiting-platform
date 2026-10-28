import { Box, Button, Card, Checkbox, Group, SegmentedControl, Stack, Table, Text, UnstyledButton } from '@mantine/core';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ErrorBanner } from '../components/ErrorBanner';
import { SkeletonRows } from '../components/SkeletonRows';
import { ColumnChooser } from './ColumnChooser';
import { downloadCsv, toCsv } from './csv';
import { loadDensity, nextSort, pageSelection, saveDensity, toggleId, togglePage, type Density } from './gridState';
import type { ColumnDef, GroupRows, SortState } from './types';
import { useGridColumns, type GridColumns } from './useGridColumns';

export interface DataGridProps<T> {
  /** Stable id: column order, visibility and density are stored under it. */
  gridId: string;
  columns: ColumnDef<T>[];
  rows: T[] | undefined;
  rowId: (row: T) => string;
  loading?: boolean;
  error?: boolean;
  onRetry?: () => void;
  /** Shown when `rows` is an empty array. */
  empty?: ReactNode;
  /** Current server-side sort and the callback fired with the next one when a sortable header is clicked. */
  sort?: SortState | null;
  onSort?: (next: SortState) => void;
  selectable?: boolean;
  /** Bulk-action bar slot: rendered while at least one row is selected. */
  bulkActions?: (ctx: { selectedIds: string[]; clear: () => void }) => ReactNode;
  /** Slot at the left of the grid toolbar (filters summary, views menu ...). */
  toolbar?: ReactNode;
  /** File name without extension; enables the CSV export button (visible columns, current page). */
  exportName?: string;
  /** Phone layout switch is decided by the screen (useMediaQuery) so tests stay deterministic. */
  phone?: boolean;
  renderCard?: (row: T) => ReactNode;
  /** Pass the result of useGridColumns when the screen needs the column state (saved views). */
  columnsController?: GridColumns<T>;
  /** Extension point for group-by (not wired to any UI yet). */
  groupBy?: GroupRows<T>;
  /** Pixels under which the sticky header sticks (app header height). */
  stickyOffset?: number;
}

function cellValue<T>(c: ColumnDef<T>, row: T): ReactNode {
  if (c.cell) return c.cell(row);
  const v = c.accessor?.(row);
  return v == null || v === '' ? '-' : String(v);
}

/** Reusable list grid: sortable, selectable, column chooser, density, CSV, phone cards. */
export function DataGrid<T>(p: DataGridProps<T>) {
  const { t } = useTranslation();
  const own = useGridColumns(p.gridId, p.columns);
  const cols = p.columnsController ?? own;
  const [density, setDensity] = useState<Density>(() => loadDensity(p.gridId));
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const rows = p.rows ?? [];
  const pageIds = rows.map(p.rowId);
  const sel = pageSelection(selected, pageIds);
  const selectedIds = [...selected];
  const clear = () => setSelected(new Set());

  const exportCsv = () => {
    const vis = cols.visible;
    downloadCsv(`${p.exportName ?? p.gridId}.csv`, toCsv(vis.map((c) => c.header), rows.map((r) => vis.map((c) => c.accessor?.(r) ?? ''))));
  };

  const groups = p.groupBy ? p.groupBy(rows) : [{ key: '', label: '', rows }];

  const toolbar = (
    <Group justify="space-between" gap="sm" wrap="wrap">
      <Group gap="sm">{p.toolbar}</Group>
      <Group gap="xs">
        <SegmentedControl
          size="sm"
          aria-label={t('grid.density')}
          value={density}
          onChange={(v) => {
            setDensity(v as Density);
            saveDensity(p.gridId, v as Density);
          }}
          data={[
            { value: 'compact', label: t('grid.compact') },
            { value: 'comfortable', label: t('grid.comfortable') },
          ]}
        />
        {p.exportName && (
          <Button variant="default" size="sm" onClick={exportCsv} disabled={rows.length === 0}>
            {t('grid.exportCsv')}
          </Button>
        )}
        {!p.phone && <ColumnChooser defs={p.columns} columns={cols} />}
      </Group>
    </Group>
  );

  const bulk =
    p.selectable && selected.size > 0 ? (
      <Group justify="space-between" p="xs" role="region" aria-label={t('grid.bulkBar')} style={{ background: 'var(--color-accentSoft)', borderRadius: 'var(--radius-md)' }}>
        <Text size="sm" fw={600}>
          {t('grid.selected', { n: selected.size })}
        </Text>
        <Group gap="xs">
          {p.bulkActions?.({ selectedIds, clear })}
          <Button variant="subtle" size="xs" onClick={clear}>
            {t('grid.clearSelection')}
          </Button>
        </Group>
      </Group>
    ) : null;

  let body: ReactNode;
  if (p.error) body = <ErrorBanner onRetry={p.onRetry} />;
  else if (p.loading && !p.rows) body = <SkeletonRows n={8} />;
  else if (p.rows && rows.length === 0) body = p.empty ?? null;
  else if (p.phone) {
    body = (
      <Stack gap="xs">
        {rows.map((r) => {
          const id = p.rowId(r);
          return (
            <Card key={id} withBorder padding="md">
              <Group wrap="nowrap" align="flex-start">
                {p.selectable && <Checkbox checked={selected.has(id)} onChange={() => setSelected(toggleId(selected, id))} aria-label={t('grid.selectRow')} />}
                <Box style={{ flex: 1, minWidth: 0 }}>
                  {p.renderCard ? (
                    p.renderCard(r)
                  ) : (
                    <Stack gap={2}>
                      {cols.visible.map((c, i) => (
                        <Text key={c.id} size={i === 0 ? 'md' : 'sm'} fw={i === 0 ? 600 : 400} c={i === 0 ? undefined : 'dimmed'} component="div">
                          {i === 0 ? '' : `${c.header}: `}
                          {cellValue(c, r)}
                        </Text>
                      ))}
                    </Stack>
                  )}
                </Box>
              </Group>
            </Card>
          );
        })}
      </Stack>
    );
  } else {
    body = (
      <Table highlightOnHover withTableBorder stickyHeader stickyHeaderOffset={p.stickyOffset ?? 56} verticalSpacing={density === 'compact' ? 6 : 14} aria-busy={!!p.loading} style={{ opacity: p.loading ? 0.6 : 1 }}>
        <Table.Thead>
          <Table.Tr>
            {p.selectable && (
              <Table.Th w={40}>
                <Checkbox checked={sel === 'all'} indeterminate={sel === 'some'} onChange={() => setSelected(togglePage(selected, pageIds))} aria-label={t('grid.selectPage')} />
              </Table.Th>
            )}
            {cols.visible.map((c) => {
              const field = c.sortKey ?? c.id;
              const active = p.sort?.field === field;
              return (
                <Table.Th key={c.id} style={{ minWidth: c.minWidth, textAlign: c.numeric ? 'right' : undefined }} aria-sort={c.sortable ? (active ? (p.sort!.dir === 'asc' ? 'ascending' : 'descending') : 'none') : undefined}>
                  {c.sortable && p.onSort ? (
                    <UnstyledButton fw={600} fz="inherit" onClick={() => p.onSort!(nextSort(p.sort ?? null, field, c.firstSortDir))} aria-label={t('grid.sortBy', { col: c.header })}>
                      {c.header} {active ? (p.sort!.dir === 'asc' ? '\u25B2' : '\u25BC') : ''}
                    </UnstyledButton>
                  ) : (
                    c.header
                  )}
                </Table.Th>
              );
            })}
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {groups.map((g) => [
            g.label ? (
              <Table.Tr key={`g-${g.key}`}>
                <Table.Td colSpan={cols.visible.length + (p.selectable ? 1 : 0)} fw={600} bg="var(--color-bg, transparent)">
                  {g.label} ({g.rows.length})
                </Table.Td>
              </Table.Tr>
            ) : null,
            ...g.rows.map((r) => {
              const id = p.rowId(r);
              return (
                <Table.Tr key={id} bg={selected.has(id) ? 'var(--color-accentSoft)' : undefined}>
                  {p.selectable && (
                    <Table.Td>
                      <Checkbox checked={selected.has(id)} onChange={() => setSelected(toggleId(selected, id))} aria-label={t('grid.selectRow')} />
                    </Table.Td>
                  )}
                  {cols.visible.map((c) => (
                    <Table.Td key={c.id} style={c.numeric ? { textAlign: 'right', fontVariantNumeric: 'tabular-nums' } : undefined}>{cellValue(c, r)}</Table.Td>
                  ))}
                </Table.Tr>
              );
            }),
          ])}
        </Table.Tbody>
      </Table>
    );
  }

  return (
    <Stack gap="sm">
      {toolbar}
      {bulk}
      {body}
    </Stack>
  );
}
