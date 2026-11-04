import { act, fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { renderShell } from '../../shell/edit/testUtils';

const GET = vi.fn();
const POST = vi.fn();
const PATCH = vi.fn();
const PUT = vi.fn();
vi.mock('../../api', () => ({ api: { GET: (...a: unknown[]) => GET(...a), POST: (...a: unknown[]) => POST(...a), PATCH: (...a: unknown[]) => PATCH(...a), PUT: (...a: unknown[]) => PUT(...a) } }));

import { BulkAddDialog } from './BulkAddDialog';
import { LonglistPanel } from './LonglistPanel';

const app = (id: string, name: string, rank: number) =>
  ({ id, rank, list: 'LONGLIST', stage: 'SOURCED', version: 1, rating: null, rationale: null, outreachStatus: 'TO_CALL', person: { id: `p-${id}`, name, title: 'CFO', company: 'Acme' } }) as never;

beforeEach(() => {
  Object.defineProperty(document, 'fonts', { value: { addEventListener() {}, removeEventListener() {} }, configurable: true });
  [GET, POST, PATCH, PUT].forEach((m) => m.mockReset());
});

describe('LonglistPanel', () => {
  const apps = [app('a1', 'Ayse', 1), app('a2', 'Burak', 2), app('a3', 'Cem', 3)];
  const setup = () => renderShell(<MemoryRouter><LonglistPanel jobId="j1" apps={apps} readOnly={false} reload={vi.fn()} onBulkAdd={vi.fn()} /></MemoryRouter>);

  it('reorders: moving Burak up sends the new full order to the ranking endpoint', async () => {
    PUT.mockResolvedValue({ data: [app('a2', 'Burak', 1), app('a1', 'Ayse', 2), app('a3', 'Cem', 3)] });
    setup();
    fireEvent.click(screen.getByRole('button', { name: 'Move Burak up' }));
    await act(async () => {});
    expect(PUT).toHaveBeenCalledWith('/job-orders/{id}/ranking', { params: { path: { id: 'j1' } }, body: { list: 'LONGLIST', applicationIds: ['a2', 'a1', 'a3'] } });
    expect(screen.getAllByRole('link').map((n) => n.textContent)).toEqual(['Burak', 'Ayse', 'Cem']);
  });

  it('on a ranking 409 reloads and shows a short inline message', async () => {
    PUT.mockResolvedValue({ error: { code: 'STATE_CONFLICT' }, response: { status: 409 } });
    const reload = vi.fn();
    renderShell(<MemoryRouter><LonglistPanel jobId="j1" apps={apps} readOnly={false} reload={reload} onBulkAdd={vi.fn()} /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: 'Move Burak up' }));
    await act(async () => {});
    expect(screen.getByTestId('longlist-error')).toHaveTextContent('The list changed elsewhere');
    expect(reload).toHaveBeenCalled();
    expect(screen.getAllByRole('link').map((n) => n.textContent)).toEqual(['Ayse', 'Burak', 'Cem']);
  });

  it('shows the contract message inline when the shortlist needs a rationale', async () => {
    PATCH.mockResolvedValue({ error: { code: 'TRANSITION_GUARD_FAILED', errors: [{ field: 'rationale', code: 'SIZE', message: 'rationale must have at least 20 characters' }] } });
    setup();
    fireEvent.click(screen.getAllByRole('button', { name: 'To shortlist' })[0]!);
    fireEvent.click(await screen.findByRole('button', { name: 'Move to shortlist' }));
    await act(async () => {});
    expect(PATCH).toHaveBeenCalledWith('/applications/{id}/stage', expect.objectContaining({ body: expect.objectContaining({ stage: 'PRESENTED' }) }));
    expect(screen.getByTestId('shortlist-error')).toHaveTextContent('rationale must have at least 20 characters');
  });
});

describe('BulkAddDialog', () => {
  it('adds several selected people with one POST each', async () => {
    const people = ['n1', 'n2', 'n3'].map((id) => ({ id, name: `Person ${id}`, title: 'Finance Director', company: 'Co', city: 'Ankara' }));
    GET.mockResolvedValue({ data: { content: people } });
    POST.mockResolvedValue({ data: {} });
    const onAdded = vi.fn();
    renderShell(<BulkAddDialog jobId="j1" existing={new Set(['n3'])} opened onClose={vi.fn()} onAdded={onAdded} />);
    await act(async () => {});
    expect(GET).toHaveBeenCalledWith('/people', { params: { query: expect.objectContaining({ size: 100 }) } });
    fireEvent.click(screen.getByLabelText(/Select all/));
    fireEvent.click(screen.getByRole('button', { name: 'Add 2 selected' }));
    await act(async () => {});
    expect(POST).toHaveBeenCalledTimes(2);
    expect(POST).toHaveBeenCalledWith('/job-orders/{id}/applications', { params: { path: { id: 'j1' } }, body: { personId: 'n1' } });
    expect(POST).toHaveBeenCalledWith('/job-orders/{id}/applications', { params: { path: { id: 'j1' } }, body: { personId: 'n2' } });
    expect(onAdded).toHaveBeenCalled();
  });

  it('reports partial failure, keeps the failed person selected and still calls onAdded', async () => {
    const people = ['n1', 'n2', 'n3'].map((id) => ({ id, name: `Person ${id}`, title: 'FD', company: 'Co', city: 'Ankara' }));
    GET.mockResolvedValue({ data: { content: people } });
    POST.mockImplementation((_p: string, o: { body: { personId: string } }) => Promise.resolve(o.body.personId === 'n2' ? { error: { detail: 'Off limits' } } : { data: {} }));
    const onAdded = vi.fn();
    const onClose = vi.fn();
    renderShell(<BulkAddDialog jobId="j1" existing={new Set()} opened onClose={onClose} onAdded={onAdded} />);
    await act(async () => {});
    fireEvent.click(screen.getByLabelText(/Select all/));
    fireEvent.click(screen.getByRole('button', { name: 'Add 3 selected' }));
    await act(async () => {});
    expect(screen.getByTestId('bulk-summary')).toHaveTextContent('Added 2, failed 1');
    expect(screen.getByTestId('bulk-failed')).toHaveTextContent('Person n2: Off limits');
    expect(onAdded).toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Add 1 selected' })).toBeInTheDocument();
  });
});
