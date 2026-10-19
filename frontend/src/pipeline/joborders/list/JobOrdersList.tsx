// Flat list of job orders for 3-click access (UX-03): defaults to open jobs owned by me.
import { Alert, Anchor, Button, Chip, Container, Group, Pagination, ScrollArea, SegmentedControl, Skeleton, Table, Text, TextInput, Title } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge, useCurrentUser, useLabel, type Schemas } from '../../shared';
import { useJobOrders } from './useJobOrders';

export function JobOrdersList() {
  const { t } = useTranslation();
  const me = useCurrentUser();
  const { ref, label } = useLabel();
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<Schemas['JobOrderStatus'][]>([]);
  const [mine, setMine] = useState(true);
  const [page, setPage] = useState(0);
  // Wait for /auth/me before the first query so "mine" is applied from the start.
  const ownerId = mine ? me?.id : undefined;
  const res = useJobOrders({ q, status, openOnly: true, ownerId, page });
  const rows = res.data?.content ?? [];
  const statuses = ref?.lifecycles.jobOrder.statuses ?? [];

  return (
    <Container py="md" fluid>
      <Title order={1} data-testid="screen-title" mb="md">
        {t('nav.jobOrders')}
      </Title>
      <Group mb="sm" align="flex-end">
        <TextInput
          label={t('pipeline.list.client')}
          placeholder={t('pipeline.list.searchPlaceholder')}
          value={q}
          onChange={(e) => {
            setQ(e.currentTarget.value);
            setPage(0);
          }}
        />
        <SegmentedControl
          value={mine ? 'me' : 'all'}
          onChange={(v) => {
            setMine(v === 'me');
            setPage(0);
          }}
          data={[
            { value: 'me', label: t('pipeline.list.mine') },
            { value: 'all', label: t('pipeline.list.all') },
          ]}
        />
      </Group>
      <Chip.Group
        multiple
        value={status}
        onChange={(v) => {
          setStatus(v as Schemas['JobOrderStatus'][]);
          setPage(0);
        }}
      >
        <Group gap="xs" mb="md">
          {statuses.map((s) => (
            <Chip key={s.value} value={s.value} size="sm">
              {label('jobOrder', s.value)}
            </Chip>
          ))}
        </Group>
      </Chip.Group>

      {res.error ? (
        <Alert color="red" role="alert" title={t('pipeline.loadFailed')}>
          <Button variant="light" onClick={res.reload}>
            {t('common.retry')}
          </Button>
        </Alert>
      ) : res.loading ? (
        <Skeleton height={160} data-testid="list-loading" />
      ) : rows.length === 0 ? (
        <Text c="dimmed" data-testid="list-empty">
          {t('pipeline.list.empty')}
        </Text>
      ) : (
        <ScrollArea>
          <Table highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{t('pipeline.list.title')}</Table.Th>
                <Table.Th>{t('pipeline.list.client')}</Table.Th>
                <Table.Th>{t('pipeline.list.status')}</Table.Th>
                <Table.Th>{t('pipeline.owner')}</Table.Th>
                <Table.Th>{t('pipeline.list.daysOpen')}</Table.Th>
                <Table.Th>{t('pipeline.list.active')}</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rows.map((j) => (
                <Table.Tr key={j.id}>
                  <Table.Td>
                    <Anchor component={Link} to={`/job-orders/${j.id}`} fw={600}>
                      {j.title}
                    </Anchor>
                  </Table.Td>
                  <Table.Td>{j.account.name}</Table.Td>
                  <Table.Td>
                    <StatusBadge lifecycle="jobOrder" value={j.status} />
                  </Table.Td>
                  <Table.Td>{j.owner.name}</Table.Td>
                  <Table.Td>{j.daysOpen}</Table.Td>
                  <Table.Td>{j.activeCandidates}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </ScrollArea>
      )}
      {(res.data?.page.totalPages ?? 0) > 1 && (
        <Pagination mt="md" total={res.data!.page.totalPages} value={page + 1} onChange={(p) => setPage(p - 1)} />
      )}
    </Container>
  );
}
