import { MantineProvider } from '@mantine/core';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, useNavigate } from 'react-router-dom';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n';
import { AuthContext, type AuthCtx } from '../../shell/auth/useAuth';
import { AccountsList } from './AccountsList';
import { useAccounts } from './useAccounts';

vi.mock('../components/NewAccountDialog', () => ({ NewAccountDialog: () => null }));
vi.mock('./AccountFilterBar', () => ({ AccountFilterBar: () => null }));
vi.mock('../../shell/components/StatusBadge', () => ({ StatusBadge: ({ value }: { value: string }) => <span>{value}</span> }));
vi.mock('./useAccounts', () => ({ useAccounts: vi.fn() }));

const acc = { id: 'a1', name: 'Acme', sector: 'FMCG', city: 'Izmir', owner: { id: 'u1', name: 'Una' }, status: 'ACTIVE', openJobCount: 2, lastActivityAt: null, openedOn: '2025-01-01', website: null };
const auth = { user: { id: 'u1', name: 'U' }, status: 'authed', isManager: false, isAdmin: false } as unknown as AuthCtx;

function Jump() {
  const nav = useNavigate();
  return <button onClick={() => nav('/accounts?status=LEAD&owner=all')}>jump</button>;
}

function renderAt(url: string) {
  return render(
    <MantineProvider env="test">
      <AuthContext.Provider value={auth}>
        <MemoryRouter initialEntries={[url]}>
          <Jump />
          <AccountsList />
        </MemoryRouter>
      </AuthContext.Provider>
    </MantineProvider>,
  );
}

beforeAll(() => i18n.changeLanguage('en'));
beforeEach(() => {
  localStorage.clear();
  vi.mocked(useAccounts).mockReturnValue({ data: { content: [acc], page: { number: 0, size: 20, totalElements: 1, totalPages: 1 } }, loading: false, reload: () => {} } as never);
});

describe('AccountsList', () => {
  it('reads status and owner from the URL query and defaults the owner to me', () => {
    renderAt('/accounts?status=active&owner=me&q=ac');
    expect(vi.mocked(useAccounts)).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'ACTIVE', ownerId: 'u1', q: 'ac', sort: 'lastActivityAt,desc' }));
  });

  it('honours owner=all and keeps the default otherwise', () => {
    renderAt('/accounts?owner=all');
    expect(vi.mocked(useAccounts)).toHaveBeenLastCalledWith(expect.objectContaining({ ownerId: '' }));
  });

  it('offers column chooser, views and CSV export, and sorts server-side', () => {
    renderAt('/accounts');
    expect(screen.getByRole('button', { name: 'Columns' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Views' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export CSV' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /Last activity/ })).toHaveAttribute('aria-sort', 'descending');
    fireEvent.click(screen.getByRole('button', { name: 'Sort by Name' }));
    expect(vi.mocked(useAccounts)).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'name,asc' }));
  });

  it('applies a URL change while open, and writes filter changes back to the URL', () => {
    renderAt('/accounts');
    expect(vi.mocked(useAccounts)).toHaveBeenLastCalledWith(expect.objectContaining({ status: '', ownerId: 'u1' }));
    fireEvent.click(screen.getByText('jump'));
    expect(vi.mocked(useAccounts)).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'LEAD', ownerId: '' }));
    // sorting goes through the URL too: the next render reads it back
    fireEvent.click(screen.getByRole('button', { name: 'Sort by Name' }));
    expect(vi.mocked(useAccounts)).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'LEAD', ownerId: '', sort: 'name,asc' }));
  });

  it('saves the current view and lists it', async () => {
    renderAt('/accounts?status=LEAD');
    fireEvent.click(screen.getByRole('button', { name: 'Views' }));
    fireEvent.click(await screen.findByText('Save current view'));
    fireEvent.change(screen.getByLabelText('View name'), { target: { value: 'Leads' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await vi.waitFor(() => expect(JSON.parse(localStorage.getItem('grid.accounts.views')!)[0]).toMatchObject({ name: 'Leads', filters: { status: 'LEAD', owner: 'me' } }));
  });
});
