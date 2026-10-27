import { Anchor, Button, Card, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api, type components } from '../../api';
import { useAsync } from '../hooks/useAsync';
import { EmptyState } from '../components/EmptyState';
import { StatusBadge } from '../components/StatusBadge';
import { fmtRelative } from '../lib/format';

type JobOrder = components['schemas']['JobOrder'];
type Account = components['schemas']['Account'];

/** The home payload lists only jobs I own; when there are open jobs elsewhere (stat > 0) show the firm's oldest open jobs instead of an empty card. */
function useFirmJobs(enabled: boolean) {
  const res = useAsync(
    () => (enabled ? api.GET('/job-orders', { params: { query: { open: true, size: 6, sort: ['openedOn,asc'] } } }) : Promise.resolve({ data: undefined })),
    [enabled],
  );
  return enabled ? (res.data?.content ?? []) : [];
}

export function OpenJobsCard({ jobs: own, openTotal }: { jobs: JobOrder[]; openTotal: number }) {
  const { t } = useTranslation();
  const firm = useFirmJobs(own.length === 0 && openTotal > 0);
  const jobs = own.length > 0 ? own : firm;
  return (
    <Card withBorder>
      <Group justify="space-between" mb="xs">
        <Title order={2} size="h3">
          {t('myday.jobs')} ({own.length > 0 ? jobs.length : openTotal})
        </Title>
        {openTotal > 0 && (
          <Anchor component={Link} to="/job-orders?owner=all" size="sm">
            {t('myday.viewAllJobs')}
          </Anchor>
        )}
      </Group>
      {jobs.length === 0 && openTotal > 0 ? null : jobs.length === 0 ? (
        <EmptyState
          title={t('myday.noJobs')}
          hint={t('myday.noJobsHint')}
          actions={
            <Button component={Link} to="/accounts" variant="light">
              {t('nav.accounts')}
            </Button>
          }
        />
      ) : (
        jobs.map((j) => (
          <Stack key={j.id} gap={2} py={8} style={{ borderTop: '1px solid var(--color-border)' }}>
            <Anchor component={Link} to={`/job-orders/${j.id}`} fw={600}>
              {j.title} - {j.account.name}
            </Anchor>
            <Group gap="xs">
              <StatusBadge kind="jobOrder" value={j.status} size="sm" />
              <Text size="xs" c="dimmed">
                {t('myday.daysOpen', { n: j.daysOpen })} - {t('myday.candidates', { n: j.activeCandidates })}
              </Text>
            </Group>
          </Stack>
        ))
      )}
    </Card>
  );
}

export function RecentAccountsCard({ accounts, today }: { accounts: Account[]; today: string }) {
  const { t, i18n } = useTranslation();
  return (
    <Card withBorder>
      <Group justify="space-between" mb="xs">
        <Title order={2} size="h3">
          {t('myday.recent')}
        </Title>
        <Anchor component={Link} to="/accounts" size="sm">
          {t('myday.viewAllClients')}
        </Anchor>
      </Group>
      {accounts.map((a) => (
        <Group key={a.id} justify="space-between" py={8} wrap="nowrap" style={{ borderTop: '1px solid var(--color-border)' }}>
          <Anchor component={Link} to={`/accounts/${a.id}`}>
            {a.name}
          </Anchor>
          <Group gap="xs" wrap="nowrap">
            <StatusBadge kind="account" value={a.status} size="sm" />
            <Text size="xs" c="dimmed">
              {fmtRelative(a.lastActivityAt, i18n.language, today)}
            </Text>
          </Group>
        </Group>
      ))}
    </Card>
  );
}
