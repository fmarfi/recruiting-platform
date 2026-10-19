import { Button } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { QuickAdd } from './QuickAdd';

/** Button plus sheet with no context, for the top bar (frontend-shell mounts this). */
export function QuickAddLauncher({ onSaved }: { onSaved?: () => void }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button size="compact-md" onClick={() => setOpen(true)} aria-label={t('quickAdd.new')}>
        + {t('quickAdd.new')}
      </Button>
      <QuickAdd opened={open} onClose={() => setOpen(false)} onSaved={onSaved} />
    </>
  );
}
