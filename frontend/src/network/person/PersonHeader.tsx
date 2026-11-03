// Person header: identity, one dimmed line for title and company, a compact fact row (S-01) and up to 2 visible actions.
import { Anchor, Avatar, Badge, Button, Card, Group, Menu, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { StatusBadge, useFormat, type Schemas } from '../../pipeline/shared';
import { canResendConsent } from './profile';

type Action = { key: string; label: string; onClick: () => void; variant?: 'default' | 'subtle' | 'light'; disabled?: boolean; testId?: string };

function Fact({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <Stack gap={0} style={{ minWidth: 0 }}>
      <Text size="xs" c="dimmed">
        {label}
      </Text>
      <div>{children}</div>
    </Stack>
  );
}

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
  const initials = person.name
    .split(' ')
    .filter(Boolean)
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
  const subline = [person.title, person.company].filter(Boolean).join(', ');

  // Primary action first; more than 3 actions in total keeps 2 visible and moves the rest into the "..." menu.
  const actions: Action[] = [
    { key: 'log', label: t('network.logActivity'), onClick: onLog },
    { key: 'action', label: t('network.addAction'), onClick: onAction, variant: 'default' },
    { key: 'edit', label: t('network.edit.action'), onClick: onEdit, variant: 'subtle', disabled: person.anonymised, testId: 'person-edit' },
  ];
  if (canResendConsent(person.consentStatus, !!person.email, person.anonymised)) {
    actions.push({
      key: 'resend',
      label: t(person.consentStatus === 'NOT_SENT' ? 'network.profile.sendConsentNow' : 'network.profile.resendConsent'),
      onClick: onResend,
      variant: 'light',
      disabled: resending,
      testId: 'consent-resend',
    });
  }
  const overflow = actions.length > 3;
  const visible = overflow ? actions.slice(0, 2) : actions;
  const menu = overflow ? actions.slice(2) : [];

  return (
    <Card withBorder padding="lg" mb="md">
      <Group justify="space-between" align="flex-start" wrap="wrap" gap="md">
        <Group align="flex-start" gap="md" wrap="nowrap" style={{ minWidth: 0, flex: '1 1 auto' }}>
          <Avatar size="lg" radius="md" aria-hidden style={{ background: 'var(--color-accent)', color: 'var(--color-accentInk)', fontFamily: 'var(--font-display)', fontWeight: 600 }}>
            {initials}
          </Avatar>
          <Stack gap="xs" style={{ minWidth: 0 }}>
            <Stack gap={0}>
              <Group gap="xs" align="baseline">
                <Title order={1} data-testid="screen-title">
                  {person.name}
                </Title>
                <Text size="sm" c="dimmed">
                  {t(`network.types.${person.type}`)}
                </Text>
              </Group>
              {subline && (
                <Text size="sm" c="dimmed">
                  {subline}
                </Text>
              )}
            </Stack>

            <Group gap="xl" align="flex-end">
              <Fact label={t('network.fields.consent')}>
                <Group gap="xs">
                  <StatusBadge lifecycle="consent" value={person.consentStatus} testId="consent-badge" />
                  {person.shareable === false && person.consentStatus !== 'GIVEN' && (
                    <Text size="xs" c="dimmed">
                      {t('network.profile.notShareable')}
                    </Text>
                  )}
                </Group>
              </Fact>
              <Fact label={t('network.fields.city')}>
                <Text size="sm">{person.city || '–'}</Text>
              </Fact>
              <Fact label={t('network.fields.owner')}>
                <Text size="sm">{person.owner.name}</Text>
              </Fact>
              <Fact label={t('network.fields.lastContact')}>
                <Text size="sm">
                  {person.lastContactAt ? `${fmt.dateLong(person.lastContactAt)} (${t('network.daysShort', { count: days })})` : t('network.never')}
                </Text>
              </Fact>
            </Group>

            {(person.email || person.phone || person.linkedinUrl) && (
              <Group gap="sm">
                {person.email && <Anchor href={`mailto:${person.email}`} size="sm">{person.email}</Anchor>}
                {person.phone && <Anchor href={`tel:${person.phone}`} size="sm">{person.phone}</Anchor>}
                {person.linkedinUrl && <Anchor href={person.linkedinUrl} target="_blank" rel="noreferrer" size="sm">LinkedIn</Anchor>}
              </Group>
            )}
            {(!!person.tags?.length || person.howWeKnow) && (
              <Group gap="xs" align="center">
                {!!person.tags?.length && (
                  <Group gap={4} aria-label={t('network.pool.tags')}>
                    {person.tags.map((tag) => (
                      <Badge key={tag} variant="outline" color="gray" size="sm" tt="none">
                        {tag}
                      </Badge>
                    ))}
                  </Group>
                )}
                {person.howWeKnow && (
                  <Text size="sm" c="dimmed">
                    {t('network.fields.howWeKnow')}: {person.howWeKnow}
                  </Text>
                )}
              </Group>
            )}
          </Stack>
        </Group>

        <Group gap="xs" wrap="nowrap">
          {visible.map((a) => (
            <Button key={a.key} variant={a.variant} onClick={a.onClick} disabled={a.disabled} data-testid={a.testId}>
              {a.label}
            </Button>
          ))}
          {menu.length > 0 && (
            <Menu position="bottom-end" shadow="md" withinPortal>
              <Menu.Target>
                <Button variant="default" aria-label={t('network.personMore')}>
                  ...
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                {menu.map((a) => (
                  <Menu.Item key={a.key} onClick={a.onClick} disabled={a.disabled} data-testid={a.testId}>
                    {a.label}
                  </Menu.Item>
                ))}
              </Menu.Dropdown>
            </Menu>
          )}
        </Group>
      </Group>
    </Card>
  );
}
