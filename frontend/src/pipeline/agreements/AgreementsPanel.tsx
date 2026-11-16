// Agreements table: on the client page (all of the client's agreements) and on the search page (that position's). Table-first; row opens the drawer.
import { Anchor, Button, Text } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { IconFileTypePdf, IconHistory, IconPlus } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api, type components } from '../../api';
import { EmptyState } from '../../shell/components/EmptyState';
import { ToneBadge } from '../../shell/components/ToneBadge';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';
import { Panel } from '../../shell/components/Panel';
import { DataGrid, type ColumnDef } from '../../shell/grid';
import { useAsync } from '../../shell/hooks/useAsync';
import { fmtDate } from '../../shell/lib/format';
import { tokens } from '../../theme/tokens';
import { AgreementDrawer, type AgreementPreset } from './AgreementDrawer';
import { HistoryDialog } from './parts';

type Agreement = components['schemas']['Agreement'];
type Status = components['schemas']['AgreementStatus'];

/** The reference data has no agreement lifecycle yet, so the tone per status is local (change request in the hand-off note). */
const TONE: Record<Status, string> = { DRAFT: 'NEUTRAL', SENT: 'INFO', SIGNED: 'GOOD', EXPIRED: 'MUTED', TERMINATED: 'BAD' };

export function AgreementStatusBadge({ status }: { status: Status }) {
  const { t } = useTranslation();
  return <ToneBadge tone={TONE[status]} label={t(`agr.status.${status}`)} />;
}

/** Agreements of a client (`accountId`) or of one position (`jobOrderId`). */
export function useAgreements(q: { accountId?: string; jobOrderId?: string }) {
  return useAsync(() => api.GET('/agreements', { params: { query: { ...q, size: 100, sort: ['createdAt,desc'] } } }), [q.accountId, q.jobOrderId]);
}

export function feeSummary(a: Agreement, tx: (k: string) => string): string {
  if (a.feeModel === 'FLAT_FEE') return a.flatFeeAmount ? `${a.flatFeeAmount} ${a.currency}` : '-';
  const pct = a.feePercent ? `${a.feePercent}%` : '';
  return [tx(`agr.model.${a.feeModel}`), pct].filter(Boolean).join(', ');
}

interface Props {
  rows: Agreement[] | undefined;
  loading: boolean;
  error: boolean;
  reload: () => void;
  preset: AgreementPreset;
  /** Show the position column (client page). */
  showPosition?: boolean;
  onChanged?: () => void;
}

export function AgreementsPanel({ rows, loading, error, reload, preset, showPosition, onChanged }: Props) {
  const { t, i18n } = useTranslation();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const [editId, setEditId] = useState<string | 'new' | null>(null);
  const [history, setHistory] = useState<Agreement>();
  const editing = rows?.find((r) => r.id === editId);
  const done = () => {
    reload();
    onChanged?.();
  };
  const date = (d?: string | null) => fmtDate(d, i18n.language);
  const add = (
    <Button size="compact-md" variant="default" leftSection={<IconPlus size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />} onClick={() => setEditId('new')}>
      {t('agr.new')}
    </Button>
  );
  const columns: ColumnDef<Agreement>[] = [
    {
      id: 'title',
      header: t('agr.col.agreement'),
      locked: true,
      minWidth: 200,
      accessor: (a) => a.title || t(`agr.kind.${a.kind}`),
      cell: (a) => (
        <div>
          <Text fz="sm" fw={500} c="var(--color-ink)">
            {a.title || t(`agr.kind.${a.kind}`)}
          </Text>
          <Text fz="0.8125rem" c="var(--color-ink3)">
            {t(`agr.kind.${a.kind}`)}
          </Text>
        </div>
      ),
    },
    ...(showPosition
      ? [
          {
            id: 'position',
            header: t('agr.position'),
            accessor: (a: Agreement) => a.jobOrder?.title ?? t('agr.wholeClient'),
            cell: (a: Agreement) =>
              a.jobOrder ? (
                <Anchor component={Link} to={`/job-orders/${a.jobOrder.id}`} size="sm" onClick={(e) => e.stopPropagation()}>
                  {a.jobOrder.title}
                </Anchor>
              ) : (
                <Text fz="sm" c="var(--color-ink3)">
                  {t('agr.wholeClient')}
                </Text>
              ),
          } satisfies ColumnDef<Agreement>,
        ]
      : []),
    { id: 'status', header: t('agr.f.status'), accessor: (a) => t(`agr.status.${a.status}`), cell: (a) => <AgreementStatusBadge status={a.status ?? 'DRAFT'} /> },
    { id: 'fee', header: t('agr.col.fee'), accessor: (a) => feeSummary(a, t as never), cell: (a) => <Text fz="sm">{feeSummary(a, t as never)}</Text> },
    { id: 'signedOn', header: t('agr.f.signedOn'), accessor: (a) => date(a.signedOn), numeric: true },
    { id: 'validUntil', header: t('agr.f.validUntil'), accessor: (a) => date(a.validUntil), numeric: true, defaultVisible: false },
    {
      id: 'file',
      header: t('agr.col.file'),
      accessor: (a) => a.signedFile?.fileName ?? '',
      cell: (a) =>
        a.signedFile ? (
          <Text fz="sm" c="var(--color-ink2)" style={{ display: 'inline-flex', gap: 4, alignItems: 'center' }} title={a.signedFile.fileName}>
            <IconFileTypePdf size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />
            {t('agr.col.hasFile')}
          </Text>
        ) : (
          <Text fz="sm" c="var(--color-ink3)">
            -
          </Text>
        ),
    },
    {
      id: 'updated',
      header: t('agr.col.updated'),
      accessor: (a) => date(a.updatedAt),
      numeric: true,
      defaultVisible: false,
    },
    {
      id: 'history',
      header: t('agr.history'),
      locked: true,
      cell: (a) => (
        <Button size="compact-sm" variant="subtle" leftSection={<IconHistory size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />} onClick={(e) => { e.stopPropagation(); setHistory(a); }}>
          {t('agr.history')}
        </Button>
      ),
    },
  ];

  return (
    <Panel title={t('agr.title')} count={rows?.length} testId="agreements" padding="none" actions={rows && rows.length > 0 ? add : undefined}>
      <DataGrid<Agreement>
        gridId={showPosition ? 'agreements-client' : 'agreements-job'}
        columns={columns}
        rows={error ? undefined : rows}
        rowId={(a) => a.id}
        loading={loading}
        error={error}
        onRetry={reload}
        phone={!!phone}
        onRowClick={(a) => setEditId(a.id)}
        exportName="agreements"
        empty={<EmptyState title={t('agr.emptyTitle')} hint={preset.fixedJob ? t('agr.emptyJob') : t('agr.emptyClient')} actions={add} />}
      />
      <AgreementDrawer
        opened={editId === 'new' || !!editing}
        onClose={() => setEditId(null)}
        onSaved={done}
        agreement={editId === 'new' ? undefined : editing}
        preset={preset}
        onHistory={editing ? () => setHistory(editing) : undefined}
      />
      <HistoryDialog target={history && { type: 'AGREEMENT', id: history.id, name: history.title || t(`agr.kind.${history.kind}`) }} onClose={() => setHistory(undefined)} />
    </Panel>
  );
}
