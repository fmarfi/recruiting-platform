import { Anchor, Button, Card, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { EmptyState } from '../components/EmptyState';
import { StatusBadge } from '../components/StatusBadge';
import { fmtRelative } from '../lib/format';

type JobOrder = components['schemas']['JobOrder'];
type Account = components['schemas']['Account'];

export function OpenJobsCard({ jobs }: { jobs: JobOrder[] }) {
  const { t } = useTranslation();
  return (
    <Card withBorder>
      <Title order={2} size="h3" mb="xs">
        {t('myday.jobs')} ({jobs.length})
      </Title>
      {jobs.length === 0 ? (
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
          <Stack key={j.id} gap={2} py={6}>
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
      <Title order={2} size="h3" mb="xs">
        {t('myday.recent')}
      </Title>
      {accounts.map((a) => (
        <Group key={a.id} justify="space-between" py={6} wrap="nowrap">
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
