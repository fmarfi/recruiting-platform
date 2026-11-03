import { Anchor, Button, Group, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { StatusBadge } from '../../shell/components/StatusBadge';
import { SectionEmpty } from './SectionEmpty';

type JobOrder = components['schemas']['JobOrder'];

export function JobsTab({ jobs, onAdd }: { jobs: JobOrder[]; onAdd: () => void }) {
  const { t } = useTranslation();
  if (jobs.length === 0) return <SectionEmpty text={t('accounts.emptyJobs')} action={<Button size="compact-sm" variant="light" onClick={onAdd}>+ {t('accounts.newJob')}</Button>} />;
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
