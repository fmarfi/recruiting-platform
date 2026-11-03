// Relationship snapshot: last contact, touchpoints, reach and pipelines as KPI tiles, the next-step line is exported separately.
import { Card, SimpleGrid, Text } from '@mantine/core';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { type Schemas } from '../../pipeline/shared';
import { ToneBadge } from '../../shell/components/ToneBadge';
import { relationshipSummary, type Health, type NextStep } from './relationshipHealth';

const TONE: Record<Health, string> = { GOOD: 'GOOD', WARN: 'WARN', BAD: 'BAD' };
/** Only these next steps are overdue or stale; they alone get the warning text colour. */
const WARN_NEXT: NextStep[] = ['overdueAction', 'reconnect', 'neverContacted'];

/** KPI tile (visual system v3, section 6): label 12/500 ink3 above a 28 number in ink, optional line below. Plain panel. */
function Tile({ label, value, sub }: { label: string; value: ReactNode; sub?: ReactNode }) {
  return (
    <Card withBorder padding="md">
      <Text size="xs" c="var(--color-ink3)" fw={500}>
        {label}
      </Text>
      <Text component="div" mt="xs" fz="1.75rem" fw={600} lh={1.2} c="var(--color-ink)" style={{ fontVariantNumeric: 'tabular-nums' }}>
        {value}
      </Text>
      {sub && (
        <Text component="div" size="xs" c="var(--color-ink3)" mt="xs">
          {sub}
        </Text>
      )}
    </Card>
  );
}

/** One neutral line on the next step; rendered inside the Next actions panel. */
export function NextStepLine({ person, activities, actions }: { person: Schemas['Person']; activities: Schemas['Activity'][]; actions: Schemas['NextAction'][] }) {
  const { t } = useTranslation();
  const s = relationshipSummary(person, activities, actions);
  return (
    <Text component="div" size="sm" c={WARN_NEXT.includes(s.next) ? 'var(--tone-warn-fg)' : 'var(--color-ink2)'} data-testid="next-step">
      {t(`network.snapshot.next.${s.next}`, { count: s.overdue })}
    </Text>
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
      <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="sm">
        <Tile
          label={t('network.snapshot.warmth')}
          value={s.days === undefined ? '–' : t('network.daysShort', { count: s.days })}
          sub={<ToneBadge tone={TONE[s.health]} label={t(`network.snapshot.health.${s.health}`)} />}
        />
        <Tile label={t('network.snapshot.touches')} value={s.touches90} sub={t('network.snapshot.last90')} />
        <Tile label={t('network.snapshot.reach')} value={linkCount} sub={t('network.snapshot.reachSub')} />
        <Tile label={t('network.snapshot.pipelines')} value={jobCount} sub={t('network.snapshot.openActions', { count: actions.length })} />
      </SimpleGrid>
    </div>
  );
}
