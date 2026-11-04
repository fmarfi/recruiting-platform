import { Button, Group, Select, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { components } from '../../api';
import { ActivityIcon, CandidateIcon, ClientIcon, ContactIcon, FeeIcon, ICON_SIZE, ICON_STROKE, SearchIcon } from '../../shell/components/objectIcons';
import { fmtDate } from '../../shell/lib/format';
import { useReference } from '../../shell/refdata/useReference';
import { SectionEmpty } from './SectionEmpty';

type Entry = components['schemas']['TimelineEntry'];
const KINDS = ['ACCOUNT_STATUS', 'ACTIVITY', 'PROPOSAL', 'JOB_ORDER', 'CONTACT', 'CANDIDATE'];
/** One object icon per activity type (visual system section 6), shown in ink3. */
const KIND_ICON: Record<string, typeof ClientIcon> = {
  ACCOUNT_STATUS: ClientIcon,
  ACTIVITY: ActivityIcon,
  PROPOSAL: FeeIcon,
  JOB_ORDER: SearchIcon,
  CONTACT: ContactIcon,
  CANDIDATE: CandidateIcon,
};

/** The panel shows the latest entries; the rest sit behind View all. */
const LATEST = 10;

const dayKey = (d: Date) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;

interface Props {
  entries: Entry[];
  kind: string;
  onKind: (k: string) => void;
  onLog?: () => void;
}

/** Activity timeline (A-08), grouped by day, shown inside the Activity panel. Text is built from kind and statuses, not from the English `summary`. */
export function TimelinePanel({ entries, kind, onKind, onLog }: Props) {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const [showAll, setShowAll] = useState(false);
  const shown = showAll ? entries : entries.slice(0, LATEST);
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
  for (const e of shown) {
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
      <Select
        aria-label={t('accounts.tab.timeline')}
        value={kind || 'ALL'}
        allowDeselect={false}
        onChange={(v) => onKind(v && v !== 'ALL' ? v : '')}
        data={[{ value: 'ALL', label: t('accounts.all') }, ...KINDS.map((k) => ({ value: k, label: t(`accounts.kind.${k}`) }))]}
      />
      {entries.length === 0 ? (
        <SectionEmpty text={t('accounts.emptyTimeline')} action={onLog && <Button size="sm" variant="default" onClick={onLog}>{t('accounts.logActivity')}</Button>} />
      ) : (
        <Stack gap="md">
          {[...groups.values()].map((g) => (
            <section key={g.key} aria-label={g.label}>
              <Text size="xs" c="var(--color-ink3)" fw={500} style={{ position: 'sticky', top: 0, zIndex: 1, background: 'var(--color-surface)', paddingBlock: 'var(--space-1)' }}>
                {g.label}
              </Text>
              <Stack component="ol" gap={0} m={0} p={0} style={{ listStyle: 'none' }}>
                {g.items.map((e) => {
                  const Icon = KIND_ICON[e.kind] ?? ActivityIcon;
                  return (
                    <li key={e.id} style={{ display: 'flex', gap: 'var(--space-3)', padding: 'var(--space-2) 0' }}>
                      <Icon size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden style={{ color: 'var(--color-ink3)', flex: 'none', marginTop: 2 }} />
                      <div style={{ minWidth: 0 }}>
                        <Text size="sm" c="var(--color-ink2)">
                          {text(e)}
                        </Text>
                        <Text size="xs" c="dimmed">
                          {e.actor?.name ?? t('accounts.system')} - <time dateTime={e.occurredAt}>{new Date(e.occurredAt).toLocaleTimeString(i18n.language, { hour: '2-digit', minute: '2-digit' })}</time>
                        </Text>
                        {e.reason && (
                          <Text size="xs" c="dimmed">
                            {e.reason}
                          </Text>
                        )}
                      </div>
                    </li>
                  );
                })}
              </Stack>
            </section>
          ))}
          {!showAll && entries.length > LATEST && (
            <Group>
              <Button size="compact-sm" variant="subtle" onClick={() => setShowAll(true)}>
                {t('accounts.timelineViewAll')}
              </Button>
            </Group>
          )}
        </Stack>
      )}
    </Stack>
  );
}
