import '../record/record.css';
import { Badge, Card, Chip, Group, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useFormat } from '../../pipeline/shared';
import { ActivityComposer, type ComposerChoice } from './ActivityComposer';
import { FILTERS, applyFilter, groupByMonth, type Activity, type NextAction, type TypeFilter } from './groupActivities';

export type ActivityTimelineProps = {
  activities: Activity[];
  /** Open next actions; shown first as "Upcoming and overdue". */
  actions: NextAction[];
  onCompose: (c: ComposerChoice) => void;
};

/** Activity timeline: composer, filter chips, upcoming and overdue group, then past activity by month. */
export function ActivityTimeline({ activities, actions, onCompose }: ActivityTimelineProps) {
  const { t, i18n } = useTranslation();
  const fmt = useFormat();
  const [filter, setFilter] = useState<TypeFilter>('ALL');
  const monthFmt = new Intl.DateTimeFormat(i18n.language === 'en' ? 'en-GB' : 'tr-TR', { month: 'long', year: 'numeric' });
  const groups = groupByMonth(applyFilter(activities, filter));
  const upcoming = [...actions].filter((a) => !a.done).sort((a, b) => a.dueOn.localeCompare(b.dueOn));
  return (
    <Card withBorder padding="md" data-testid="activity-timeline">
      <Stack gap="sm">
        <ActivityComposer onChoose={onCompose} />
        <Chip.Group multiple={false} value={filter} onChange={(v) => setFilter(v as TypeFilter)}>
          <Group gap={6}>
            {FILTERS.map((f) => (
              <Chip key={f} value={f} size="xs">
                {t(`activity.filter.${f}`)}
              </Chip>
            ))}
          </Group>
        </Chip.Group>

        {upcoming.length > 0 && (
          <section aria-label={t('activity.upcoming')}>
            <Text size="sm" fw={600} mb={4}>
              {t('activity.upcoming')}
            </Text>
            {upcoming.map((a) => (
              <Group key={a.id} gap="xs" my={4} wrap="nowrap">
                <Text size="sm" c={a.overdue ? 'red' : undefined} fw={a.overdue ? 600 : 400}>
                  {a.title}
                </Text>
                <Text size="xs" c="dimmed">
                  {fmt.date(a.dueOn)}
                </Text>
                {a.overdue && (
                  <Badge color="red" variant="light" size="sm">
                    {t('activity.overdue')}
                  </Badge>
                )}
              </Group>
            ))}
          </section>
        )}

        {groups.length === 0 && (
          <Text size="sm" c="dimmed">
            {t('activity.empty')}
          </Text>
        )}
        {groups.map((g) => (
          <section key={g.key} aria-label={monthFmt.format(g.date)}>
            <Text size="sm" fw={600} mb={4}>
              {monthFmt.format(g.date)}
            </Text>
            {g.items.map((a) => (
              <div key={a.id} className="related-row" style={{ alignItems: 'flex-start' }}>
                <div>
                  <Text size="sm">
                    <b>{t(`quickAdd.types.${a.type}`)}</b> {a.note}
                  </Text>
                  <Text size="xs" c="dimmed">
                    {a.user.name} - {fmt.dateLong(a.occurredAt)}
                  </Text>
                </div>
              </div>
            ))}
          </section>
        ))}
      </Stack>
    </Card>
  );
}
