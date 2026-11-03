import { Button, Select, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { fmtDate } from '../../shell/lib/format';
import { useReference } from '../../shell/refdata/useReference';
import { SectionEmpty } from './SectionEmpty';

type Entry = components['schemas']['TimelineEntry'];
const KINDS = ['ACCOUNT_STATUS', 'ACTIVITY', 'PROPOSAL', 'JOB_ORDER', 'CONTACT', 'CANDIDATE'];
/** One colour per activity type, so the dot alone tells the types apart. */
const DOT: Record<string, string> = {
  ACCOUNT_STATUS: 'var(--color-brass)',
  ACTIVITY: 'var(--color-accent)',
  PROPOSAL: 'var(--color-info)',
  JOB_ORDER: 'var(--color-success)',
  CONTACT: 'var(--color-ink3)',
  CANDIDATE: 'var(--color-warning)',
};

const dayKey = (d: Date) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;

interface Props {
  entries: Entry[];
  kind: string;
  onKind: (k: string) => void;
  onLog?: () => void;
}

/** Timeline (A-08), grouped by day. Text is built from kind and statuses, not from the English `summary`. */
export function TimelinePanel({ entries, kind, onKind, onLog }: Props) {
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

  const now = new Date();
  const today = dayKey(now);
  const yesterday = dayKey(new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1));
  const groups = new Map<string, { key: string; label: string; items: Entry[] }>();
  for (const e of entries) {
    const key = dayKey(new Date(e.occurredAt));
    let g = groups.get(key);
    if (!g) {
      const label = key === today ? t('accounts.day.today') : key === yesterday ? t('accounts.day.yesterday') : fmtDate(e.occurredAt, i18n.language);
      g = { key, label, items: [] };
      groups.set(key, g);
    }
    g.items.push(e);
  }

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
        <SectionEmpty text={t('accounts.emptyTimeline')} action={onLog && <Button size="compact-sm" variant="light" onClick={onLog}>{t('accounts.logActivity')}</Button>} />
      ) : (
        <Stack gap="md" style={{ maxHeight: 640, overflowY: 'auto' }}>
          {[...groups.values()].map((g) => (
            <section key={g.key} aria-label={g.label}>
              <Text size="xs" c="dimmed" fw={600} style={{ position: 'sticky', top: 0, zIndex: 1, background: 'var(--color-surface)', paddingBlock: 'var(--space-1)' }}>
                {g.label}
              </Text>
              <Stack component="ol" gap={0} m={0} p={0} style={{ listStyle: 'none' }}>
                {g.items.map((e) => (
                  <li key={e.id} style={{ position: 'relative', borderLeft: 'thin solid var(--color-border)', marginLeft: 'var(--space-1)', padding: '0 0 var(--space-4) var(--space-4)' }}>
                    <span aria-hidden style={{ position: 'absolute', left: 'calc(var(--badge-dot) * -0.5)', top: 'var(--space-1)', width: 'var(--badge-dot)', height: 'var(--badge-dot)', borderRadius: '50%', background: DOT[e.kind] ?? 'var(--color-ink3)', boxShadow: '0 0 0 var(--space-1) var(--color-surface)' }} />
                    <Text size="xs" c="dimmed">
                      {e.actor?.name ?? t('accounts.system')} - <time dateTime={e.occurredAt}>{new Date(e.occurredAt).toLocaleTimeString(i18n.language, { hour: '2-digit', minute: '2-digit' })}</time>
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
            </section>
          ))}
        </Stack>
      )}
    </Stack>
  );
}
