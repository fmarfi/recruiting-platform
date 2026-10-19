// Person header: name and actions, consent strip and the highlights panel (S-01, lightning-style P4).
import { Anchor, Avatar, Button, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Path } from '../../common/path/Path';
import { Highlights } from '../../common/record/Highlights';
import { StatusBadge, useFormat, type Schemas } from '../../pipeline/shared';

export function PersonHeader({
  person,
  onLog,
  onAction,
}: {
  person: Schemas['Person'];
  onLog: () => void;
  onAction: () => void;
}) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const days = person.lastContactAt ? Math.floor((Date.now() - new Date(person.lastContactAt).getTime()) / 86_400_000) : undefined;
  return (
    <>
      <Group justify="space-between" align="flex-start" mb="sm">
        <Group align="flex-start">
          <Avatar color="blue">{person.name.split(' ').map((w) => w[0]).slice(0, 2).join('')}</Avatar>
          <div>
            <Title order={1} size="h3" data-testid="screen-title">
              {person.name}
            </Title>
            <Text size="sm" c="dimmed">
              {t(`network.types.${person.type}`)}
            </Text>
          </div>
        </Group>
        <Group gap="xs">
          <Button onClick={onLog}>{t('network.logActivity')}</Button>
          <Button variant="default" onClick={onAction}>
            {t('network.addAction')}
          </Button>
        </Group>
      </Group>
      {/* Consent as a read-only strip; sending the request stays on the Privacy tab. */}
      <div style={{ marginBottom: 12 }} data-testid="consent-path" data-value={person.consentStatus}>
        <Path lifecycle="consent" value={person.consentStatus} isManager={false} disabled onChoose={() => undefined} />
      </div>
      <Highlights
        items={[
          { label: t('network.fields.type'), value: t(`network.types.${person.type}`) },
          { label: t('network.fields.titleCompany'), value: [person.title, person.company].filter(Boolean).join(', ') },
          { label: t('network.fields.city'), value: person.city },
          { label: t('network.fields.owner'), value: person.owner.name },
          {
            label: t('network.fields.lastContact'),
            value: person.lastContactAt
              ? `${fmt.dateLong(person.lastContactAt)} (${t('network.daysShort', { count: days })})`
              : t('network.never'),
          },
          { label: t('network.fields.consent'), value: <StatusBadge lifecycle="consent" value={person.consentStatus} testId="consent-badge" /> },
          {
            label: t('network.fields.email'),
            value: (
              <Stack gap={0}>
                {person.email && (
                  <Anchor href={`mailto:${person.email}`} size="sm">
                    {person.email}
                  </Anchor>
                )}
                {person.phone && (
                  <Anchor href={`tel:${person.phone}`} size="sm">
                    {person.phone}
                  </Anchor>
                )}
                {person.linkedinUrl && (
                  <Anchor href={person.linkedinUrl} target="_blank" rel="noreferrer" size="sm">
                    LinkedIn
                  </Anchor>
                )}
              </Stack>
            ),
          },
        ]}
      />
      {person.howWeKnow && (
        <Text size="sm" mb="md">
          {t('network.fields.howWeKnow')}: {person.howWeKnow}
        </Text>
      )}
    </>
  );
}
