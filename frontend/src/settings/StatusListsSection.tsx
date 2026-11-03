import { Box, Group, Stack, Tabs, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { SkeletonRows } from '../shell/components/SkeletonRows';
import { ToneBadge } from '../shell/components/ToneBadge';
import { useReference, type LifecycleKind } from '../shell/refdata/useReference';

const KINDS: LifecycleKind[] = ['account', 'proposal', 'jobOrder', 'pipelineStage', 'consent', 'dataSubjectRequest'];

/** Read-only status lists from GET /reference-data (ADR 0002): no edit controls by design. */
export function StatusListsSection() {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const tr = i18n.language.startsWith('tr');
  if (!ref.data) return <SkeletonRows n={4} />;
  const data = ref.data;
  return (
    <Stack>
      <Text c="dimmed" size="sm">{t('settings.statuses.hint')}</Text>
      <Tabs defaultValue="account">
        <Tabs.List style={{ flexWrap: 'wrap' }}>
          {KINDS.map((k) => (
            <Tabs.Tab key={k} value={k}>{t(`settings.statuses.lifecycle.${k}`)}</Tabs.Tab>
          ))}
        </Tabs.List>
        {KINDS.map((k) => (
          <Tabs.Panel key={k} value={k} pt="sm">
            <Stack gap="xs">
              {/* Rows divided by a 1 px line, not cards: the list already sits inside the section panel. */}
              {data.lifecycles[k].statuses.map((s, i) => (
                <Box key={s.value} py="xs" style={{ borderBottom: '1px solid var(--color-border)' }}>
                  <Group gap="sm" align="flex-start">
                    <Text w={24}>{i + 1}</Text>
                    <ToneBadge size="sm" tone={s.tone} label={tr ? s.labelTr : s.labelEn} />
                    <Text c="dimmed" size="sm">{s.value}{s.terminal ? ` (${t('settings.statuses.terminal')})` : ''}</Text>
                    <Text size="sm">{s.next.map((n) => ref.label(k, n.to) + (n.reasonRequired ? ' *' : '') + (n.managerOnly ? ' M' : '')).join(', ')}</Text>
                  </Group>
                </Box>
              ))}
              <Text size="xs" c="dimmed">* {t('settings.statuses.reason')}; M = {t('settings.statuses.manager')}</Text>
            </Stack>
          </Tabs.Panel>
        ))}
      </Tabs>
    </Stack>
  );
}
