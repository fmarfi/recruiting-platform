// Aside of the client page (visual system v3 section 5): Details (DetailList) and Next actions.
import { Button, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { DetailList } from '../../shell/components/DetailList';
import { Panel } from '../../shell/components/Panel';
import { fmtDate } from '../../shell/lib/format';
import { useReference } from '../../shell/refdata/useReference';

type Account = components['schemas']['Account'];
type Dashboard = components['schemas']['AccountDashboard'];

/** Only http(s) links become anchors; anything else is shown as plain text. */
const webHref = (u?: string | null) => (u && /^https?:\/\//i.test(u) ? u : undefined);

export function AccountAside({ account: a, dashboard: d, onAddAction }: { account: Account; dashboard: Dashboard | undefined; onAddAction: () => void }) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const next = d?.nextAction;
  return (
    <>
      <Panel title={t('accounts.record.details')} testId="account-details">
        <DetailList
          items={[
            { label: t('accounts.sector'), value: a.sector },
            { label: t('accounts.city'), value: a.city },
            { label: t('accounts.record.website'), value: a.website, href: webHref(a.website) },
            { label: t('accounts.owner'), value: a.owner.name },
            { label: t('accounts.record.status'), value: ref.label('account', a.status) },
            { label: t('accounts.record.notes'), value: a.notes },
            { label: t('accounts.record.lastUpdated'), value: fmtDate(a.updatedAt, i18n.language) },
          ]}
        />
      </Panel>
      <Panel title={t('accounts.record.nextActions')}>
        {next ? (
          <Stack gap={4}>
            <Text size="sm" fw={500} c="var(--color-ink)">
              {next.title}
            </Text>
            <Text size="sm" c="var(--color-ink3)">
              {fmtDate(next.dueOn, i18n.language)}
            </Text>
          </Stack>
        ) : (
          <Stack gap="sm" align="flex-start">
            <Text size="sm" c="var(--color-ink3)">
              {t('accounts.record.noNextAction')}
            </Text>
            <Button size="sm" variant="default" onClick={onAddAction}>
              {t('accounts.setNextAction')}
            </Button>
          </Stack>
        )}
      </Panel>
    </>
  );
}
