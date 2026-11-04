import { Anchor, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { StatusBadge } from '../../shell/components/StatusBadge';
import { AddButton } from './AddButton';
import { SectionEmpty } from './SectionEmpty';

type JobOrder = components['schemas']['JobOrder'];

export function JobsTab({ jobs, onAdd }: { jobs: JobOrder[]; onAdd: () => void }) {
  const { t } = useTranslation();
  if (jobs.length === 0) return <SectionEmpty text={t('accounts.emptyJobs')} action={<AddButton label={t('accounts.newJob')} onClick={onAdd} />} />;
  return (
    <>
      {jobs.map((j, i) => (
        <Group key={j.id} justify="space-between" wrap="nowrap" gap="sm" style={{ padding: 'var(--space-3) var(--space-4)', borderTop: i === 0 ? 0 : '1px solid var(--color-border)' }}>
          <div style={{ minWidth: 0 }}>
            <Anchor component={Link} to={`/job-orders/${j.id}`} fw={500} underline="never" c="var(--color-ink)">
              {j.title}
            </Anchor>
            <Text size="sm" c="var(--color-ink3)">
              {t('myday.daysOpen', { count: j.daysOpen })} - {t('myday.candidates', { n: j.activeCandidates })}
            </Text>
          </div>
          <StatusBadge kind="jobOrder" value={j.status} size="sm" />
        </Group>
      ))}
    </>
  );
}
