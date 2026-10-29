import { TextInput } from '@mantine/core';
import { act, fireEvent, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { EditDrawer, type EditDrawerProps } from './EditDrawer';
import { axeViolations, renderShell } from './testUtils';

const base = (p: Partial<EditDrawerProps> = {}): EditDrawerProps => ({
  opened: true,
  onClose: vi.fn(),
  title: 'Acme',
  dirty: false,
  onSave: vi.fn().mockResolvedValue(undefined),
  sections: [
    { id: 'main', title: 'Main', content: <TextInput label="Name" defaultValue="Acme" /> },
    { id: 'contact', title: 'Contact', content: <TextInput label="City" /> },
  ],
  ...p,
});

describe('EditDrawer', () => {
  it('renders a labelled dialog with section headings', () => {
    renderShell(<EditDrawer {...base()} />);
    const dlg = screen.getByRole('dialog', { name: 'Acme' });
    expect(within(dlg).getByRole('heading', { level: 3, name: 'Main' })).toBeInTheDocument();
    expect(within(dlg).getByRole('heading', { level: 3, name: 'Contact' })).toBeInTheDocument();
  });

  it('closes straight away when clean, asks first when dirty', () => {
    const clean = base();
    const { unmount } = renderShell(<EditDrawer {...clean} />);
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(clean.onClose).toHaveBeenCalled();
    unmount();

    const dirty = base({ dirty: true });
    renderShell(<EditDrawer {...dirty} />);
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(dirty.onClose).not.toHaveBeenCalled();
    const guard = screen.getByRole('dialog', { name: 'Discard unsaved changes?' });
    fireEvent.click(within(guard).getByRole('button', { name: 'Keep editing' }));
    expect(dirty.onClose).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    fireEvent.click(screen.getByRole('button', { name: 'Discard changes' }));
    expect(dirty.onClose).toHaveBeenCalledTimes(1);
  });

  it('saves with the button and with Ctrl+Enter, then closes', async () => {
    const p = base({ dirty: true });
    renderShell(<EditDrawer {...p} />);
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await act(async () => {});
    expect(p.onSave).toHaveBeenCalledTimes(1);
    expect(p.onClose).toHaveBeenCalledTimes(1);
    fireEvent.keyDown(screen.getByLabelText('Name'), { key: 'Enter', ctrlKey: true });
    await act(async () => {});
    expect(p.onSave).toHaveBeenCalledTimes(2);
  });

  it('keeps the drawer open and announces a failed save', async () => {
    const p = base({ dirty: true, onSave: () => Promise.reject({ detail: 'Server said no' }) });
    renderShell(<EditDrawer {...p} />);
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await act(async () => {});
    expect(screen.getByRole('alert')).toHaveTextContent('Server said no');
    expect(p.onClose).not.toHaveBeenCalled();
  });

  it('opens Compare and save on a 409 and resolves with the chosen side', async () => {
    const onResolveConflict = vi.fn().mockResolvedValue(undefined);
    const p = base({
      dirty: true,
      onSave: async () => ({ conflict: [{ key: 'city', label: 'City', mine: 'Izmir', theirs: 'Ankara' }] }),
      onResolveConflict,
    });
    renderShell(<EditDrawer {...p} />);
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await act(async () => {});
    const dlg = screen.getByRole('dialog', { name: 'Compare and save' });
    expect(within(dlg).getByRole('rowheader', { name: 'City' })).toBeInTheDocument();
    expect(within(dlg).getByText('Izmir')).toBeInTheDocument();
    expect(within(dlg).getByText('Ankara')).toBeInTheDocument();
    fireEvent.click(within(dlg).getByRole('button', { name: 'Keep my changes and save' }));
    await act(async () => {});
    expect(onResolveConflict).toHaveBeenCalledWith('mine');
    expect(p.onClose).toHaveBeenCalled();
  });

  it('is axe clean, also with the guard open', async () => {
    renderShell(<EditDrawer {...base({ dirty: true })} />);
    expect(await axeViolations()).toEqual([]);
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(await axeViolations()).toEqual([]);
  });
});
