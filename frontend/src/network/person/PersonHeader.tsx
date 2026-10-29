// Person header: name, type, contact data, owner, last contact and the consent status badge (S-01).
import { Anchor, Avatar, Badge, Button, Card, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { StatusBadge, useFormat, type Schemas } from '../../pipeline/shared';
import { canResendConsent } from './profile';

export function PersonHeader({
  person,
  onLog,
  onAction,
  onResend,
  onEdit,
  resending = false,
}: {
  person: Schemas['Person'];
  onLog: () => void;
  onAction: () => void;
  onResend: () => void;
  onEdit: () => void;
  resending?: boolean;
}) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const days = person.lastContactAt ? Math.floor((Date.now() - new Date(person.lastContactAt).getTime()) / 86_400_000) : undefined;
  return (
    <Card withBorder mb="md">
      <Group justify="space-between" align="flex-start">
        <Group align="flex-start">
          <Avatar color="blue">{person.name.split(' ').map((w) => w[0]).slice(0, 2).join('')}</Avatar>
          <Stack gap={2}>
            <Group gap="xs">
              <Title order={1} size="h3" data-testid="screen-title">
                {person.name}
              </Title>
              <Text size="sm" c="dimmed">
                {t(`network.types.${person.type}`)}
              </Text>
            </Group>
            <Text size="sm">{[person.title, person.company, person.city].filter(Boolean).join(' - ')}</Text>
            <Group gap="sm">
              {person.email && <Anchor href={`mailto:${person.email}`} size="sm">{person.email}</Anchor>}
              {person.phone && <Anchor href={`tel:${person.phone}`} size="sm">{person.phone}</Anchor>}
              {person.linkedinUrl && <Anchor href={person.linkedinUrl} target="_blank" rel="noreferrer" size="sm">LinkedIn</Anchor>}
            </Group>
            <Text size="sm" c="dimmed">
              {t('network.fields.owner')}: {person.owner.name}
            </Text>
            {!!person.tags?.length && (
              <Group gap={4} aria-label={t('network.pool.tags')}>
                {person.tags.map((tag) => (
                  <Badge key={tag} variant="light" size="sm" tt="none">
                    {tag}
                  </Badge>
                ))}
              </Group>
            )}
            {person.howWeKnow && (
              <Text size="sm">
                {t('network.fields.howWeKnow')}: {person.howWeKnow}
              </Text>
            )}
            <Group gap="sm">
              <Text size="sm">
                {t('network.fields.lastContact')}: {person.lastContactAt ? `${fmt.dateLong(person.lastContactAt)} (${t('network.daysShort', { count: days })})` : t('network.never')}
              </Text>
              <Text size="sm">{t('network.privacy')}:</Text>
              <StatusBadge lifecycle="consent" value={person.consentStatus} testId="consent-badge" />
              {person.shareable === false && person.consentStatus !== 'GIVEN' && (
                <Text size="xs" c="dimmed">
                  {t('network.profile.notShareable')}
                </Text>
              )}
              {canResendConsent(person.consentStatus, !!person.email, person.anonymised) && (
                <Button size="compact-sm" variant="light" loading={resending} onClick={onResend} data-testid="consent-resend">
                  {t(person.consentStatus === 'NOT_SENT' ? 'network.profile.sendConsentNow' : 'network.profile.resendConsent')}
                </Button>
              )}
            </Group>
          </Stack>
        </Group>
        <Group gap="xs">
          <Button variant="subtle" onClick={onEdit} disabled={person.anonymised} data-testid="person-edit">
            {t('network.edit.action')}
          </Button>
          <Button onClick={onLog}>{t('network.logActivity')}</Button>
          <Button variant="default" onClick={onAction}>
            {t('network.addAction')}
          </Button>
        </Group>
      </Group>
    </Card>
  );
}
