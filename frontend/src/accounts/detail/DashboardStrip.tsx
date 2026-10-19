import { Button, Card, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { fmtDate } from '../../shell/lib/format';

type Dashboard = components['schemas']['AccountDashboard'];

/** A-09: open jobs, candidates per stage, average days open, next action. */
export function DashboardStrip({ dashboard: d, onAddAction }: { dashboard: Dashboard | undefined; onAddAction: () => void }) {
  const { t, i18n } = useTranslation();
  const s = d?.candidatesPerStage;
  return (
    <Card withBorder>
      <Group gap="xl" wrap="wrap">
        <Text>
          {t('accounts.dash.openJobs')}: <b>{d ? d.openJobOrders : '-'}</b>
        </Text>
        <Text>
          {t('accounts.dash.candidates')}: <b>{s ? `${s.SOURCED}/${s.SCREENED}/${s.PRESENTED}/${s.CLIENT_INTERVIEW}/${s.OFFER}` : '-'}</b>
        </Text>
        <Text>
          {t('accounts.dash.avgDays')}: <b>{d?.avgDaysOpen != null ? Math.round(d.avgDaysOpen) : '-'}</b>
        </Text>
        <Text>
          {t('accounts.dash.nextAction')}:{' '}
          {d?.nextAction ? (
            <b>
              {d.nextAction.title} ({fmtDate(d.nextAction.dueOn, i18n.language)})
            </b>
          ) : (
            <Button size="compact-sm" variant="light" onClick={onAddAction}>
              {t('accounts.setNextAction')}
            </Button>
          )}
        </Text>
      </Group>
    </Card>
  );
}
