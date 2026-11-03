import { Box, Button, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { EmptyState } from '../components/EmptyState';
import { HomePanel, HomeSection } from './HomeSection';
import { NextActionRow } from './NextActionRow';
import type { NextAction } from './useMyDay';

interface Props {
  actions: NextAction[];
  today: string;
  undo: NextAction | null;
  onDone: (a: NextAction, done: boolean) => void;
  onSnooze: (a: NextAction, dueOn: string) => void;
  onAdd: () => void;
}

/** Next actions grouped Overdue / Today / Upcoming (A-10). Sits in the left panel of the home page. */
export function NextActionsSection({ actions, today, undo, onDone, onSnooze, onAdd }: Props) {
  const { t } = useTranslation();
  const groups: [string, NextAction[]][] = [
    ['overdue', actions.filter((a) => a.overdue)],
    ['today', actions.filter((a) => !a.overdue && a.dueOn === today)],
    ['upcoming', actions.filter((a) => !a.overdue && a.dueOn !== today)],
  ];
  return (
    <HomePanel>
      <HomeSection title={t('myday.actions')}>
        {undo && (
          <Group justify="space-between" wrap="nowrap" gap="sm" py="var(--space-1)" role="status">
            <Text size="sm" c="var(--color-ink2)">
              {t('myday.doneToast', { title: undo.title })}
            </Text>
            <Button size="compact-sm" variant="subtle" onClick={() => onDone({ ...undo, version: undo.version + 1 }, false)}>
              {t('shell.undo')}
            </Button>
          </Group>
        )}
        {actions.length === 0 ? (
          <EmptyState title={t('myday.caughtUp')} hint={t('myday.caughtUpHint')} actions={<Button variant="default" onClick={onAdd}>{t('accounts.addAction')}</Button>} />
        ) : (
          groups
            .filter(([, list]) => list.length > 0)
            .map(([g, list]) => (
              <Box key={g}>
                <Text fz="0.75rem" fw={500} c="dimmed" mt="var(--space-3)">
                  {t(`myday.group.${g}`)}
                </Text>
                {list.map((a, i) => (
                  <NextActionRow key={a.id} action={a} today={today} divided={i > 0} onDone={() => onDone(a, true)} onSnooze={(d) => onSnooze(a, d)} />
                ))}
              </Box>
            ))
        )}
      </HomeSection>
    </HomePanel>
  );
}
