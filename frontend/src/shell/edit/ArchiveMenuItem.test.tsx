import { act, fireEvent, screen } from '@testing-library/react';
import { Menu } from '@mantine/core';
import { describe, expect, it, vi } from 'vitest';
import { ArchiveMenuItem, type ArchiveMenuItemProps } from './ArchiveMenuItem';
import { axeViolations, renderShell } from './testUtils';

const host = (p: Partial<ArchiveMenuItemProps>) => (
  <Menu opened withinPortal={false}>
    <Menu.Target>
      <button>more</button>
    </Menu.Target>
    <Menu.Dropdown>
      <ArchiveMenuItem name="Acme" onArchive={async () => {}} {...p} />
      <Menu.Item>Other</Menu.Item>
    </Menu.Dropdown>
  </Menu>
);

describe('ArchiveMenuItem', () => {
  it('is hidden by default', () => {
    renderShell(host({}));
    expect(screen.queryByText('Archive')).toBeNull();
  });

  it('can be shown disabled with a hint', () => {
    const onArchive = vi.fn();
    renderShell(host({ mode: 'disabled', onArchive }));
    const item = screen.getByRole('menuitem', { name: 'Archive' });
    expect(item).toBeDisabled();
    fireEvent.click(item);
    expect(onArchive).not.toHaveBeenCalled();
  });

  it('archives and offers undo to restore', async () => {
    const onArchive = vi.fn().mockResolvedValue(undefined);
    const onRestore = vi.fn().mockResolvedValue(undefined);
    renderShell(host({ mode: 'enabled', onArchive, onRestore }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Archive' }));
    await act(async () => {});
    expect(onArchive).toHaveBeenCalled();
    expect(screen.getByText('Acme archived')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Undo/ }));
    await act(async () => {});
    expect(onRestore).toHaveBeenCalled();
  });

  it('shows an error when archiving fails', async () => {
    renderShell(host({ mode: 'enabled', onArchive: () => Promise.reject(new Error('x')) }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Archive' }));
    await act(async () => {});
    expect(screen.getByText('Could not archive. Try again.')).toBeInTheDocument();
  });

  it('is axe clean', async () => {
    renderShell(host({ mode: 'enabled' }));
    expect(await axeViolations(document.body, ['aria-required-children']) /* Mantine's focus sentinel inside role=menu */).toEqual([]);
  });
});

