import { Anchor, Button, Group, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { EmptyState } from '../../shell/components/EmptyState';
import { StatusBadge } from '../../shell/components/StatusBadge';

type JobOrder = components['schemas']['JobOrder'];

export function JobsTab({ jobs, onAdd }: { jobs: JobOrder[]; onAdd: () => void }) {
  const { t } = useTranslation();
  if (jobs.length === 0) return <EmptyState title={t('accounts.emptyJobs')} hint={t('accounts.emptyJobsHint')} actions={<Button onClick={onAdd}>+ {t('accounts.newJob')}</Button>} />;
  return (
    <Stack gap="xs">
      {jobs.map((j) => (
        <Group key={j.id} justify="space-between" wrap="nowrap">
          <div>
            <Anchor component={Link} to={`/job-orders/${j.id}`} fw={600}>
              {j.title}
            </Anchor>
            <Text size="sm" c="dimmed">
              {t('myday.daysOpen', { n: j.daysOpen })} - {t('myday.candidates', { n: j.activeCandidates })}
            </Text>
          </div>
          <StatusBadge kind="jobOrder" value={j.status} size="sm" />
        </Group>
      ))}
    </Stack>
  );
}
