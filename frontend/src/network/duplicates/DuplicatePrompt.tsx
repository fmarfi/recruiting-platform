// "Possible duplicate" prompt: shown before a person is created. Use existing (open that person) or create anyway.
import { Alert, Anchor, Badge, Button, Group, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge } from '../../pipeline/shared';
import { REASON_ORDER, type Match } from './duplicateLogic';

export function ReasonBadges({ reasons }: { reasons: Match['reasons'] }) {
  const { t } = useTranslation();
  return (
    <Group gap={4}>
      {REASON_ORDER.filter((r) => reasons.includes(r)).map((r) => (
        <Badge key={r} size="sm" variant="light" color="orange" tt="none">
          {t(`network.dup.reason.${r}`)}
        </Badge>
      ))}
    </Group>
  );
}

export function DuplicatePrompt({ matches, onCreateAnyway, onBack, busy }: { matches: Match[]; onCreateAnyway: () => void; onBack: () => void; busy: boolean }) {
  const { t } = useTranslation();
  return (
    <Alert color="orange" title={t('network.dup.promptTitle')} role="alert" data-testid="duplicate-prompt">
      <Stack gap="xs">
        <Text size="sm">{t('network.dup.promptBody', { count: matches.length })}</Text>
        {matches.map((m) => (
          <Group key={m.person.id} justify="space-between" wrap="nowrap" align="flex-start">
            <Stack gap={2}>
              <Anchor component={Link} to={`/people/${m.person.id}`} fw={600} target="_blank">
                {m.person.name}
              </Anchor>
              <Text size="xs" c="dimmed">
                {[m.person.title, m.person.company, m.person.city].filter(Boolean).join(', ')}
              </Text>
              <ReasonBadges reasons={m.reasons} />
            </Stack>
            <Group gap="xs" wrap="nowrap">
              <StatusBadge lifecycle="consent" value={m.person.consentStatus} />
              <Button size="compact-sm" component={Link} to={`/people/${m.person.id}`}>
                {t('network.dup.useExisting')}
              </Button>
            </Group>
          </Group>
        ))}
        <Group justify="flex-end">
          <Button variant="default" onClick={onBack}>
            {t('network.dup.back')}
          </Button>
          <Button color="orange" variant="light" loading={busy} onClick={onCreateAnyway}>
            {t('network.dup.createAnyway')}
          </Button>
        </Group>
      </Stack>
    </Alert>
  );
}
