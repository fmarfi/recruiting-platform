import { Anchor, Button, Checkbox, Group, Menu, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { addDays, daysAgo, fmtDate } from '../lib/format';
import type { NextAction } from './useMyDay';

interface Props {
  action: NextAction;
  today: string;
  onDone: () => void;
  onSnooze: (dueOn: string) => void;
}

export function NextActionRow({ action: a, today, onDone, onSnooze }: Props) {
  const { t, i18n } = useTranslation();
  const due = a.overdue ? t('myday.overdueBy', { n: daysAgo(a.dueOn, today) }) : a.dueOn === today ? t('myday.group.today') : fmtDate(a.dueOn, i18n.language);
  return (
    <Group wrap="nowrap" justify="space-between" py={6} style={{ minHeight: 'var(--touch-target-min)' }}>
      <Checkbox label={a.title} checked={false} onChange={onDone} aria-label={a.title} />
      <Group gap="xs" wrap="nowrap">
        {a.account && (
          <Anchor component={Link} to={`/accounts/${a.account.id}`} size="sm">
            {a.account.name}
          </Anchor>
        )}
        <Text size="sm" c={a.overdue ? 'red' : 'dimmed'} title={fmtDate(a.dueOn, i18n.language)}>
          {due}
        </Text>
        <Menu withinPortal position="bottom-end">
          <Menu.Target>
            <Button size="compact-xs" variant="subtle" aria-label={t('myday.snooze')}>
              ...
            </Button>
          </Menu.Target>
          <Menu.Dropdown>
            <Menu.Item onClick={() => onSnooze(addDays(today, 1))}>{t('myday.snoozeTomorrow')}</Menu.Item>
            <Menu.Item onClick={() => onSnooze(addDays(today, 7))}>{t('myday.snoozeWeek')}</Menu.Item>
          </Menu.Dropdown>
        </Menu>
      </Group>
    </Group>
  );
}
