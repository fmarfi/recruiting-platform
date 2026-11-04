import { Box, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { useReference } from '../../shell/refdata/useReference';

type Dashboard = components['schemas']['AccountDashboard'];

const STAGES = ['SOURCED', 'SCREENED', 'PRESENTED', 'CLIENT_INTERVIEW', 'OFFER'] as const;

/** A-09: open searches, candidates per stage, average days open. Plain tiles on one white surface, split by 1 px lines (no cards). The next action lives in the aside. */
export function DashboardStrip({ dashboard: d }: { dashboard: Dashboard | undefined }) {
  const { t } = useTranslation();
  const ref = useReference();
  const s = d?.candidatesPerStage;
  const total = s ? STAGES.reduce((n, k) => n + (s[k] ?? 0), 0) : undefined;
  const items: { label: string; value: ReactNode }[] = [
    { label: t('accounts.dash.openJobs'), value: d ? d.openJobOrders : '-' },
    {
      label: t('accounts.dash.candidates'),
      value: s ? (
        <span title={STAGES.map((k) => `${ref.label('pipelineStage', k)}: ${s[k] ?? 0}`).join(' / ')}>
          {total}
          <Text component="span" display="block" size="xs" c="var(--color-ink3)" fw={400}>
            {STAGES.filter((k) => (s[k] ?? 0) > 0)
              .map((k) => `${s[k]} ${ref.label('pipelineStage', k).toLocaleLowerCase()}`)
              .join(' · ')}
          </Text>
        </span>
      ) : (
        '-'
      ),
    },
    { label: t('accounts.dash.avgDays'), value: d?.avgDaysOpen != null ? Math.round(d.avgDaysOpen) : '-' },
  ];
  return (
    <Box
      component="dl"
      m={0}
      data-testid="account-dashboard"
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(11rem, 1fr))',
        gap: 1,
        background: 'var(--color-border)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--space-2)',
        overflow: 'hidden',
      }}
    >
      {items.map((h) => (
        <div key={h.label} style={{ minWidth: 0, background: 'var(--color-surface)', padding: 'var(--space-3) var(--space-4)' }}>
          <Text component="dt" fz="0.8125rem" fw={400} c="var(--color-ink3)" style={{ marginBottom: 'var(--space-1)' }}>
            {h.label}
          </Text>
          <Text component="dd" fz="1.25rem" m={0} c="var(--color-ink)" fw={600} style={{ overflowWrap: 'anywhere', fontVariantNumeric: 'tabular-nums' }}>
            {h.value}
          </Text>
        </div>
      ))}
    </Box>
  );
}
