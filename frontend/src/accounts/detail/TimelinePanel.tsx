import { Select, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { EmptyState } from '../../shell/components/EmptyState';
import { fmtDate } from '../../shell/lib/format';
import { useReference } from '../../shell/refdata/useReference';

type Entry = components['schemas']['TimelineEntry'];
const KINDS = ['ACCOUNT_STATUS', 'ACTIVITY', 'PROPOSAL', 'JOB_ORDER', 'CONTACT', 'CANDIDATE'];

interface Props {
  entries: Entry[];
  kind: string;
  onKind: (k: string) => void;
}

/** Timeline (A-08). Text is built from kind and statuses, not from the English `summary`. */
export function TimelinePanel({ entries, kind, onKind }: Props) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const text = (e: Entry) => {
    if (e.kind === 'ACCOUNT_STATUS' && e.toStatus)
      return t('accounts.tl.ACCOUNT_STATUS', { to: ref.label('account', e.toStatus) });
    if (e.kind === 'PROPOSAL' && e.toStatus) return t('accounts.tl.PROPOSAL', { to: ref.label('proposal', e.toStatus) });
    if (e.kind === 'JOB_ORDER' && e.jobOrder) return t('accounts.tl.JOB_ORDER', { title: e.jobOrder.title });
    if (e.kind === 'CANDIDATE' && e.person) return t('accounts.tl.CANDIDATE', { name: e.person.name });
    return e.summary;
  };
  return (
    <Stack gap="xs">
      <Select
        aria-label={t('accounts.tab.timeline')}
        value={kind || 'ALL'}
        allowDeselect={false}
        onChange={(v) => onKind(v && v !== 'ALL' ? v : '')}
        data={[{ value: 'ALL', label: t('accounts.all') }, ...KINDS.map((k) => ({ value: k, label: t(`accounts.kind.${k}`) }))]}
      />
      {entries.length === 0 ? (
        <EmptyState title={t('accounts.emptyTimeline')} />
      ) : (
        entries.map((e) => (
          <div key={e.id}>
            <Text size="xs" c="dimmed">
              {fmtDate(e.occurredAt, i18n.language)} - {e.actor?.name ?? t('accounts.system')}
            </Text>
            <Text size="sm">{text(e)}</Text>
            {e.reason && (
              <Text size="xs" c="dimmed">
                {e.reason}
              </Text>
            )}
          </div>
        ))
      )}
    </Stack>
  );
}
