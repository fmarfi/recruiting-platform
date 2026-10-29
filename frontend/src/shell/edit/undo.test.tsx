import { act, fireEvent, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { axeViolations, renderShell } from './testUtils';
import { UNDO_MS, useUndoToast } from './undo';

function Trigger({ onUndo }: { onUndo: () => void }) {
  const show = useUndoToast();
  return (
    <>
      <button onClick={() => show({ message: 'Owner changed', onUndo })}>go</button>
      <input aria-label="note" />
    </>
  );
}

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe('useUndoToast', () => {
  it('shows a status toast that disappears after 6 s', () => {
    renderShell(<Trigger onUndo={() => {}} />);
    fireEvent.click(screen.getByText('go'));
    expect(screen.getByRole('status')).toHaveTextContent('Owner changed');
    act(() => void vi.advanceTimersByTime(UNDO_MS - 100));
    expect(screen.getByText('Owner changed')).toBeInTheDocument();
    act(() => void vi.advanceTimersByTime(200));
    expect(screen.queryByText('Owner changed')).toBeNull();
  });

  it('pauses while focused and resumes on blur', () => {
    renderShell(<Trigger onUndo={() => {}} />);
    fireEvent.click(screen.getByText('go'));
    const undo = screen.getByRole('button', { name: /Undo/ });
    fireEvent.focus(undo);
    act(() => void vi.advanceTimersByTime(UNDO_MS * 3));
    expect(screen.getByText('Owner changed')).toBeInTheDocument();
    fireEvent.blur(undo);
    act(() => void vi.advanceTimersByTime(UNDO_MS + 100));
    expect(screen.queryByText('Owner changed')).toBeNull();
  });

  it('undoes with the button and with Ctrl+Z, but not inside a text field', async () => {
    const onUndo = vi.fn();
    renderShell(<Trigger onUndo={onUndo} />);
    fireEvent.click(screen.getByText('go'));
    fireEvent.keyDown(screen.getByLabelText('note'), { key: 'z', ctrlKey: true });
    expect(onUndo).not.toHaveBeenCalled();
    fireEvent.keyDown(document.body, { key: 'z', ctrlKey: true });
    await act(async () => {});
    expect(onUndo).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('status')).toHaveTextContent('Undone');
  });

  it('reports a failed undo as an alert', async () => {
    renderShell(<Trigger onUndo={() => Promise.reject(new Error('x')) as never} />);
    fireEvent.click(screen.getByText('go'));
    fireEvent.click(screen.getByRole('button', { name: /Undo/ }));
    await act(async () => {});
    expect(screen.getByRole('alert')).toHaveTextContent('Could not undo');
  });

  it('is axe clean', async () => {
    vi.useRealTimers();
    renderShell(<Trigger onUndo={() => {}} />);
    fireEvent.click(screen.getByText('go'));
    expect(await axeViolations()).toEqual([]);
  });
});
