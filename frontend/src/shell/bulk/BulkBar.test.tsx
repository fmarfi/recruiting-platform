import { act, fireEvent, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { axeViolations, renderShell } from '../edit/testUtils';
import { BulkBar, type BulkActionResult } from './BulkBar';

const result = (over: Partial<BulkActionResult> = {}): BulkActionResult => ({
  id: 'b1',
  entity: 'PEOPLE',
  action: 'ADD_TAG',
  requested: 3,
  ok: 2,
  skipped: 1,
  failed: 0,
  results: [
    { id: 'a', outcome: 'OK', code: null, message: null },
    { id: 'b', outcome: 'OK', code: null, message: null },
    { id: 'c', outcome: 'SKIPPED', code: 'NO_CHANGE', message: 'x' },
  ],
  ...over,
});

const setup = (res: BulkActionResult | null = result()) => {
  const POST = vi.fn().mockResolvedValue(res ? { data: res } : { error: {} });
  const clear = vi.fn();
  const utils = renderShell(<BulkBar entity="PEOPLE" selectedIds={['a', 'b', 'c']} clear={clear} client={{ POST } as never} />);
  return { POST, clear, ...utils };
};

const pick = (name: string) => {
  fireEvent.click(screen.getByRole('button', { name: /Actions/ }));
  fireEvent.click(screen.getByRole('menuitem', { name }));
};

describe('BulkBar', () => {
  it('posts the tag action with the selected ids and summarises partial success', async () => {
    const { POST, clear } = setup();
    pick('Add tag');
    fireEvent.change(screen.getByLabelText(/Tag/), { target: { value: ' cfo ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));
    await act(async () => {});
    expect(POST).toHaveBeenCalledWith('/bulk-actions', { body: { action: 'ADD_TAG', entity: 'PEOPLE', ids: ['a', 'b', 'c'], params: { tag: 'cfo' } } });
    expect(screen.getAllByRole('status')[0]).toHaveTextContent('2 done, 1 skipped, 0 failed');
    expect(clear).not.toHaveBeenCalled();
  });

  it('clears the selection when everything succeeded and undoes a tag action with the inverse call', async () => {
    const { POST, clear } = setup(result({ skipped: 0, ok: 2, results: result().results.slice(0, 2) }));
    pick('Add tag');
    fireEvent.change(screen.getByLabelText(/Tag/), { target: { value: 'cfo' } });
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));
    await act(async () => {});
    expect(clear).toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: /Undo/ }));
    await act(async () => {});
    expect(POST).toHaveBeenLastCalledWith('/bulk-actions', { body: { action: 'REMOVE_TAG', entity: 'PEOPLE', ids: ['a', 'b'], params: { tag: 'cfo' } } });
  });

  it('runs consent requests directly and lists skip reasons', async () => {
    const { POST } = setup(result({ action: 'REQUEST_CONSENT' }));
    pick('Request consent');
    await act(async () => {});
    expect(POST).toHaveBeenCalledWith('/bulk-actions', { body: { action: 'REQUEST_CONSENT', entity: 'PEOPLE', ids: ['a', 'b', 'c'], params: {} } });
    fireEvent.click(screen.getByRole('button', { name: 'Details' }));
    expect(within(screen.getByRole('list')).getByText('1 x No change needed')).toBeInTheDocument();
  });

  it('keeps Apply disabled until required fields are filled and reports request errors', async () => {
    setup(null);
    pick('Set next action');
    expect(screen.getByRole('button', { name: 'Apply' })).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/Next action/), { target: { value: 'Call' } });
    fireEvent.change(screen.getByLabelText(/Due date/), { target: { value: '2026-11-01' } });
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));
    await act(async () => {});
    expect(screen.getByRole('alert')).toHaveTextContent('Bulk action failed');
  });

  it('is axe clean with the dialog open', async () => {
    setup();
    expect(await axeViolations()).toEqual([]);
    pick('Change owner');
    expect(await axeViolations(document.body, ['aria-required-children'])).toEqual([]);
  });
});
