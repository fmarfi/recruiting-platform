// Person header (visual system v3, sections 5 and 6b): shared PageHeader with breadcrumb, then a fact row and contact line on the canvas. No card.
import { Anchor, Badge, Button, Group, Menu, Stack, Text } from '@mantine/core';
import { IconDots } from '@tabler/icons-react';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge, useFormat, type Schemas } from '../../pipeline/shared';
import { PageHeader } from '../../shell/components/PageHeader';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';
import { canResendConsent } from './profile';

type Action = { key: string; label: string; onClick: () => void; disabled?: boolean; testId?: string };

function Fact({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Stack gap="xs" style={{ minWidth: 0 }}>
      <Text size="xs" c="var(--color-ink3)" fw={500}>
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
  const subline = [person.title, person.company].filter(Boolean).join(', ');

  // Two visible actions: "Log activity" is the one primary, "Add action" the one secondary. Everything else sits in the More menu.
  // A consent request that was never sent is started from the consent banner (send dialog), so the menu offers a resend only after one went out.
  const menu: Action[] = [{ key: 'edit', label: t('network.edit.action'), onClick: onEdit, disabled: person.anonymised, testId: 'person-edit' }];
  if (person.consentStatus === 'SENT' && canResendConsent(person.consentStatus, !!person.email, person.anonymised)) {
    menu.push({ key: 'resend', label: t('network.profile.resendConsent'), onClick: onResend, disabled: resending, testId: 'consent-resend' });
  }

  return (
    <>
      <PageHeader
        breadcrumb={
          <Anchor component={Link} to="/network" size="sm">
            {t('nav.network')}
          </Anchor>
        }
        title={person.name}
        subtitle={subline || undefined}
        actions={
          <>
            <Menu position="bottom-end" shadow="md" withinPortal>
              <Menu.Target>
                <Button variant="default" aria-label={t('network.personMore')}>
                  <IconDots size={ICON_SIZE} stroke={ICON_STROKE} />
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
            <Button variant="default" onClick={onAction}>
              {t('network.addAction')}
            </Button>
            <Button onClick={onLog}>{t('network.logActivity')}</Button>
          </>
        }
      />

      <Stack gap="md" mb="md">
        <Group gap="xl" align="flex-start" wrap="wrap" data-testid="person-facts">
          <Fact label={t('network.fields.type')}>
            <Text size="sm" c="var(--color-ink)">
              {t(`network.types.${person.type}`)}
            </Text>
          </Fact>
          <Fact label={t('network.fields.consent')}>
            <Group gap="xs">
              <StatusBadge lifecycle="consent" value={person.consentStatus} testId="consent-badge" />
              {person.shareable === false && person.consentStatus !== 'GIVEN' && (
                <Text size="xs" c="var(--color-ink3)">
                  {t('network.profile.notShareable')}
                </Text>
              )}
            </Group>
          </Fact>
          <Fact label={t('network.fields.city')}>
            <Text size="sm" c="var(--color-ink)">
              {person.city || '–'}
            </Text>
          </Fact>
          <Fact label={t('network.fields.owner')}>
            <Text size="sm" c="var(--color-ink)">
              {person.owner.name}
            </Text>
          </Fact>
          <Fact label={t('network.fields.lastContact')}>
            <Text size="sm" c="var(--color-ink)">
              {person.lastContactAt ? `${fmt.dateLong(person.lastContactAt)} (${t('network.daysShort', { count: days })})` : t('network.never')}
            </Text>
          </Fact>
        </Group>

        {(person.email || person.phone || person.linkedinUrl || !!person.tags?.length || person.howWeKnow) && (
          <Group gap="sm" align="center" wrap="wrap">
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
              <Text size="sm" c="var(--color-ink3)">
                {t('network.fields.howWeKnow')}: {person.howWeKnow}
              </Text>
            )}
          </Group>
        )}
      </Stack>
    </>
  );
}
