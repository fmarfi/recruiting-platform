import { MantineProvider } from '@mantine/core';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n';
import { AuthContext, type AuthCtx } from '../auth/useAuth';
import { CommandButton, useCommandHotkey } from './CommandButton';
import { CommandPalette } from './CommandPalette';
import { useState, useCallback } from 'react';

vi.mock('../../accounts/components/NewAccountDialog', () => ({ NewAccountDialog: () => null }));
vi.mock('../../network/list/NewPersonDialog', () => ({ NewPersonDialog: () => null }));
vi.mock('../../pipeline/quickadd/QuickAdd', () => ({ QuickAdd: () => null }));

vi.mock('../components/StatusBadge', () => ({ StatusBadge: ({ value }: { value: string }) => <span>{value}</span> }));
vi.mock('../search/useTypeahead', () => ({
  useTypeahead: (q: string) => (q.trim().length >= 2 ? { accounts: [{ id: 'a1', name: 'Acme Holding', sector: 'FMCG', city: 'Izmir', status: 'ACTIVE' }], people: [], jobOrders: [] } : null),
}));

const auth = { user: { id: 'u1', name: 'U' }, status: 'authed', isManager: true, isAdmin: false, login: async () => 'ok', logout: async () => {} } as unknown as AuthCtx;

function Where() {
  const l = useLocation();
  return <div data-testid="where">{l.pathname + l.search}</div>;
}

function setup() {
  const onClose = vi.fn();
  render(
    <MantineProvider env="test">
      <AuthContext.Provider value={auth}>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route path="*" element={<Where />} />
          </Routes>
          <CommandPalette opened onClose={onClose} onChanged={() => {}} />
        </MemoryRouter>
      </AuthContext.Provider>
    </MantineProvider>,
  );
  return { onClose, input: screen.getByRole('combobox') };
}

beforeAll(() => i18n.changeLanguage('en'));
beforeEach(() => {
  localStorage.clear();
});

describe('CommandPalette', () => {
  it('exposes combobox + listbox semantics and moves the active option with arrows', () => {
    const { input } = setup();
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    const options = screen.getAllByRole('option');
    expect(options.length).toBeGreaterThan(3);
    expect(input).toHaveAttribute('aria-activedescendant', options[0]!.id);
    expect(options[0]).toHaveAttribute('aria-selected', 'true');
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(input).toHaveAttribute('aria-activedescendant', options[1]!.id);
    fireEvent.keyDown(input, { key: 'ArrowUp' });
    fireEvent.keyDown(input, { key: 'ArrowUp' });
    expect(input).toHaveAttribute('aria-activedescendant', options[options.length - 1]!.id);
  });

  it('turns operators into chips and Enter opens the list with URL filters', () => {
    const { input, onClose } = setup();
    fireEvent.change(input, { target: { value: 'owner:me stale:90 type:exec' } });
    expect(screen.getByText('Owner: me')).toBeInTheDocument();
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(screen.getByTestId('where')).toHaveTextContent('/network?owner=me&stale=90&type=EXECUTIVE');
    expect(onClose).toHaveBeenCalled();
  });

  it('removes a chip from the input', () => {
    const { input } = setup();
    fireEvent.change(input, { target: { value: 'owner:me stale:90' } });
    fireEvent.click(screen.getByRole('button', { name: /Remove filter Owner me/ }));
    expect(input).toHaveValue('stale:90');
  });

  it('shows grouped typeahead results and opens one with Enter, recording it as recent', () => {
    const { input } = setup();
    fireEvent.change(input, { target: { value: 'acme' } });
    expect(screen.getByRole('option', { name: /Acme Holding/ })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Clients' })).toBeInTheDocument();
    fireEvent.mouseEnter(screen.getByRole('option', { name: /Acme Holding/ }));
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(screen.getByTestId('where')).toHaveTextContent('/accounts/a1');
    // KVKK: only kind and id are stored, under a per-user key
    const raw = localStorage.getItem('command.recent.u1')!;
    expect(JSON.parse(raw)[0]).toMatchObject({ kind: 'account', id: 'a1' });
    expect(raw).not.toContain('Acme');
  });

  it('runs a navigation quick action', () => {
    const { input } = setup();
    fireEvent.change(input, { target: { value: 'network' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(screen.getByTestId('where')).toHaveTextContent('/network');
  });
});




function Harness() {
  const [open, setOpen] = useState(false);
  const toggle = useCallback(() => setOpen((o) => !o), []);
  useCommandHotkey(toggle);
  return (
    <>
      <CommandButton onClick={() => setOpen(true)} />
      <CommandPalette opened={open} onClose={() => setOpen(false)} onChanged={() => {}} />
    </>
  );
}

function renderHarness() {
  return render(
    <MantineProvider env="test">
      <AuthContext.Provider value={auth}>
        <MemoryRouter>
          <Harness />
        </MemoryRouter>
      </AuthContext.Provider>
    </MantineProvider>,
  );
}

describe('command bar entry points', () => {
  it('opens and closes with Ctrl+K and Cmd+K', async () => {
    renderHarness();
    expect(screen.queryByRole('dialog')).toBeNull();
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
    expect(await screen.findByRole('dialog', { name: 'Command bar' })).toBeInTheDocument();
    fireEvent.keyDown(window, { key: 'K', metaKey: true });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    fireEvent.keyDown(window, { key: 'k' });
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('returns focus to the trigger when closed with Escape', async () => {
    renderHarness();
    const btn = screen.getByRole('button', { name: /Open command bar/ });
    btn.focus();
    fireEvent.click(btn);
    const dialog = await screen.findByRole('dialog');
    fireEvent.keyDown(dialog, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    await waitFor(() => expect(btn).toHaveFocus());
  });
});
