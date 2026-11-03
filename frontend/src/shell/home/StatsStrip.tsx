import { Card, SimpleGrid, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';

type Stats = components['schemas']['MyDay']['stats'];
type JobOrder = components['schemas']['JobOrder'];

/** Average days open over the searches in the pool. Null when there are none. */
export function averageDaysOpen(jobs: JobOrder[]): number | null {
  if (jobs.length === 0) return null;
  return Math.round(jobs.reduce((sum, j) => sum + j.daysOpen, 0) / jobs.length);
}

/** KPI tile, same look as the person page strip (visual system v3, section 6): label 12/500 ink3 above a 28 number in ink. */
function Tile({ label, value }: { label: string; value: string }) {
  return (
    <Card withBorder padding="md" component={Link} to="/searches" style={{ textDecoration: 'none', color: 'inherit' }}>
      <Text size="xs" c="var(--color-ink3)" fw={500}>
        {label}
      </Text>
      <Text component="div" mt="xs" fz="1.75rem" fw={600} lh={1.2} c="var(--color-ink)" style={{ fontVariantNumeric: 'tabular-nums' }}>
        {value}
      </Text>
    </Card>
  );
}

/**
 * Four KPI tiles for a search firm. `openJobs` is the pool of open searches loaded on the page; `mineOnly` labels the
 * average as covering my searches only.
 */
export function StatsStrip({ stats, openJobs, mineOnly }: { stats: Stats; openJobs: JobOrder[]; mineOnly: boolean }) {
  const { t, i18n } = useTranslation();
  const avg = averageDaysOpen(openJobs);
  const fmt = (n: number | null) => (n === null ? '–' : n.toLocaleString(i18n.language));
  return (
    <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="sm">
      <Tile label={t('myday.stat.openJobs')} value={fmt(stats.openJobOrders)} />
      <Tile label={t('myday.stat.inProcess')} value={fmt(stats.candidatesInProcess)} />
      <Tile label={t(mineOnly ? 'myday.stat.avgDaysOpenMine' : 'myday.stat.avgDaysOpen')} value={fmt(avg)} />
      <Tile label={t('myday.stat.placed')} value={fmt(stats.placedThisYear)} />
    </SimpleGrid>
  );
}
