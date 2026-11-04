import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderShell } from '../edit/testUtils';
import { QuickLog } from './QuickLog';
import { contextFromPath } from './QuickLogHost';
import './text';

const get = vi.fn();
const post = vi.fn();
const del = vi.fn();
vi.mock('../../api', () => ({
  api: { GET: (...a: unknown[]) => get(...a), POST: (...a: unknown[]) => post(...a), DELETE: (...a: unknown[]) => del(...a) },
}));

function setup(props: Partial<React.ComponentProps<typeof QuickLog>> = {}) {
  const onClose = vi.fn();
  const onDone = vi.fn();
  renderShell(
    <MemoryRouter>
      <QuickLog opened onClose={onClose} onDone={onDone} {...props} />
    </MemoryRouter>,
  );
  return { onClose, onDone };
}

beforeEach(() => {
  localStorage.clear();
  get.mockReset().mockImplementation((path: string) =>
    Promise.resolve(
      path === '/people/{id}'
        ? { data: { id: 'p1', name: 'Deniz Kaya' } }
        : { data: { id: 'j1', title: 'CFO search', account: { id: 'a1', name: 'Acme' } } },
    ),
  );
  post.mockReset().mockResolvedValue({ data: { id: 'act1' } });
  del.mockReset().mockResolvedValue({});
});

describe('QuickLog', () => {
  it('prefills the person from the page and defaults to a call', async () => {
    setup({ personId: 'p1' });
    await waitFor(() => expect(screen.getByDisplayValue('Deniz Kaya')).toBeInTheDocument());
    expect(screen.getByRole('radio', { name: 'Call' })).toBeChecked();
  });

  it('prefills the search and takes its client', async () => {
    setup({ personId: 'p1', jobOrderId: 'j1' });
    await waitFor(() => expect(screen.getByText('CFO search')).toBeInTheDocument());
    fireEvent.keyDown(screen.getByRole('textbox', { name: /^Note/ }), { key: 'Enter' });
    await waitFor(() => expect(post).toHaveBeenCalled());
    expect(post.mock.calls[0]![1].body).toMatchObject({ personId: 'p1', accountId: 'a1', jobOrderId: 'j1' });
  });

  it('saves with the keyboard only: outcome chip, Enter in the note', async () => {
    const { onClose, onDone } = setup({ personId: 'p1' });
    await waitFor(() => expect(screen.getByDisplayValue('Deniz Kaya')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('radio', { name: 'No answer' }));
    const note = screen.getByRole('textbox', { name: /^Note/ });
    fireEvent.change(note, { target: { value: 'Try again Friday' } });
    fireEvent.keyDown(note, { key: 'Enter' });
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post.mock.calls[0]![0]).toBe('/activities');
    expect(post.mock.calls[0]![1].body).toMatchObject({ type: 'CALL', note: 'Try again Friday', personId: 'p1', outcome: 'NO_ANSWER', direction: 'OUTBOUND' });
    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(onDone).toHaveBeenCalled();
    expect(screen.getByRole('status')).toHaveTextContent('Activity logged');
  });

  it('makes a note from type and outcome when the note is empty', async () => {
    setup({ personId: 'p1' });
    await waitFor(() => expect(screen.getByDisplayValue('Deniz Kaya')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('radio', { name: 'Connected' }));
    fireEvent.keyDown(screen.getByRole('textbox', { name: /^Note/ }), { key: 'Enter' });
    await waitFor(() => expect(post).toHaveBeenCalled());
    expect(post.mock.calls[0]![1].body.note).toBe('Call: Connected');
  });

  it('does not save on Shift+Enter and shows a validation error without a person or client', async () => {
    setup();
    const note = screen.getByRole('textbox', { name: /^Note/ });
    fireEvent.keyDown(note, { key: 'Enter', shiftKey: true });
    expect(post).not.toHaveBeenCalled();
    fireEvent.keyDown(note, { key: 'Enter' });
    expect(await screen.findByText('Pick a person or a client.')).toBeInTheDocument();
    expect(post).not.toHaveBeenCalled();
  });

  it('shows the server error and keeps the composer open', async () => {
    post.mockResolvedValue({ error: { title: 'Nope', status: 400 } });
    const { onClose } = setup({ personId: 'p1' });
    await waitFor(() => expect(screen.getByDisplayValue('Deniz Kaya')).toBeInTheDocument());
    fireEvent.keyDown(screen.getByRole('textbox', { name: /^Note/ }), { key: 'Enter' });
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('undo deletes the activity again', async () => {
    const { onDone } = setup({ personId: 'p1' });
    await waitFor(() => expect(screen.getByDisplayValue('Deniz Kaya')).toBeInTheDocument());
    fireEvent.keyDown(screen.getByRole('textbox', { name: /^Note/ }), { key: 'Enter' });
    const undo = await screen.findByRole('button', { name: /Undo/ });
    onDone.mockClear();
    await act(async () => {
      fireEvent.click(undo);
    });
    expect(del).toHaveBeenCalledWith('/activities/{id}', { params: { path: { id: 'act1' } } });
    expect(onDone).toHaveBeenCalled();
  });
});

describe('contextFromPath', () => {
  it('reads the person, client or search from the route', () => {
    expect(contextFromPath('/candidates/p9')).toEqual({ personId: 'p9' });
    expect(contextFromPath('/clients/a9')).toEqual({ accountId: 'a9' });
    expect(contextFromPath('/searches/j9')).toEqual({ jobOrderId: 'j9' });
    expect(contextFromPath('/')).toEqual({});
  });
});
