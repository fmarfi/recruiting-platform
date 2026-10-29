import { act, fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderShell } from '../../shell/edit/testUtils';

const PATCH = vi.fn();
const GET = vi.fn();
vi.mock('../../api', () => ({ api: { PATCH: (...a: unknown[]) => PATCH(...a), GET: (...a: unknown[]) => GET(...a) } }));

import { AccountEditDrawer } from './AccountEditDrawer';

const account = {
  id: 'a1', name: 'Acme', sector: 'FMCG', city: 'Ankara', website: null, notes: null, owner: { id: 'u1', name: 'Deniz' },
  status: 'ACTIVE', statusChangedAt: '2026-01-01T00:00:00Z', openedOn: '2026-01-01', openJobCount: 0, createdAt: '', updatedAt: '', version: 3,
} as never;

function setup() {
  const onSaved = vi.fn();
  const onClose = vi.fn();
  renderShell(<AccountEditDrawer account={account} opened onClose={onClose} onSaved={onSaved} />);
  return { onSaved, onClose };
}
const type = (label: RegExp, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });

describe('AccountEditDrawer', () => {
  beforeEach(() => {
    // jsdom has no document.fonts, which Mantine's autosize textarea listens to.
    Object.defineProperty(document, 'fonts', { value: { addEventListener() {}, removeEventListener() {} }, configurable: true });
    PATCH.mockReset();
    GET.mockReset();
    GET.mockResolvedValue({ data: { content: [{ id: 'u1', name: 'Deniz' }] } });
  });

  it('shows every writable field and saves only the changed ones, then undo restores the old value', async () => {
    GET.mockImplementation((path: string) => Promise.resolve(path === '/users' ? { data: { content: [{ id: 'u1', name: 'Deniz' }] } } : { data: { ...(account as object), name: 'Acme Foods', version: 4 } }));
    PATCH.mockResolvedValueOnce({ data: { ...(account as object), name: 'Acme Foods', version: 4 } }).mockResolvedValueOnce({ data: { version: 5 } });
    const { onSaved, onClose } = setup();
    for (const l of [/Client name/, /Sector/, /City/, /Website/, /Notes/, /Owner/]) expect(screen.getByLabelText(l)).toBeInTheDocument();
    type(/Client name/, 'Acme Foods');
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await act(async () => {});
    expect(PATCH).toHaveBeenCalledWith('/accounts/{id}', { params: { path: { id: 'a1' } }, body: { name: 'Acme Foods', version: 3 } });
    expect(onSaved).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: /Undo/ }));
    await act(async () => {});
    expect(PATCH).toHaveBeenLastCalledWith('/accounts/{id}', { params: { path: { id: 'a1' } }, body: { name: 'Acme', version: 4 } });
  });

  it('opens the compare dialog on a version conflict and re-saves on top of the new version', async () => {
    GET.mockImplementation((path: string) => Promise.resolve(path === '/users' ? { data: { content: [{ id: 'u1', name: 'Deniz' }] } } : { data: { ...(account as object), name: 'Acme Theirs', version: 9 } }));
    PATCH.mockResolvedValueOnce({ error: { code: 'VERSION_CONFLICT' }, response: { status: 409 } }).mockResolvedValueOnce({ data: { version: 10 } });
    const { onSaved } = setup();
    type(/Client name/, 'Acme Mine');
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await act(async () => {});
    expect(screen.getByRole('dialog', { name: 'Compare and save' })).toBeInTheDocument();
    expect(screen.getByText('Acme Theirs')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Keep my changes and save' }));
    await act(async () => {});
    expect(PATCH).toHaveBeenLastCalledWith('/accounts/{id}', { params: { path: { id: 'a1' } }, body: { name: 'Acme Mine', version: 9 } });
    expect(onSaved).toHaveBeenCalled();
  });

  it('shows the API field error next to the field and keeps the drawer open', async () => {
    PATCH.mockResolvedValueOnce({ error: { code: 'VALIDATION_FAILED', errors: [{ field: 'website', code: 'URL', message: 'must be a valid URL' }] }, response: { status: 400 } });
    const { onClose } = setup();
    type(/Website/, 'nope');
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await act(async () => {});
    expect(screen.getByText('must be a valid URL')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });
});
