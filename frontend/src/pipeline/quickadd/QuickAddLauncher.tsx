import { Button } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { QuickAdd } from './QuickAdd';

/** "New" in the top bar: secondary style with a plus icon (v3 header bar). Opens quick add with no context. */
export function QuickAddLauncher({ onSaved }: { onSaved?: () => void }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button
        size="compact-md"
        variant="default"
        leftSection={<IconPlus size={16} stroke={1.75} aria-hidden />}
        onClick={() => setOpen(true)}
        aria-label={t('quickAdd.new')}
      >
        {t('quickAdd.new')}
      </Button>
      <QuickAdd opened={open} onClose={() => setOpen(false)} onSaved={onSaved} />
    </>
  );
}
