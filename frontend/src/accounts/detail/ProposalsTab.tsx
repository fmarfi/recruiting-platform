// Proposals of a client as a table (separate from agreements, M2-UI2): status change stays in the badge menu, History per row.
import { Button, Group, Text } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { IconHistory } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';
import { StatusMenu } from '../../shell/components/StatusMenu';
import { DataGrid, type ColumnDef } from '../../shell/grid';
import { fmtDate } from '../../shell/lib/format';
import { useReference } from '../../shell/refdata/useReference';
import { HistoryDialog } from '../../pipeline/agreements/parts';
import { tokens } from '../../theme/tokens';
import { ReasonDialog } from '../components/ReasonDialog';
import { StatusFailureAlert } from '../components/StatusFailureAlert';
import { AddButton } from './AddButton';
import { useStatusChange } from './useStatusChange';

type Proposal = components['schemas']['Proposal'];

function StatusCell({ p, onChanged }: { p: Proposal; onChanged: () => void }) {
  const { t } = useTranslation();
  const ref = useReference();
  const change = useStatusChange(
    (status, reason) => api.PATCH('/proposals/{id}/status', { params: { path: { id: p.id } }, body: { status: status as Proposal['status'], reason, version: p.version } }),
    onChanged,
    t('shell.saveError'),
  );
  return (
    <div onClick={(e) => e.stopPropagation()}>
      <StatusMenu kind="proposal" value={p.status} size="md" onMove={change.move} />
      {change.failure && <StatusFailureAlert failure={change.failure} />}
      <ReasonDialog opened={!!change.pending} target={change.pending ? ref.label('proposal', change.pending.to) : ''} onConfirm={change.confirm} onCancel={change.cancel} />
    </div>
  );
}

export function ProposalsTab({ proposals, onChanged, onAdd }: { proposals: Proposal[]; onChanged: () => void; onAdd: () => void }) {
  const { t, i18n } = useTranslation();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const [history, setHistory] = useState<Proposal>();
  const date = (d?: string | null) => fmtDate(d, i18n.language);
  const columns: ColumnDef<Proposal>[] = [
    {
      id: 'title',
      header: t('accounts.proposalTitle'),
      locked: true,
      minWidth: 220,
      accessor: (p) => p.title,
      cell: (p) => (
        <div>
          <Text fz="sm" fw={500} c="var(--color-ink)">
            {p.title}
          </Text>
          {p.terms && (
            <Text fz="0.8125rem" c="var(--color-ink3)" lineClamp={1}>
              {p.terms}
            </Text>
          )}
        </div>
      ),
    },
    { id: 'status', header: t('agr.f.status'), accessor: (p) => p.status, cell: (p) => <StatusCell p={p} onChanged={onChanged} /> },
    { id: 'fee', header: t('accounts.fee'), numeric: true, accessor: (p) => (p.feeAmount ? `${p.feeAmount} ${p.currency ?? ''}`.trim() : '') },
    { id: 'sentOn', header: t('agr.col.sentOn'), numeric: true, accessor: (p) => date(p.sentOn) },
    { id: 'decidedOn', header: t('agr.col.decidedOn'), numeric: true, accessor: (p) => date(p.decidedOn) },
    { id: 'owner', header: t('pipeline.hl.owner'), accessor: (p) => p.owner.name, defaultVisible: false },
    {
      id: 'history',
      header: t('agr.history'),
      locked: true,
      cell: (p) => (
        <Button size="compact-sm" variant="subtle" leftSection={<IconHistory size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />} onClick={() => setHistory(p)}>
          {t('agr.history')}
        </Button>
      ),
    },
  ];
  return (
    <>
      <DataGrid<Proposal>
        gridId="proposals-client"
        columns={columns}
        rows={proposals}
        rowId={(p) => p.id}
        phone={!!phone}
        exportName="proposals"
        empty={
          <Group justify="center" py="lg" data-testid="empty-state">
            <Text c="dimmed">{t('accounts.emptyProposals')}</Text>
            <AddButton label={t('accounts.newProposal')} onClick={onAdd} />
          </Group>
        }
      />
      <HistoryDialog target={history && { type: 'PROPOSAL', id: history.id, name: history.title }} onClose={() => setHistory(undefined)} />
    </>
  );
}
