import { act, fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { axeViolations, renderShell } from '../edit/testUtils';
import { HistoryPanel } from './HistoryPanel';

const rev = (n: number, over = {}) => ({
  revision: n,
  at: '2026-10-16T09:30:00Z',
  user: { id: 'u1', name: 'Deniz Aksoy' },
  type: 'MOD' as const,
  changes: [{ field: 'status', from: 'ACTIVE', to: 'ON_HOLD' }],
  ...over,
});
const page = (content: unknown[], total: number, number = 0) => ({ data: { content, page: { size: 1, number, totalElements: total, totalPages: 2 } } });

describe('HistoryPanel', () => {
  it('requests the revisions of the record and lists who changed what', async () => {
    const GET = vi.fn().mockResolvedValue(page([rev(2), rev(1, { type: 'ADD', user: null, changes: [{ field: 'name', to: 'Acme' }] })], 2));
    renderShell(<HistoryPanel entityType="ACCOUNT" entityId="a1" client={{ GET } as never} />);
    await act(async () => {});
    expect(GET).toHaveBeenCalledWith('/audit/revisions', { params: { query: { entityType: 'ACCOUNT', entityId: 'a1', page: 0, size: 20 } } });
    expect(screen.getByRole('heading', { name: 'History' })).toBeInTheDocument();
    expect(screen.getByText('Deniz Aksoy')).toBeInTheDocument();
    expect(screen.getByText('status: ACTIVE to ON_HOLD')).toBeInTheDocument();
    expect(screen.getByText('System')).toBeInTheDocument();
    expect(screen.getByText('name: (empty) to Acme')).toBeInTheDocument();
  });

  it('loads the next page on demand', async () => {
    const GET = vi.fn().mockResolvedValueOnce(page([rev(2)], 2)).mockResolvedValueOnce(page([rev(1)], 2, 1));
    renderShell(<HistoryPanel entityType="PERSON" entityId="p1" pageSize={1} client={{ GET } as never} />);
    await act(async () => {});
    fireEvent.click(screen.getByRole('button', { name: 'Load more' }));
    await act(async () => {});
    expect(GET).toHaveBeenLastCalledWith('/audit/revisions', { params: { query: { entityType: 'PERSON', entityId: 'p1', page: 1, size: 1 } } });
    expect(screen.getAllByRole('listitem').length).toBeGreaterThanOrEqual(2);
    expect(screen.queryByRole('button', { name: 'Load more' })).toBeNull();
  });

  it('shows empty and error states', async () => {
    const empty = renderShell(<HistoryPanel entityType="ACCOUNT" entityId="a" client={{ GET: vi.fn().mockResolvedValue(page([], 0)) } as never} />);
    await act(async () => {});
    expect(screen.getByText('No changes recorded yet.')).toBeInTheDocument();
    empty.unmount();
    renderShell(<HistoryPanel entityType="ACCOUNT" entityId="a" client={{ GET: vi.fn().mockResolvedValue({ error: {} }) } as never} />);
    await act(async () => {});
    expect(screen.getByRole('alert')).toHaveTextContent('Could not load history.');
  });

  it('is axe clean', async () => {
    renderShell(<HistoryPanel entityType="ACCOUNT" entityId="a" client={{ GET: vi.fn().mockResolvedValue(page([rev(1)], 1)) } as never} />);
    await act(async () => {});
    expect(await axeViolations()).toEqual([]);
  });
});
