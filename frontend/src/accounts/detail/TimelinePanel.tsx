import { Select, Stack, Text, Title } from '@mantine/core';
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
      <Title order={2} size="h3">
        {t('accounts.tab.timeline')}
      </Title>
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
        <Stack gap={0} component="ol" m={0} p={0} style={{ listStyle: 'none', maxHeight: 640, overflowY: 'auto' }}>
        {entries.map((e) => (
          <li key={e.id} style={{ borderLeft: '2px solid var(--color-border)', marginLeft: 5, padding: '0 0 14px 14px', position: 'relative' }}>
            <span aria-hidden style={{ position: 'absolute', left: -6, top: 4, width: 10, height: 10, borderRadius: '50%', background: e.kind === 'ACTIVITY' ? 'var(--color-accent)' : 'var(--color-borderStrong)', border: '2px solid var(--color-surface)' }} />
            <Text size="xs" c="dimmed">
              {fmtDate(e.occurredAt, i18n.language)} - {e.actor?.name ?? t('accounts.system')}
            </Text>
            <Text size="sm">{text(e)}</Text>
            {e.reason && (
              <Text size="xs" c="dimmed">
                {e.reason}
              </Text>
            )}
          </li>
        ))}
        </Stack>
      )}
    </Stack>
  );
}
