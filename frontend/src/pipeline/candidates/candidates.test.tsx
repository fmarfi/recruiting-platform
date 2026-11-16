import { fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { renderShell } from '../../shell/edit/testUtils';

vi.mock('../shared/useRefData', () => ({ useRefData: () => undefined }));
vi.mock('../../api', () => ({ api: { GET: vi.fn(), POST: vi.fn(), PATCH: vi.fn(), PUT: vi.fn() } }));

import { CandidateTable } from './CandidateTable';
import { countByStage, filterByStage, sortApps } from './candidateRows';
import type { App } from '../longlist/useLonglist';

const app = (id: string, name: string, stage: string, list: string | null, rank: number | null = null) =>
  ({ id, rank, list, stage, version: 1, rating: null, person: { id: `p-${id}`, name, title: 'CFO', company: 'Acme', consentStatus: 'GIVEN', lastContactAt: null } }) as unknown as App;

const apps = [app('a1', 'Ayse', 'SOURCED', 'LONGLIST', 2), app('a2', 'Burak', 'PRESENTED', 'SHORTLIST', 1), app('a3', 'Cem', 'SOURCED', 'LONGLIST', 1), app('a4', 'Deniz', 'REJECTED', null)];

beforeEach(() => {
  Object.defineProperty(document, 'fonts', { value: { addEventListener() {}, removeEventListener() {} }, configurable: true });
});

describe('candidateRows', () => {
  it('orders by stage then rank, filters and counts by stage', () => {
    expect(sortApps(apps, null).map((a) => a.person.name)).toEqual(['Cem', 'Ayse', 'Burak', 'Deniz']);
    expect(sortApps(apps, { field: 'name', dir: 'desc' }).map((a) => a.person.name)).toEqual(['Deniz', 'Cem', 'Burak', 'Ayse']);
    expect(filterByStage(apps, ['SOURCED']).map((a) => a.id)).toEqual(['a1', 'a3']);
    expect(countByStage(apps)).toEqual({ SOURCED: 2, PRESENTED: 1, REJECTED: 1 });
  });
});

describe('CandidateTable', () => {
  const setup = (onStage = vi.fn()) => {
    renderShell(
      <MemoryRouter>
        <CandidateTable apps={apps} readOnly={false} onStage={onStage} onRate={vi.fn()} onLog={vi.fn()} />
      </MemoryRouter>,
    );
    return onStage;
  };

  it('shows every candidate in one table with stage, list and consent columns', () => {
    setup();
    expect(screen.getAllByRole('row')).toHaveLength(5);
    expect(screen.getByRole('columnheader', { name: /Stage/ })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /List/ })).toBeInTheDocument();
    expect(screen.getAllByText('Longlist').length).toBeGreaterThan(0);
    expect(screen.queryByTestId('card-a1')).toBeNull();
  });

  it('filters by name and shows an empty state when nothing matches', () => {
    setup();
    fireEvent.change(screen.getByLabelText('Filter by name or company'), { target: { value: 'burak' } });
    expect(screen.getAllByRole('row')).toHaveLength(2);
    fireEvent.change(screen.getByLabelText('Filter by name or company'), { target: { value: 'zzz' } });
    expect(screen.getByText('No candidates match')).toBeInTheDocument();
  });

  it('changes the stage from the row select', () => {
    const onStage = setup();
    fireEvent.click(screen.getByRole('combobox', { name: 'Stage of Cem' }));
    fireEvent.click(screen.getByRole('option', { name: 'PRESENTED' }));
    expect(onStage).toHaveBeenCalledWith(expect.objectContaining({ id: 'a3' }), 'PRESENTED');
  });
});
