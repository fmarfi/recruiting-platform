import { Card, Group, Stack, Tabs, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { ToneBadge } from '../shell/components/ToneBadge';
import { useReference, type LifecycleKind } from '../shell/refdata/useReference';

const KINDS: LifecycleKind[] = ['account', 'proposal', 'jobOrder', 'pipelineStage', 'consent', 'dataSubjectRequest'];

/** Read-only status lists from GET /reference-data (ADR 0002): no edit controls by design. */
export function StatusListsSection() {
  const { t, i18n } = useTranslation();
  const ref = useReference();
  const tr = i18n.language.startsWith('tr');
  if (!ref.data) return null;
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
              {data.lifecycles[k].statuses.map((s, i) => (
                <Card key={s.value} withBorder padding="sm">
                  <Group gap="sm" align="flex-start">
                    <Text w={24}>{i + 1}</Text>
                    <ToneBadge size="sm" tone={s.tone} label={tr ? s.labelTr : s.labelEn} />
                    <Text c="dimmed" size="sm">{s.value}{s.terminal ? ` (${t('settings.statuses.terminal')})` : ''}</Text>
                    <Text size="sm">{s.next.map((n) => ref.label(k, n.to) + (n.reasonRequired ? ' *' : '') + (n.managerOnly ? ' M' : '')).join(', ')}</Text>
                  </Group>
                </Card>
              ))}
              <Text size="xs" c="dimmed">* {t('settings.statuses.reason')}; M = {t('settings.statuses.manager')}</Text>
            </Stack>
          </Tabs.Panel>
        ))}
      </Tabs>
    </Stack>
  );
}
