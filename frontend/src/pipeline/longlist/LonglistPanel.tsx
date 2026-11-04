// Longlist panel (R65, S5): the ranked table of a search. Rank order, call status, rating and rationale are edited inline.
import { ActionIcon, Alert, Button, Group, Select, Table, Text, TextInput } from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconUsersPlus } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Panel } from '../../shell/components/Panel';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';
import { type Schemas } from '../shared';
import { ShortlistDialog } from './ShortlistDialog';
import { useLonglist, type App } from './useLonglist';

export const OUTREACH: Schemas['OutreachStatus'][] = ['TO_CALL', 'CALLED', 'INTERESTED', 'NOT_INTERESTED', 'NOT_NOW', 'SCREENED'];

function RationaleCell({ app, disabled, onSave }: { app: App; disabled: boolean; onSave: (v: string) => void }) {
  const { t } = useTranslation();
  const [v, setV] = useState(app.rationale ?? '');
  return (
    <TextInput
      size="xs"
      aria-label={t('pipeline.ll.rationale')}
      value={v}
      disabled={disabled}
      onChange={(e) => setV(e.currentTarget.value)}
      onBlur={() => v !== (app.rationale ?? '') && onSave(v)}
    />
  );
}

export function LonglistPanel({ jobId, apps, readOnly, reload, onBulkAdd }: { jobId: string; apps: App[]; readOnly: boolean; reload: () => void; onBulkAdd: () => void }) {
  const { t } = useTranslation();
  const ll = useLonglist(jobId, apps, reload);
  const [ask, setAsk] = useState<App>();
  return (
    <Panel
      title={t('pipeline.ll.title')}
      count={ll.rows.length}
      testId="longlist"
      padding="none"
      actions={
        !readOnly && (
          <Button variant="default" size="compact-md" leftSection={<IconUsersPlus size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />} onClick={onBulkAdd}>
            {t('pipeline.ll.bulkAdd')}
          </Button>
        )
      }
      empty={ll.rows.length === 0 ? t('pipeline.ll.empty') : undefined}
    >
      {ll.error && (
        <Alert color="red" role="alert" data-testid="longlist-error" m="sm">
          {ll.error}
        </Alert>
      )}
      <Table.ScrollContainer minWidth={900}>
        <Table verticalSpacing="xs" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th w="7rem">{t('pipeline.ll.rank')}</Table.Th>
              <Table.Th>{t('pipeline.ll.name')}</Table.Th>
              <Table.Th>{t('pipeline.ll.roleCompany')}</Table.Th>
              <Table.Th w="11rem">{t('pipeline.ll.status')}</Table.Th>
              <Table.Th w="6rem">{t('pipeline.ll.rating')}</Table.Th>
              <Table.Th>{t('pipeline.ll.rationale')}</Table.Th>
              <Table.Th w="8rem" />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {ll.rows.map((a, i) => (
              <Table.Tr key={a.id} data-testid={`ll-row-${a.id}`}>
                <Table.Td>
                  <Group gap="xs" wrap="nowrap">
                    <Text span fz="sm" style={{ fontVariantNumeric: 'tabular-nums' }} data-testid="ll-rank">
                      {a.rank ?? i + 1}
                    </Text>
                    <ActionIcon variant="subtle" size="sm" aria-label={t('pipeline.ll.up', { name: a.person.name })} disabled={readOnly || ll.pending || i === 0} onClick={() => void ll.move(a.id, -1)}>
                      <IconArrowUp size={ICON_SIZE} stroke={ICON_STROKE} />
                    </ActionIcon>
                    <ActionIcon variant="subtle" size="sm" aria-label={t('pipeline.ll.down', { name: a.person.name })} disabled={readOnly || ll.pending || i === ll.rows.length - 1} onClick={() => void ll.move(a.id, 1)}>
                      <IconArrowDown size={ICON_SIZE} stroke={ICON_STROKE} />
                    </ActionIcon>
                  </Group>
                </Table.Td>
                <Table.Td>
                  <Text component={Link} to={`/people/${a.person.id}`} fz="sm" c="var(--color-navy)">
                    {a.person.name}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Text fz="sm" c="var(--color-ink2)">
                    {[a.person.title, a.person.company].filter(Boolean).join(', ') || '–'}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Select
                    size="xs"
                    aria-label={t('pipeline.ll.status')}
                    data={OUTREACH.map((s) => ({ value: s, label: t(`pipeline.ll.outreach.${s}`) }))}
                    value={a.outreachStatus ?? 'TO_CALL'}
                    allowDeselect={false}
                    disabled={readOnly || ll.pending}
                    onChange={(v) => v && void ll.patch(a, { outreachStatus: v as Schemas['OutreachStatus'] })}
                  />
                </Table.Td>
                <Table.Td>
                  <Select
                    size="xs"
                    aria-label={t('pipeline.ll.rating')}
                    data={['1', '2', '3', '4', '5']}
                    value={a.rating ? String(a.rating) : null}
                    disabled={readOnly || ll.pending}
                    onChange={(v) => void ll.patch(a, { rating: v ? Number(v) : null })}
                  />
                </Table.Td>
                <Table.Td>
                  <RationaleCell key={`${a.id}-${a.version}`} app={a} disabled={readOnly || ll.pending} onSave={(v) => void ll.patch(a, { rationale: v || null })} />
                </Table.Td>
                <Table.Td>
                  <Button size="compact-sm" variant="default" disabled={readOnly} onClick={() => setAsk(a)}>
                    {t('pipeline.ll.toShortlist')}
                  </Button>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
      <ShortlistDialog app={ask} onClose={() => setAsk(undefined)} onConfirm={(rating, rationale) => ll.toShortlist(ask!, rating, rationale)} />
    </Panel>
  );
}
