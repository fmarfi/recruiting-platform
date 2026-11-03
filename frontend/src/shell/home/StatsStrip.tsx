import { Paper, SimpleGrid, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';

type Stats = components['schemas']['MyDay']['stats'];
type JobOrder = components['schemas']['JobOrder'];

/** Average days open over the searches in the payload (my open searches). Null when there are none. */
export function averageDaysOpen(jobs: JobOrder[]): number | null {
  if (jobs.length === 0) return null;
  return Math.round(jobs.reduce((sum, j) => sum + j.daysOpen, 0) / jobs.length);
}

/** Four KPI tiles for a search firm (visual system v3 sections 4 and 6): plain panels, label above number. */
export function StatsStrip({ stats, openJobs }: { stats: Stats; openJobs: JobOrder[] }) {
  const { t, i18n } = useTranslation();
  const avg = averageDaysOpen(openJobs);
  const items: [string, number | null, boolean][] = [
    ['openJobs', stats.openJobOrders, false],
    ['inProcess', stats.candidatesInProcess, false],
    ['avgDaysOpen', avg, true],
    ['placed', stats.placedThisYear, false],
  ];
  return (
    <SimpleGrid cols={{ base: 2, md: 4 }}>
      {items.map(([k, n, scoped]) => (
        <Paper
          key={k}
          withBorder
          p="md"
          component={Link}
          to="/searches"
          style={{ boxShadow: 'none', textDecoration: 'none', color: 'inherit' }}
        >
          <Text fz="0.75rem" fw={500} c="dimmed">
            {t(`myday.stat.${k}`)}
          </Text>
          <Text fz="1.75rem" fw={600} lh={1.2} mt="var(--space-2)" style={{ fontVariantNumeric: 'tabular-nums' }}>
            {n === null ? '-' : n.toLocaleString(i18n.language)}
          </Text>
          {scoped && (
            <Text fz="0.8125rem" c="dimmed" mt="var(--space-1)">
              {t('myday.stat.avgNote')}
            </Text>
          )}
        </Paper>
      ))}
    </SimpleGrid>
  );
}
