// Privacy (S-01, S-03): consent status, retention date with the 60-day warning, consent records, send request.
import { Alert, Button, Group, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { StatusBadge, useFormat, type Schemas } from '../../pipeline/shared';

const SENDABLE = ['NOT_SENT', 'REFUSED', 'WITHDRAWN'];

export function PrivacyTab({ consent, onSend }: { consent?: Schemas['ConsentOverview']; onSend: () => void }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  if (!consent) return <Text c="dimmed">{t('network.privacyUnavailable')}</Text>;
  const daysLeft = Math.ceil((new Date(consent.retention.retainUntil).getTime() - Date.now()) / 86_400_000);
  return (
    <Stack>
      <Group>
        <StatusBadge lifecycle="consent" value={consent.status} />
        {SENDABLE.includes(consent.status) && <Button onClick={onSend}>{t('network.sendConsent')}</Button>}
      </Group>
      <Text size="sm">{t('network.retention', { date: fmt.dateLong(consent.retention.retainUntil) })}</Text>
      {daysLeft <= 60 && (
        <Alert color="yellow" data-testid="retention-warning">
          {t('network.retentionWarning')}
        </Alert>
      )}
      {consent.records.map((r) => (
        <Text key={r.id} size="sm">
          {fmt.dateLong(r.recordedAt)} - {r.decision} ({r.channel}) - {r.notice.version}
        </Text>
      ))}
    </Stack>
  );
}
