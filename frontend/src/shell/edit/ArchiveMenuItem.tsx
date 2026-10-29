import { Menu } from '@mantine/core';
import { useContext, useState } from 'react';
import { useText } from './text';
import { UndoContext } from './undo';

export interface ArchiveMenuItemProps {
  /** Record name for the toast message. */
  name: string;
  /**
   * "hidden" (default) renders nothing, "disabled" shows the item greyed out with a hint,
   * "enabled" is for when the backend supports archive.
   */
  mode?: 'hidden' | 'disabled' | 'enabled';
  /** Archives the record. */
  onArchive: () => Promise<void>;
  /** When given, the success toast offers Undo and calls this. */
  onRestore?: () => Promise<void>;
}

/** Menu item for a record overflow `Menu` ("Archive", never "Delete"). Reversible, so it undoes instead of confirming. */
export function ArchiveMenuItem({ name, mode = 'hidden', onArchive, onRestore }: ArchiveMenuItemProps) {
  const { tx } = useText();
  const showUndo = useContext(UndoContext);
  const [error, setError] = useState(false);
  if (mode === 'hidden') return null;

  if (mode === 'disabled') {
    return (
      <Menu.Item disabled title={tx('edit.archive.unavailable')}>
        {tx('edit.archive.action')}
      </Menu.Item>
    );
  }

  return (
    <Menu.Item
      color={error ? 'red' : undefined}
      onClick={async () => {
        setError(false);
        try {
          await onArchive();
          if (onRestore && showUndo) showUndo({ message: tx('edit.archive.done', { name }), onUndo: onRestore });
        } catch {
          setError(true);
        }
      }}
    >
      {error ? tx('edit.archive.error') : tx('edit.archive.action')}
    </Menu.Item>
  );
}

