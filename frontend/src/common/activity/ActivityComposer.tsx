import { Button, Group } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { ActivityType, Mode } from '../../pipeline/quickadd/quickAddState';

export type ComposerChoice = { mode: Mode; type?: ActivityType };

/**
 * Composer with the four segmented actions: log a call, new task, note, email (log only).
 * Each opens Quick add pre-set, so a record is saved in under 30 seconds (UX-06).
 */
export function ActivityComposer({ onChoose }: { onChoose: (c: ComposerChoice) => void }) {
  const { t } = useTranslation();
  const items: { key: string; choice: ComposerChoice }[] = [
    { key: 'call', choice: { mode: 'activity', type: 'CALL' } },
    { key: 'task', choice: { mode: 'action' } },
    { key: 'note', choice: { mode: 'activity', type: 'NOTE' } },
    { key: 'email', choice: { mode: 'activity', type: 'EMAIL' } },
  ];
  return (
    <Group gap="xs" data-testid="composer">
      {items.map((i) => (
        <Button key={i.key} size="compact-md" variant="default" onClick={() => onChoose(i.choice)}>
          {t(`activity.composer.${i.key}`)}
        </Button>
      ))}
    </Group>
  );
}
