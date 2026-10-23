import { Button, Kbd } from '@mantine/core';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

/** Ctrl+K / Cmd+K toggles the command bar, also while typing in a field. */
export function useCommandHotkey(toggle: () => void) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && !e.altKey && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        toggle();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [toggle]);
}

/** Visible entry point in the shell header. */
export function CommandButton({ onClick }: { onClick: () => void }) {
  const { t } = useTranslation();
  return (
    <Button variant="default" size="sm" onClick={onClick} aria-label={t('command.open')} aria-haspopup="dialog" rightSection={<Kbd size="xs" visibleFrom="md">Ctrl K</Kbd>}>
      {t('command.button')}
    </Button>
  );
}
