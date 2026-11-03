// Activity (A-04): newest first, grouped by day; empty state invites the first log (UX-08).
import { Box, Button, Group, Stack, Text } from '@mantine/core';
import { EmptyState } from '../../shell/components/EmptyState';
import { useTranslation } from 'react-i18next';
import { useFormat, type Schemas } from '../../pipeline/shared';

type Activity = Schemas['Activity'];

// One dot colour per activity type: calls, visits and meetings are the live contact; e-mail and notes are supporting.
const DOT: Record<string, string> = {
  CALL: 'var(--color-accent)',
  VISIT: 'var(--color-accent)',
  MEETING: 'var(--color-accent)',
  EMAIL: 'var(--color-brass)',
  NOTE: 'var(--color-ink3)',
};

const dayKey = (d: Date) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;

export function ActivityTab({ activities, onLog }: { activities: Activity[]; onLog: () => void }) {
  const { t, i18n } = useTranslation();
  const fmt = useFormat();
  if (activities.length === 0) {
    return <EmptyState title={t('network.noActivity')} actions={<Button onClick={onLog}>+ {t('network.logActivity')}</Button>} />;
  }
  const locale = i18n.language === 'en' ? 'en-GB' : 'tr-TR';
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  const groups: { key: string; label: string; items: Activity[] }[] = [];
  for (const a of activities) {
    const d = new Date(a.occurredAt);
    const key = dayKey(d);
    let group = groups.find((g) => g.key === key);
    if (!group) {
      const label = key === dayKey(today) ? t('network.dayGroup.today') : key === dayKey(yesterday) ? t('network.dayGroup.yesterday') : fmt.dateLong(a.occurredAt);
      group = { key, label, items: [] };
      groups.push(group);
    }
    group.items.push(a);
  }

  return (
    <Stack gap="lg">
      {groups.map((g) => (
        <section key={g.key} aria-label={g.label}>
          <Text size="sm" fw={600} mb="xs">
            {g.label}
          </Text>
          <Stack gap={0}>
            {g.items.map((a) => (
              <Group key={a.id} gap="sm" align="flex-start" wrap="nowrap" py="sm">
                <Box style={{ display: 'flex', alignItems: 'center', height: '1.5em', flexShrink: 0 }}>
                  <span
                    aria-hidden
                    data-type={a.type}
                    style={{ width: 'var(--badge-dot)', height: 'var(--badge-dot)', borderRadius: '50%', background: DOT[a.type] ?? 'var(--color-ink3)' }}
                  />
                </Box>
                <Stack gap={2} style={{ minWidth: 0, flex: 1 }}>
                  <Group gap="xs" align="baseline">
                    <Text size="sm" fw={600}>
                      {t(`quickAdd.types.${a.type}`)}
                    </Text>
                    <Text size="xs" c="dimmed" style={{ fontVariantNumeric: 'tabular-nums' }}>
                      {new Date(a.occurredAt).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })}
                    </Text>
                  </Group>
                  {a.note && (
                    <Text size="sm" style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
                      {a.note}
                    </Text>
                  )}
                  <Text size="xs" c="dimmed">
                    {a.user.name}
                  </Text>
                </Stack>
              </Group>
            ))}
          </Stack>
        </section>
      ))}
    </Stack>
  );
}
