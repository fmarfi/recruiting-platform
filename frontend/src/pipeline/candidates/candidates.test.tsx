import { act, fireEvent, screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { renderShell } from '../../shell/edit/testUtils';

const REF = { lifecycles: { consent: { statuses: [] }, pipelineStage: { statuses: [{ value: 'SOURCED', labelEn: 'Sourced', labelTr: 'Sourced', next: [{ to: 'SCREENED', reasonRequired: false }, { to: 'CLIENT_INTERVIEW', reasonRequired: false }, { to: 'REJECTED', reasonRequired: true }] }] } } };
const refState = { data: undefined as unknown };
vi.mock('../shared/useRefData', () => ({ useRefData: () => refState.data }));
vi.mock('../shared/useCurrentUser', () => ({ useCurrentUser: () => ({ role: 'CONSULTANT' }) }));
const PATCH = vi.fn();
const DELETE = vi.fn();
vi.mock('../../api', () => ({ api: { GET: vi.fn(), POST: vi.fn(), PATCH: (...a: unknown[]) => PATCH(...a), PUT: vi.fn(), DELETE: (...a: unknown[]) => DELETE(...a) } }));

import { CandidateTable } from './CandidateTable';
import { useStageControl } from './useStageControl';
import { countByStage, filterByStage, sortApps } from './candidateRows';
import type { App } from '../longlist/useLonglist';

const app = (id: string, name: string, stage: string, list: string | null, rank: number | null = null) =>
  ({ id, rank, list, stage, version: 1, rating: null, person: { id: `p-${id}`, name, title: 'CFO', company: 'Acme', consentStatus: 'GIVEN', lastContactAt: null } }) as unknown as App;

const apps = [app('a1', 'Ayse', 'SOURCED', 'LONGLIST', 2), app('a2', 'Burak', 'PRESENTED', 'SHORTLIST', 1), app('a3', 'Cem', 'SOURCED', 'LONGLIST', 1), app('a4', 'Deniz', 'REJECTED', null)];

beforeEach(() => {
  refState.data = undefined;
  Element.prototype.scrollIntoView = vi.fn();
  PATCH.mockReset();
  DELETE.mockReset();
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
        <CandidateTable apps={apps} readOnly={false} onStage={onStage} onRate={vi.fn()} onLog={vi.fn()} onRemove={vi.fn()} />
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

function Harness() {
  const c = useStageControl(apps, vi.fn(), vi.fn());
  return (
    <MemoryRouter>
      {c.toast}
      <CandidateTable apps={c.apps} readOnly={false} onStage={c.change} onRate={vi.fn()} onLog={vi.fn()} onRemove={(a) => void c.remove(a)} busyIds={c.busy} />
      {c.dialogs}
    </MemoryRouter>
  );
}
const pick = (name: string, option: string) => {
  fireEvent.click(screen.getByRole('combobox', { name }));
  fireEvent.click(screen.getByRole('option', { name: option }));
};

describe('stage control', () => {
  beforeEach(() => {
    refState.data = REF;
  });

  it('asks for a reason before rejecting', async () => {
    PATCH.mockResolvedValue({ data: { ...apps[2], stage: 'REJECTED' } });
    renderShell(<Harness />);
    pick('Stage of Cem', 'REJECTED');
    expect(PATCH).not.toHaveBeenCalled();
    fireEvent.change(within(await screen.findByRole('dialog')).getByRole('textbox'), { target: { value: 'Not a fit' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));
    await act(async () => {});
    expect(PATCH).toHaveBeenCalledWith('/applications/{id}/stage', expect.objectContaining({ body: expect.objectContaining({ stage: 'REJECTED', reason: 'Not a fit' }) }));
  });

  it('opens the shortlist dialog for any client-facing target from the longlist and sends that stage', async () => {
    PATCH.mockResolvedValue({ error: { code: 'TRANSITION_GUARD_FAILED', errors: [{ message: 'rationale must have at least 20 characters' }] } });
    renderShell(<Harness />);
    pick('Stage of Cem', 'CLIENT_INTERVIEW');
    expect(PATCH).not.toHaveBeenCalled();
    fireEvent.click(await screen.findByRole('button', { name: 'Move to shortlist' }));
    await act(async () => {});
    expect(PATCH).toHaveBeenCalledWith('/applications/{id}/stage', expect.objectContaining({ body: expect.objectContaining({ stage: 'CLIENT_INTERVIEW' }) }));
    expect(screen.getByTestId('shortlist-error')).toHaveTextContent('rationale must have at least 20 characters');
  });

  it('shows a network error in the shortlist dialog instead of swallowing it', async () => {
    PATCH.mockRejectedValue(new Error('network'));
    renderShell(<Harness />);
    pick('Stage of Cem', 'PRESENTED');
    fireEvent.click(await screen.findByRole('button', { name: 'Move to shortlist' }));
    await act(async () => {});
    expect(screen.getByTestId('shortlist-error')).toHaveTextContent('The change was not saved.');
  });

  it('shows a 409 from a plain move to the user', async () => {
    PATCH.mockResolvedValue({ error: { code: 'VERSION_CONFLICT', detail: 'Changed by someone else' }, response: { status: 409 } });
    renderShell(<Harness />);
    pick('Stage of Cem', 'SCREENED');
    await act(async () => {});
    expect(screen.getByRole('alert')).toHaveTextContent('Changed by someone else');
  });

  it('removes a sourced candidate from the search', async () => {
    DELETE.mockResolvedValue({});
    renderShell(<Harness />);
    fireEvent.click(screen.getByRole('button', { name: /Remove from search: Cem/ }));
    await act(async () => {});
    expect(DELETE).toHaveBeenCalledWith('/applications/{id}', { params: { path: { id: 'a3' } } });
  });
});
