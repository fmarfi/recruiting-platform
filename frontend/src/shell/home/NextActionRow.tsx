import { ActionIcon, Anchor, Checkbox, Group, Menu, Text } from '@mantine/core';
import { IconDots } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { ICON_SIZE, ICON_STROKE } from '../components/objectIcons';
import { addDays, daysAgo, fmtDate } from '../lib/format';
import type { NextAction } from './useMyDay';

interface Props {
  action: NextAction;
  today: string;
  divided: boolean;
  onDone: () => void;
  onSnooze: (dueOn: string) => void;
}

/** One next action: tick to complete, client link, due text (overdue in the danger tone), snooze menu. */
export function NextActionRow({ action: a, today, divided, onDone, onSnooze }: Props) {
  const { t, i18n } = useTranslation();
  const due = a.overdue ? t('myday.overdueBy', { count: daysAgo(a.dueOn, today) }) : a.dueOn === today ? t('myday.group.today') : fmtDate(a.dueOn, i18n.language);
  return (
    <Group wrap="wrap" justify="space-between" align="center" gap="xs" py="var(--space-2)" style={{ borderTop: divided ? '1px solid var(--color-border)' : undefined }}>
      <Checkbox size="sm" label={a.title} checked={false} onChange={onDone} aria-label={a.title} style={{ flex: '1 1 220px', minWidth: 0 }} styles={{ label: { overflowWrap: 'anywhere' } }} />
      <Group gap="sm" wrap="nowrap">
        {a.account && (
          <Anchor component={Link} to={`/clients/${a.account.id}`} size="sm" underline="hover">
            {a.account.name}
          </Anchor>
        )}
        <Text fz="0.8125rem" c={a.overdue ? 'var(--color-danger)' : 'dimmed'} title={fmtDate(a.dueOn, i18n.language)} style={{ whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' }}>
          {due}
        </Text>
        <Menu withinPortal position="bottom-end">
          <Menu.Target>
            <ActionIcon variant="subtle" color="gray" size="lg" aria-label={t('myday.snooze')} title={t('myday.snooze')}>
              <IconDots size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />
            </ActionIcon>
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
