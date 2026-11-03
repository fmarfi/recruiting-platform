import { Box, Button, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { fmtDate } from '../../shell/lib/format';
import { useReference } from '../../shell/refdata/useReference';

type Dashboard = components['schemas']['AccountDashboard'];

const STAGES = ['SOURCED', 'SCREENED', 'PRESENTED', 'CLIENT_INTERVIEW', 'OFFER'] as const;

/** A-09: open jobs, candidates per stage, average days open, next action. A plain fact row on the canvas, not a card. */
export function DashboardStrip({ dashboard: d, onAddAction }: { dashboard: Dashboard | undefined; onAddAction: () => void }) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const s = d?.candidatesPerStage;
  const total = s ? STAGES.reduce((n, k) => n + (s[k] ?? 0), 0) : undefined;
  const items: { label: string; value: ReactNode }[] = [
    { label: t('accounts.dash.openJobs'), value: d ? d.openJobOrders : '-' },
    {
      label: t('accounts.dash.candidates'),
      value: s ? (
        <span title={STAGES.map((k) => `${ref.label('pipelineStage', k)}: ${s[k] ?? 0}`).join(' / ')}>
          {total} <Text span size="xs" c="dimmed" fw={400}>({STAGES.map((k) => s[k] ?? 0).join('/')})</Text>
        </span>
      ) : (
        '-'
      ),
    },
    { label: t('accounts.dash.avgDays'), value: d?.avgDaysOpen != null ? Math.round(d.avgDaysOpen) : '-' },
    {
      label: t('accounts.dash.nextAction'),
      value: d?.nextAction ? (
        `${d.nextAction.title} (${fmtDate(d.nextAction.dueOn, i18n.language)})`
      ) : (
        <Button size="sm" variant="default" onClick={onAddAction}>
          {t('accounts.setNextAction')}
        </Button>
      ),
    },
  ];
  return (
    <Box component="dl" m={0} data-testid="account-dashboard" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(11rem, 1fr))', gap: 'var(--space-3) var(--space-5)' }}>
      {items.map((h) => (
        <div key={h.label} style={{ minWidth: 0 }}>
          <Text component="dt" size="xs" c="dimmed" fw={500} style={{ marginBottom: 'var(--space-2)' }}>
            {h.label}
          </Text>
          <Text component="dd" m={0} c="var(--color-ink)" fw={500} style={{ overflowWrap: 'anywhere' }}>
            {h.value}
          </Text>
        </div>
      ))}
    </Box>
  );
}
