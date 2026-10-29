import { act, fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { InlineEditField } from './InlineEditField';
import { axeViolations, renderShell } from './testUtils';

const open = () => fireEvent.click(screen.getByRole('button', { name: 'Edit Owner' }));
const input = () => screen.getByLabelText('Owner');

describe('InlineEditField', () => {
  it('saves with Enter, shows the new value and returns focus to the pencil', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    renderShell(<InlineEditField label="Owner" value="Can" onSave={onSave} />);
    open();
    fireEvent.change(input(), { target: { value: 'Deniz' } });
    fireEvent.keyDown(input(), { key: 'Enter' });
    await act(async () => {});
    expect(onSave).toHaveBeenCalledWith('Deniz');
    expect(screen.getByText('Deniz')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Edit Owner' })).toHaveFocus();
  });

  it('cancels with Escape without saving', () => {
    const onSave = vi.fn();
    renderShell(<InlineEditField label="Owner" value="Can" onSave={onSave} />);
    open();
    fireEvent.change(input(), { target: { value: 'x' } });
    fireEvent.keyDown(input(), { key: 'Escape' });
    expect(onSave).not.toHaveBeenCalled();
    expect(screen.getByText('Can')).toBeInTheDocument();
  });

  it('rolls back and announces the error when saving fails', async () => {
    renderShell(<InlineEditField label="Owner" value="Can" onSave={() => Promise.reject({ detail: 'Nope' })} />);
    open();
    fireEvent.change(input(), { target: { value: 'Deniz' } });
    fireEvent.keyDown(input(), { key: 'Enter' });
    await act(async () => {});
    expect(screen.getByText('Nope')).toBeInTheDocument();
    expect(input()).toHaveValue('Deniz');
  });

  it('blocks invalid values and offers undo after a save', async () => {
    const onUndo = vi.fn().mockResolvedValue(undefined);
    renderShell(<InlineEditField label="Owner" value="Can" onSave={async () => {}} onUndo={onUndo} validate={(v) => (v.length < 2 ? 'Too short' : null)} />);
    open();
    fireEvent.change(input(), { target: { value: 'D' } });
    fireEvent.keyDown(input(), { key: 'Enter' });
    expect(screen.getByText('Too short')).toBeInTheDocument();
    fireEvent.change(input(), { target: { value: 'Deniz' } });
    fireEvent.keyDown(input(), { key: 'Enter' });
    await act(async () => {});
    fireEvent.click(screen.getByRole('button', { name: /Undo/ }));
    await act(async () => {});
    expect(onUndo).toHaveBeenCalledWith('Can');
    expect(screen.getByText('Can')).toBeInTheDocument();
  });

  it('is axe clean in read and edit mode', async () => {
    renderShell(<InlineEditField label="Owner" value="Can" onSave={async () => {}} />);
    expect(await axeViolations()).toEqual([]);
    open();
    expect(await axeViolations()).toEqual([]);
  });
});
