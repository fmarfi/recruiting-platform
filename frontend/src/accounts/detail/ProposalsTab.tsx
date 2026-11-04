import { Group, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { StatusMenu } from '../../shell/components/StatusMenu';
import { fmtDate } from '../../shell/lib/format';
import { useReference } from '../../shell/refdata/useReference';
import { ReasonDialog } from '../components/ReasonDialog';
import { StatusFailureAlert } from '../components/StatusFailureAlert';
import { AddButton } from './AddButton';
import { SectionEmpty } from './SectionEmpty';
import { useStatusChange } from './useStatusChange';

type Proposal = components['schemas']['Proposal'];

function ProposalRow({ p, onChanged }: { p: Proposal; onChanged: () => void }) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const change = useStatusChange(
    (status, reason) => api.PATCH('/proposals/{id}/status', { params: { path: { id: p.id } }, body: { status: status as Proposal['status'], reason, version: p.version } }),
    onChanged,
    t('shell.saveError'),
  );
  return (
    <div>
      <Group justify="space-between" wrap="nowrap">
        <div>
          <Text fw={500}>{p.title}</Text>
          <Text size="sm" c="dimmed">
            {[p.feeAmount ? `${p.feeAmount} ${p.currency ?? ''}` : null, p.sentOn ? fmtDate(p.sentOn, i18n.language) : null].filter(Boolean).join(' - ')}
          </Text>
        </div>
        <StatusMenu kind="proposal" value={p.status} size="md" onMove={change.move} />
      </Group>
      {change.failure && <StatusFailureAlert failure={change.failure} />}
      <ReasonDialog opened={!!change.pending} target={change.pending ? ref.label('proposal', change.pending.to) : ''} onConfirm={change.confirm} onCancel={change.cancel} />
    </div>
  );
}

export function ProposalsTab({ proposals, onChanged, onAdd }: { proposals: Proposal[]; onChanged: () => void; onAdd: () => void }) {
  const { t } = useTranslation();
  if (proposals.length === 0) return <SectionEmpty text={t('accounts.emptyProposals')} action={<AddButton label={t('accounts.newProposal')} onClick={onAdd} />} />;
  return (
    <Stack gap="sm">
      {proposals.map((p) => (
        <ProposalRow key={p.id} p={p} onChanged={onChanged} />
      ))}
    </Stack>
  );
}
