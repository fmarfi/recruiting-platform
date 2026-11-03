import { act, fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderShell } from '../../shell/edit/testUtils';

const PATCH = vi.fn();
const GET = vi.fn();
const POST = vi.fn();
vi.mock('../../api', () => ({ api: { PATCH: (...a: unknown[]) => PATCH(...a), GET: (...a: unknown[]) => GET(...a), POST: (...a: unknown[]) => POST(...a) } }));

import { PersonEditDrawer } from './PersonEditDrawer';

const person = (over: object = {}) =>
  ({
    id: 'p1', name: 'Elif Yildirim', type: 'CANDIDATE', title: 'CFO', company: 'Marmara', city: 'Ankara', email: 'e@example.com', phone: null, linkedinUrl: null, howWeKnow: null, notes: null,
    owner: { id: 'u1', name: 'Deniz' }, tags: ['cfo-network'], employments: [], languages: [], educations: [], compensation: { currency: 'TRY', period: 'ANNUAL', basis: 'GROSS', currentBase: 100 },
    compensationRestricted: false, anonymised: false, consentStatus: 'GIVEN', version: 2, ...over,
  }) as never;

function setup(p = person()) {
  const onSaved = vi.fn();
  const onClose = vi.fn();
  renderShell(<PersonEditDrawer person={p} opened onClose={onClose} onSaved={onSaved} />);
  return { onSaved, onClose };
}
const type = (label: RegExp, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });

describe('PersonEditDrawer', () => {
  beforeEach(() => {
    // jsdom has no document.fonts, which Mantine's autosize textarea listens to.
    Object.defineProperty(document, 'fonts', { value: { addEventListener() {}, removeEventListener() {} }, configurable: true });
    PATCH.mockReset();
    GET.mockReset();
    POST.mockReset();
    GET.mockResolvedValue({ data: { content: [{ id: 'u1', name: 'Deniz' }] } });
    POST.mockResolvedValue({ data: { results: [{ id: 'p1', outcome: 'OK', code: 'OK', message: '' }] } });
  });

  it('saves changed fields, adds a tag through a bulk action, and undo reverts both', async () => {
    // Each tag call bumps the version, so the undo must read it again: the person is at version 5 by then.
    GET.mockImplementation((path: string) => Promise.resolve(path === '/users' ? { data: { content: [{ id: 'u1', name: 'Deniz' }] } } : { data: { ...(person() as object), phone: '+90 555 000 00 00', version: 5 } }));
    PATCH.mockResolvedValueOnce({ data: { ...(person() as object), version: 3 } }).mockResolvedValueOnce({ data: { version: 6 } });
    const { onSaved } = setup();
    type(/^Phone/, '+90 555 000 00 00');
    const tags = screen.getByLabelText(/Tags/);
    fireEvent.change(tags, { target: { value: 'finance' } });
    fireEvent.keyDown(tags, { key: 'Enter' });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await act(async () => {});
    expect(PATCH).toHaveBeenCalledWith('/people/{id}', { params: { path: { id: 'p1' } }, body: { phone: '+90 555 000 00 00', version: 2 } });
    expect(POST).toHaveBeenCalledWith('/bulk-actions', { body: { entity: 'PEOPLE', ids: ['p1'], action: 'ADD_TAG', params: { tag: 'finance' } } });
    expect(onSaved).toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: /Undo/ }));
    await act(async () => {});
    expect(PATCH).toHaveBeenLastCalledWith('/people/{id}', { params: { path: { id: 'p1' } }, body: { phone: null, version: 5 } });
    expect(POST).toHaveBeenLastCalledWith('/bulk-actions', { body: { entity: 'PEOPLE', ids: ['p1'], action: 'REMOVE_TAG', params: { tag: 'finance' } } });
  });

  it('shows an inline error from the API and stays open', async () => {
    PATCH.mockResolvedValueOnce({ error: { code: 'VALIDATION_FAILED', errors: [{ field: 'email', code: 'EMAIL', message: 'must be a well-formed email address' }] }, response: { status: 400 } });
    const { onClose } = setup();
    type(/^Email/, 'bad');
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await act(async () => {});
    expect(screen.getByText('must be a well-formed email address')).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('hides compensation when the server restricted it', () => {
    setup(person({ compensation: null, compensationRestricted: true }));
    expect(screen.queryByRole('heading', { name: 'Compensation' })).toBeNull();
    expect(screen.queryByLabelText(/^Base/)).toBeNull();
  });
});
