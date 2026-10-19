// Activity (A-04): newest first; empty state invites the first log (UX-08).
import { Button, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { useFormat, type Schemas } from '../../pipeline/shared';

export function ActivityTab({ activities, onLog }: { activities: Schemas['Activity'][]; onLog: () => void }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  if (activities.length === 0) {
    return (
      <Stack align="flex-start">
        <Text c="dimmed">{t('network.noActivity')}</Text>
        <Button onClick={onLog}>{t('network.logActivity')}</Button>
      </Stack>
    );
  }
  return (
    <Stack gap="xs">
      {activities.map((a) => (
        <Text key={a.id} size="sm">
          {fmt.date(a.occurredAt)} <b>{t(`quickAdd.types.${a.type}`)}</b> {a.note} <Text span c="dimmed">- {a.user.name}</Text>
        </Text>
      ))}
    </Stack>
  );
}
