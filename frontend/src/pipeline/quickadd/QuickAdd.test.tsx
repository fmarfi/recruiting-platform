import { MantineProvider } from '@mantine/core';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import i18n from '../../i18n';

const post = vi.fn();
vi.mock('../../api', () => ({ api: { POST: (...a: unknown[]) => post(...a), GET: () => Promise.resolve({ data: { content: [] } }) } }));

import { QuickAdd } from './QuickAdd';

function setup(props: Partial<React.ComponentProps<typeof QuickAdd>> = {}) {
  const onClose = vi.fn();
  const onSaved = vi.fn();
  render(
    <MantineProvider>
      <QuickAdd opened onClose={onClose} onSaved={onSaved} context={{ account: { id: 'a1', name: 'Acme' } }} {...props} />
    </MantineProvider>,
  );
  return { onClose, onSaved };
}

describe('QuickAdd', () => {
  beforeEach(async () => {
    // jsdom has no document.fonts, which Mantine's autosize textarea listens to.
    Object.defineProperty(document, 'fonts', { value: { addEventListener() {}, removeEventListener() {} }, configurable: true });
    post.mockReset();
    post.mockResolvedValue({ data: {} });
    await i18n.changeLanguage('en');
  });

  it('keeps Save disabled until a note is typed, then logs the activity with the account context', async () => {
    const { onSaved, onClose } = setup();
    const save = screen.getByRole('button', { name: 'Save' });
    expect(save).toBeDisabled();
    fireEvent.change(screen.getByRole('textbox', { name: 'Note' }), { target: { value: 'Called the CFO' } });
    expect(save).toBeEnabled();
    fireEvent.click(save);
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
    expect(onClose).toHaveBeenCalled();
    expect(post).toHaveBeenCalledTimes(1);
    expect(post.mock.calls[0]![0]).toBe('/activities');
    expect(post.mock.calls[0]![1].body).toMatchObject({ type: 'CALL', note: 'Called the CFO', accountId: 'a1' });
  });

  it('creates a next action in action mode and needs a due date', async () => {
    const { onSaved } = setup({ initialMode: 'action' });
    fireEvent.change(screen.getByRole('textbox', { name: 'Action' }), { target: { value: 'Send proposal' } });
    const save = screen.getByRole('button', { name: 'Save' });
    expect(save).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Due date'), { target: { value: '2026-11-01' } });
    fireEvent.click(save);
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
    expect(post.mock.calls[0]![0]).toBe('/next-actions');
    expect(post.mock.calls[0]![1].body).toMatchObject({ title: 'Send proposal', dueOn: '2026-11-01', accountId: 'a1' });
  });

  it('shows the server error and stays open when saving fails', async () => {
    post.mockResolvedValue({ error: { title: 'Boom' } });
    const { onClose, onSaved } = setup();
    fireEvent.change(screen.getByRole('textbox', { name: 'Note' }), { target: { value: 'x' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(onSaved).not.toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('asks for an account or person when there is no context', () => {
    setup({ context: undefined });
    expect(screen.getByText('Pick an account or a person.')).toBeInTheDocument();
  });
});
