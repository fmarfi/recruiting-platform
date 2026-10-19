import { Card, SimpleGrid, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';

type Stats = components['schemas']['MyDay']['stats'];

/** Four numbers, each linking to the matching list (UX-02). */
export function StatsStrip({ stats }: { stats: Stats }) {
  const { t } = useTranslation();
  const items = [
    ['activeAccounts', stats.activeAccounts, '/accounts'],
    ['openJobs', stats.openJobOrders, '/job-orders'],
    ['inProcess', stats.candidatesInProcess, '/job-orders'],
    ['placed', stats.placedThisYear, '/job-orders'],
  ] as const;
  return (
    <SimpleGrid cols={{ base: 2, md: 4 }}>
      {items.map(([k, n, to]) => (
        <Card key={k} withBorder component={Link} to={to} style={{ textDecoration: 'none', color: 'inherit' }}>
          <Text size="xl" fw={700}>
            {n}
          </Text>
          <Text size="sm" c="dimmed">
            {t(`myday.stat.${k}`)}
          </Text>
        </Card>
      ))}
    </SimpleGrid>
  );
}
