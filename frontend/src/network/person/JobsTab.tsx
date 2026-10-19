// Clients and jobs: every pipeline the person is in (across accounts) and the client accounts they work for.
import { Anchor, Button, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge, useFormat, type Schemas } from '../../pipeline/shared';

export function JobsTab({
  applications,
  roles,
  canAdd,
  onAdd,
}: {
  applications: Schemas['Application'][];
  roles: Schemas['Contact'][];
  canAdd: boolean;
  onAdd: () => void;
}) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const current = roles.filter((r) => r.active);
  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3} size="h5">
          {t('network.pipelines')}
        </Title>
        <Button variant="light" disabled={!canAdd} onClick={onAdd}>
          {t('network.addToJob')}
        </Button>
      </Group>
      {applications.length === 0 && <Text c="dimmed">{t('network.noPipelines')}</Text>}
      {applications.map((a) => (
        <Group key={a.id} gap="xs" data-testid="person-application">
          <Anchor component={Link} to={`/job-orders/${a.jobOrder.id}`}>
            {a.jobOrder.title}
          </Anchor>
          <Text size="sm" c="dimmed">
            {a.account.name}
          </Text>
          <StatusBadge lifecycle="pipelineStage" value={a.stage} />
          <Text size="xs" c="dimmed">
            {fmt.date(a.stageChangedAt)}
          </Text>
        </Group>
      ))}
      {current.length > 0 && (
        <>
          <Title order={3} size="h5">
            {t('network.accounts')}
          </Title>
          {current.map((r) => (
            <Group key={r.id} gap="xs">
              <Anchor component={Link} to={`/accounts/${r.account.id}`}>
                {r.account.name}
              </Anchor>
              <Text size="sm" c="dimmed">
                {r.role}
              </Text>
            </Group>
          ))}
        </>
      )}
    </Stack>
  );
}
