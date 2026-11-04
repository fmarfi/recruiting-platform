import { act, fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderShell } from '../../../shell/edit/testUtils';

const PATCH = vi.fn();
const GET = vi.fn();
vi.mock('../../../api', () => ({ api: { PATCH: (...a: unknown[]) => PATCH(...a), GET: (...a: unknown[]) => GET(...a) } }));

import { JobOrderEditDrawer } from './JobOrderEditDrawer';

const job = {
  id: 'j1', title: 'CFO', location: 'Istanbul', seniority: null, description: null, feeTerms: null, openings: 1, openedOn: '2026-09-01',
  owner: { id: 'u1', name: 'Deniz' }, account: { id: 'a1', name: 'Acme', status: 'ACTIVE' }, status: 'OPEN', version: 7,
} as never;

function setup() {
  const onSaved = vi.fn();
  const onClose = vi.fn();
  renderShell(<JobOrderEditDrawer job={job} opened onClose={onClose} onSaved={onSaved} />);
  return { onSaved, onClose };
}
const type = (label: RegExp, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });

describe('JobOrderEditDrawer', () => {
  beforeEach(() => {
    // jsdom has no document.fonts, which Mantine's autosize textarea listens to.
    Object.defineProperty(document, 'fonts', { value: { addEventListener() {}, removeEventListener() {} }, configurable: true });
    PATCH.mockReset();
    GET.mockReset();
    GET.mockResolvedValue({ data: { content: [{ id: 'u1', name: 'Deniz' }] } });
  });

  it('edits description and fee terms, then undo sends the old values back', async () => {
    GET.mockImplementation((path: string) => Promise.resolve(path === '/users' ? { data: { content: [{ id: 'u1', name: 'Deniz' }] } } : { data: { ...(job as object), description: 'Lead finance team', feeTerms: '33% retained', version: 8 } }));
    PATCH.mockResolvedValueOnce({ data: { ...(job as object), version: 8 } }).mockResolvedValueOnce({ data: { version: 9 } });
    const { onSaved } = setup();
    type(/Description/, 'Lead finance team');
    type(/Fee terms/, '33% retained');
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await act(async () => {});
    expect(PATCH).toHaveBeenCalledWith('/job-orders/{id}', { params: { path: { id: 'j1' } }, body: { description: 'Lead finance team', feeTerms: '33% retained', version: 7 } });
    expect(onSaved).toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: /Undo/ }));
    await act(async () => {});
    expect(PATCH).toHaveBeenLastCalledWith('/job-orders/{id}', { params: { path: { id: 'j1' } }, body: { description: null, feeTerms: null, version: 8 } });
  });

  it('shows the field error inline and does not close', async () => {
    PATCH.mockResolvedValueOnce({ error: { code: 'VALIDATION_FAILED', errors: [{ field: 'title', code: 'SIZE', message: 'size must be between 1 and 200' }] }, response: { status: 400 } });
    const { onClose } = setup();
    type(/Job title/, 'x');
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await act(async () => {});
    expect(screen.getByText('size must be between 1 and 200')).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });
});
