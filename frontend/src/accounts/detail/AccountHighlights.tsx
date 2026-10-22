import { Button } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { fmtDate } from '../../shell/lib/format';
import { HighlightsPanel } from '../../shell/ui/HighlightsPanel';

type Account = components['schemas']['Account'];
type Dashboard = components['schemas']['AccountDashboard'];

interface Props {
  account: Account;
  dashboard: Dashboard | undefined;
  onAddAction: () => void;
}

/** A-09 as a highlights panel: sector, city, owner, open jobs, last activity, next action. */
export function AccountHighlights({ account: a, dashboard: d, onAddAction }: Props) {
  const { t, i18n } = useTranslation();
  const next = d?.nextAction;
  return (
    <HighlightsPanel
      items={[
        { label: t('accounts.sector'), value: a.sector },
        { label: t('accounts.city'), value: a.city },
        { label: t('accounts.owner'), value: a.owner.name },
        { label: t('accounts.dash.openJobs'), value: <Link to={`/job-orders?accountId=${a.id}`}>{d ? d.openJobOrders : a.openJobCount}</Link> },
        { label: t('accounts.lastActivity'), value: fmtDate(a.lastActivityAt, i18n.language) },
        {
          label: t('accounts.dash.nextAction'),
          value: next ? (
            `${next.title} (${fmtDate(next.dueOn, i18n.language)})`
          ) : (
            <Button size="compact-sm" variant="light" onClick={onAddAction}>{t('accounts.setNextAction')}</Button>
          ),
        },
      ]}
    />
  );
}
