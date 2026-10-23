import { MantineProvider } from '@mantine/core';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n';
import { DataGrid } from './DataGrid';
import type { ColumnDef } from './types';

interface Row {
  id: string;
  name: string;
  city: string;
}
const rows: Row[] = [
  { id: '1', name: 'Alpha', city: 'Izmir' },
  { id: '2', name: 'Beta', city: '=cmd' },
];
const cols: ColumnDef<Row>[] = [
  { id: 'name', header: 'Name', sortable: true, locked: true, accessor: (r) => r.name },
  { id: 'city', header: 'City', accessor: (r) => r.city },
];

const wrap = (ui: React.ReactElement) => render(<MantineProvider env="test">{ui}</MantineProvider>);

beforeAll(() => i18n.changeLanguage('en'));
beforeEach(() => localStorage.clear());

describe('DataGrid', () => {
  it('shows aria-sort and reports the next sort on header click', () => {
    const onSort = vi.fn();
    wrap(<DataGrid gridId="t" columns={cols} rows={rows} rowId={(r) => r.id} sort={{ field: 'name', dir: 'asc' }} onSort={onSort} />);
    expect(screen.getByRole('columnheader', { name: /Name/ })).toHaveAttribute('aria-sort', 'ascending');
    fireEvent.click(screen.getByRole('button', { name: 'Sort by Name' }));
    expect(onSort).toHaveBeenCalledWith({ field: 'name', dir: 'desc' });
  });

  it('selects all rows on the page and shows the bulk-action slot', () => {
    wrap(<DataGrid gridId="t" columns={cols} rows={rows} rowId={(r) => r.id} selectable bulkActions={({ selectedIds }) => <span>ids:{selectedIds.join(',')}</span>} />);
    expect(screen.queryByRole('region', { name: 'Bulk actions' })).toBeNull();
    fireEvent.click(screen.getByLabelText('Select all rows on this page'));
    const bar = screen.getByRole('region', { name: 'Bulk actions' });
    expect(within(bar).getByText('2 selected')).toBeInTheDocument();
    expect(within(bar).getByText('ids:1,2')).toBeInTheDocument();
    fireEvent.click(within(bar).getByRole('button', { name: 'Clear selection' }));
    expect(screen.queryByRole('region', { name: 'Bulk actions' })).toBeNull();
  });

  it('hides a column from the chooser and remembers it per grid id', () => {
    const { unmount } = wrap(<DataGrid gridId="t" columns={cols} rows={rows} rowId={(r) => r.id} />);
    fireEvent.click(screen.getByRole('button', { name: 'Columns' }));
    fireEvent.click(screen.getByLabelText('City'));
    expect(screen.queryByRole('columnheader', { name: 'City' })).toBeNull();
    expect(JSON.parse(localStorage.getItem('grid.t.columns')!).visible).toEqual(['name']);
    unmount();
    wrap(<DataGrid gridId="t" columns={cols} rows={rows} rowId={(r) => r.id} />);
    expect(screen.queryByRole('columnheader', { name: 'City' })).toBeNull();
  });

  it('exports only visible columns as safe CSV', async () => {
    let blob: Blob | undefined;
    URL.createObjectURL = ((b: Blob) => ((blob = b), 'blob:x')) as typeof URL.createObjectURL;
    URL.revokeObjectURL = () => {};
    HTMLAnchorElement.prototype.click = () => {};
    wrap(<DataGrid gridId="t" columns={cols} rows={rows} rowId={(r) => r.id} exportName="things" />);
    fireEvent.click(screen.getByRole('button', { name: 'Export CSV' }));
    expect(await blob!.text()).toBe('Name;City\r\nAlpha;Izmir\r\nBeta;\'=cmd');
  });

  it('renders loading, error and empty states and phone cards', () => {
    const { rerender } = wrap(<DataGrid gridId="t" columns={cols} rows={undefined} rowId={(r) => r.id} loading />);
    expect(screen.getByTestId('skeleton')).toBeInTheDocument();
    rerender(
      <MantineProvider env="test">
        <DataGrid gridId="t" columns={cols} rows={undefined} rowId={(r) => r.id} error onRetry={() => {}} />
      </MantineProvider>,
    );
    expect(screen.getByRole('alert')).toBeInTheDocument();
    rerender(
      <MantineProvider env="test">
        <DataGrid gridId="t" columns={cols} rows={[]} rowId={(r) => r.id} empty={<p>nothing here</p>} />
      </MantineProvider>,
    );
    expect(screen.getByText('nothing here')).toBeInTheDocument();
    rerender(
      <MantineProvider env="test">
        <DataGrid gridId="t" columns={cols} rows={rows} rowId={(r) => r.id} phone />
      </MantineProvider>,
    );
    expect(screen.queryByRole('table')).toBeNull();
    expect(screen.getByText('Alpha')).toBeInTheDocument();
  });
});

