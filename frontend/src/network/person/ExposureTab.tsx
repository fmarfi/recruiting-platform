// Exposure history (B-05): which client accounts saw this person, for which job order, when, and the outcome so far.
import { Alert, Anchor, Badge, Skeleton, Stack, Table, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { useAsync, useFormat, useLabel } from '../../pipeline/shared';

export function ExposureTab({ personId }: { personId: string }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const { label } = useLabel();
  const res = useAsync(() => api.GET('/people/{id}/exposure', { params: { path: { id: personId } } }), [personId]);
  if (res.loading) return <Skeleton height={80} />;
  if (res.error || !res.data) return <Alert color="red">{t('network.exposure.loadFailed')}</Alert>;
  if (res.data.length === 0) return <Text c="dimmed">{t('network.exposure.empty')}</Text>;
  return (
    <Stack>
      <Table.ScrollContainer minWidth={520}>
        <Table data-testid="exposure-table">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('network.exposure.account')}</Table.Th>
              <Table.Th>{t('network.exposure.jobOrder')}</Table.Th>
              <Table.Th>{t('network.exposure.date')}</Table.Th>
              <Table.Th>{t('network.exposure.outcome')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {res.data.map((e) => (
              <Table.Tr key={e.id}>
                <Table.Td>
                  <Anchor component={Link} to={`/accounts/${e.account.id}`}>
                    {e.account.name}
                  </Anchor>
                </Table.Td>
                <Table.Td>
                  <Anchor component={Link} to={`/job-orders/${e.jobOrder.id}`}>
                    {e.jobOrder.title}
                  </Anchor>
                </Table.Td>
                <Table.Td>
                  {fmt.dateLong(e.presentedAt)}
                  {e.presentedBy ? ` - ${e.presentedBy.name}` : ''}{' '}
                  <Text span size="xs" c="dimmed">
                    ({t(`network.exposure.channel.${e.channel}`)})
                  </Text>
                </Table.Td>
                <Table.Td>
                  {e.stage ? (
                    <Badge variant="light">{label('pipelineStage', e.stage)}</Badge>
                  ) : (
                    <Text span c="dimmed">
                      {t('network.exposure.removed')}
                    </Text>
                  )}
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Stack>
  );
}
