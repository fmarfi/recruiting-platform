// Clients and jobs: every pipeline the person is in (across accounts) and the client accounts they work for.
import { Anchor, Button, Group, Stack, Text, Title } from '@mantine/core';
import { EmptyState } from '../../shell/components/EmptyState';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Table } from '@mantine/core';
import { StageSelect } from '../../pipeline/candidates/StageSelect';
import type { Stage } from '../../pipeline/board/boardRules';
import { useFormat, type Schemas } from '../../pipeline/shared';

export function JobsTab({
  applications,
  roles,
  canAdd,
  onAdd,
  onStage,
}: {
  applications: Schemas['Application'][];
  roles: Schemas['Contact'][];
  canAdd: boolean;
  onAdd: () => void;
  onStage: (app: Schemas['Application'], to: Stage) => void;
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
          + {t('network.addToJob')}
        </Button>
      </Group>
      {applications.length === 0 && <EmptyState title={t('network.noPipelines')} hint={t('network.noPipelinesHint')} actions={<Button disabled={!canAdd} onClick={onAdd}>+ {t('network.addToJob')}</Button>} />}
      {applications.length > 0 && (
        <Table.ScrollContainer minWidth={560}>
          <Table verticalSpacing="xs" highlightOnHover data-testid="person-applications">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{t('pipeline.cand.search')}</Table.Th>
                <Table.Th>{t('pipeline.hl.client')}</Table.Th>
                <Table.Th>{t('pipeline.cand.stage')}</Table.Th>
                <Table.Th>{t('pipeline.cand.since')}</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {applications.map((a) => (
                <Table.Tr key={a.id} data-testid="person-application">
                  <Table.Td>
                    <Anchor component={Link} to={`/job-orders/${a.jobOrder.id}`} size="sm">
                      {a.jobOrder.title}
                    </Anchor>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm" c="dimmed">
                      {a.account.name}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <StageSelect app={a} onChange={onStage} />
                  </Table.Td>
                  <Table.Td>
                    <Text size="xs" c="dimmed">
                      {fmt.date(a.stageChangedAt)}
                    </Text>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
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
