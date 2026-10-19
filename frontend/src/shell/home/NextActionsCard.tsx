import { Alert, Button, Card, Group, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { EmptyState } from '../components/EmptyState';
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

/** Next actions grouped Overdue / Today / Upcoming (A-10). */
export function NextActionsCard({ actions, today, undo, onDone, onSnooze, onAdd }: Props) {
  const { t } = useTranslation();
  const groups: [string, NextAction[]][] = [
    ['overdue', actions.filter((a) => a.overdue)],
    ['today', actions.filter((a) => !a.overdue && a.dueOn === today)],
    ['upcoming', actions.filter((a) => !a.overdue && a.dueOn !== today)],
  ];
  return (
    <Card withBorder>
      <Group justify="space-between" mb="xs">
        <Title order={2} size="h3">
          {t('myday.actions')} ({actions.length})
        </Title>
        <Button size="xs" variant="light" onClick={onAdd}>
          + {t('accounts.addAction')}
        </Button>
      </Group>
      {undo && (
        <Alert color="green" mb="xs" role="status">
          <Group justify="space-between">
            <span>{t('myday.doneToast', { title: undo.title })}</span>
            <Button size="compact-xs" variant="subtle" onClick={() => onDone({ ...undo, version: undo.version + 1 }, false)}>
              {t('shell.undo')}
            </Button>
          </Group>
        </Alert>
      )}
      {actions.length === 0 ? (
        <EmptyState title={t('myday.caughtUp')} hint={t('myday.caughtUpHint')} actions={<Button onClick={onAdd}>{t('accounts.addAction')}</Button>} />
      ) : (
        groups
          .filter(([, list]) => list.length > 0)
          .map(([g, list]) => (
            <div key={g}>
              <Text size="xs" fw={700} tt="uppercase" c={g === 'overdue' ? 'red' : 'dimmed'} mt="xs">
                {t(`myday.group.${g}`)}
              </Text>
              {list.map((a) => (
                <NextActionRow key={a.id} action={a} today={today} onDone={() => onDone(a, true)} onSnooze={(d) => onSnooze(a, d)} />
              ))}
            </div>
          ))
      )}
    </Card>
  );
}
