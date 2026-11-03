import { Anchor, Box, Button, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api, type components } from '../../api';
import { useAsync } from '../hooks/useAsync';
import { EmptyState } from '../components/EmptyState';
import { ClientIcon, ICON_SIZE, ICON_STROKE, SearchIcon } from '../components/objectIcons';
import { StatusBadge } from '../components/StatusBadge';
import { fmtRelative } from '../lib/format';
import { HomeSection, rowDivider } from './HomeSection';

type JobOrder = components['schemas']['JobOrder'];
type Account = components['schemas']['Account'];

/** The home payload lists only jobs I own; when there are open jobs elsewhere (stat > 0) show the firm's oldest open jobs instead of an empty list. */
function useFirmJobs(enabled: boolean) {
  const res = useAsync(
    () => (enabled ? api.GET('/job-orders', { params: { query: { open: true, size: 6, sort: ['openedOn,asc'] } } }) : Promise.resolve({ data: undefined })),
    [enabled],
  );
  return enabled ? (res.data?.content ?? []) : [];
}

/** Open searches with object icon, client, days open and candidates. Right-hand panel. */
export function OpenSearchesSection({ jobs: own, openTotal }: { jobs: JobOrder[]; openTotal: number }) {
  const { t } = useTranslation();
  const firm = useFirmJobs(own.length === 0 && openTotal > 0);
  const jobs = own.length > 0 ? own : firm;
  return (
    <HomeSection
      title={t('myday.jobs')}
      link={
        openTotal > 0 && (
          <Anchor component={Link} to="/job-orders?owner=all" size="sm" underline="hover">
            {t('myday.viewAllJobs')}
          </Anchor>
        )
      }
    >
      {jobs.length === 0 && openTotal > 0 ? null : jobs.length === 0 ? (
        <EmptyState
          title={t('myday.noJobs')}
          hint={t('myday.noJobsHint')}
          actions={
            <Button component={Link} to="/accounts" variant="default">
              {t('nav.accounts')}
            </Button>
          }
        />
      ) : (
        jobs.map((j, i) => (
          <Group key={j.id} wrap="nowrap" align="flex-start" gap="sm" py="var(--space-2)" style={rowDivider(i > 0)}>
            <Box c="dimmed" style={{ display: 'flex', flex: 'none', paddingTop: "var(--space-1)" }}>
              <SearchIcon size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />
            </Box>
            <Box style={{ flex: '1 1 auto', minWidth: 0 }}>
              <Anchor component={Link} to={`/searches/${j.id}`} size="sm" fw={500} underline="hover">
                {j.title}
              </Anchor>
              <Group gap="sm" mt="var(--space-1)">
                <Text fz="0.8125rem" c="dimmed">
                  {j.account.name}
                </Text>
                <Text fz="0.8125rem" c="dimmed" style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {t('myday.daysOpen', { n: j.daysOpen })}
                </Text>
                <Text fz="0.8125rem" c="dimmed" style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {t('myday.candidates', { n: j.activeCandidates })}
                </Text>
              </Group>
            </Box>
            <StatusBadge kind="jobOrder" value={j.status} size="sm" />
          </Group>
        ))
      )}
    </HomeSection>
  );
}

/** Recently active clients with object icon, status and last activity. Right-hand panel, below open searches. */
export function RecentClientsSection({ accounts, today }: { accounts: Account[]; today: string }) {
  const { t, i18n } = useTranslation();
  return (
    <HomeSection
      title={t('myday.recent')}
      divided
      link={
        <Anchor component={Link} to="/accounts" size="sm" underline="hover">
          {t('myday.viewAllClients')}
        </Anchor>
      }
    >
      {accounts.map((a, i) => (
        <Group key={a.id} justify="space-between" wrap="nowrap" gap="sm" py="var(--space-2)" style={rowDivider(i > 0)}>
          <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
            <Box c="dimmed" style={{ display: 'flex', flex: 'none' }}>
              <ClientIcon size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />
            </Box>
            <Anchor component={Link} to={`/clients/${a.id}`} size="sm" fw={500} underline="hover">
              {a.name}
            </Anchor>
          </Group>
          <Group gap="sm" wrap="nowrap" style={{ flex: 'none' }}>
            <StatusBadge kind="account" value={a.status} size="sm" />
            <Text fz="0.8125rem" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
              {fmtRelative(a.lastActivityAt, i18n.language, today)}
            </Text>
          </Group>
        </Group>
      ))}
    </HomeSection>
  );
}
