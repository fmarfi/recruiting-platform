import { Button, Card, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { Highlights } from '../../shell/components/Highlights';
import { fmtDate } from '../../shell/lib/format';
import { useReference } from '../../shell/refdata/useReference';

type Dashboard = components['schemas']['AccountDashboard'];

const STAGES = ['SOURCED', 'SCREENED', 'PRESENTED', 'CLIENT_INTERVIEW', 'OFFER'] as const;

/** A-09: open jobs, candidates per stage, average days open, next action (highlights panel). */
export function DashboardStrip({ dashboard: d, onAddAction }: { dashboard: Dashboard | undefined; onAddAction: () => void }) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const s = d?.candidatesPerStage;
  const total = s ? STAGES.reduce((n, k) => n + (s[k] ?? 0), 0) : undefined;
  return (
    <Card withBorder data-testid="account-dashboard">
      <Highlights
        items={[
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
              <Button size="compact-sm" variant="light" onClick={onAddAction}>
                {t('accounts.setNextAction')}
              </Button>
            ),
          },
        ]}
      />
    </Card>
  );
}
