// Relationship snapshot: warmth, recent touchpoints, reach and a suggested next step, from data already on the page.
import { Alert, Badge, Card, SimpleGrid, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { type Schemas } from '../../pipeline/shared';
import { relationshipSummary, type Health } from './relationshipHealth';

const COLOR: Record<Health, string> = { GOOD: 'green', WARN: 'yellow', BAD: 'red' };

function Tile({ label, value, sub, color }: { label: string; value: React.ReactNode; sub?: React.ReactNode; color?: string }) {
  return (
    <Card withBorder padding="sm">
      <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
        {label}
      </Text>
      <Text fz={24} fw={700} c={color}>
        {value}
      </Text>
      {sub && <Text size="xs" c="dimmed">{sub}</Text>}
    </Card>
  );
}

export function RelationshipSnapshot({
  person,
  activities,
  actions,
  linkCount,
  jobCount,
}: {
  person: Schemas['Person'];
  activities: Schemas['Activity'][];
  actions: Schemas['NextAction'][];
  linkCount: number;
  jobCount: number;
}) {
  const { t } = useTranslation();
  const s = relationshipSummary(person, activities, actions);
  return (
    <div data-testid="relationship-snapshot">
      <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="sm" mb="sm">
        <Tile
          label={t('network.snapshot.warmth')}
          value={s.days === undefined ? '–' : t('network.daysShort', { count: s.days })}
          color={COLOR[s.health]}
          sub={<Badge size="xs" color={COLOR[s.health]} variant="light">{t(`network.snapshot.health.${s.health}`)}</Badge>}
        />
        <Tile label={t('network.snapshot.touches')} value={s.touches90} sub={t('network.snapshot.last90')} />
        <Tile label={t('network.snapshot.reach')} value={linkCount} sub={t('network.snapshot.reachSub')} />
        <Tile label={t('network.snapshot.pipelines')} value={jobCount} sub={t('network.snapshot.openActions', { count: actions.length })} />
      </SimpleGrid>
      <Alert color={s.next === 'doNotContact' ? 'red' : s.next === 'onTrack' ? 'green' : 'blue'} variant="light" data-testid="next-step">
        {t(`network.snapshot.next.${s.next}`, { count: s.overdue })}
      </Alert>
    </div>
  );
}
