// Person header (visual system v3, sections 5 and 6b): RecordHeader band content: name, type badge, subtitle, actions, fact strip and the tabs slot.
import { Anchor, Button, Group, Menu, Text } from '@mantine/core';
import { IconDots } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge, useFormat, type Schemas } from '../../pipeline/shared';
import { ICON_SIZE, ICON_STROKE, PersonIcon } from '../../shell/components/objectIcons';
import { RecordHeader } from '../../shell/components/RecordHeader';
import { ToneBadge } from '../../shell/components/ToneBadge';
import { canResendConsent } from './profile';

type Action = { key: string; label: string; onClick: () => void; disabled?: boolean; testId?: string };

export function PersonHeader({
  person,
  onLog,
  onAction,
  onResend,
  onEdit,
  resending = false,
  searchCount = 0,
  tabs,
}: {
  person: Schemas['Person'];
  onLog: () => void;
  onAction: () => void;
  onResend: () => void;
  onEdit: () => void;
  resending?: boolean;
  searchCount?: number;
  tabs?: ReactNode;
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
    <RecordHeader
      breadcrumb={
        <Anchor component={Link} to="/network" size="sm">
          {t('nav.network')}
        </Anchor>
      }
      avatar={<PersonIcon size={32} stroke={ICON_STROKE} color="var(--color-ink3)" aria-hidden />}
      name={person.name}
      badge={<ToneBadge tone="NEUTRAL" label={t(`network.types.${person.type}`)} />}
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
      facts={[
        {
          label: t('network.fields.consent'),
          value: (
            <Group gap="xs">
              <StatusBadge lifecycle="consent" value={person.consentStatus} testId="consent-badge" />
              {person.shareable === false && person.consentStatus !== 'GIVEN' && (
                <Text span size="xs" c="var(--color-ink3)">
                  {t('network.profile.notShareable')}
                </Text>
              )}
            </Group>
          ),
        },
        { label: t('network.fields.city'), value: person.city || '–' },
        { label: t('network.fields.owner'), value: person.owner.name },
        {
          label: t('network.fields.lastContact'),
          value: person.lastContactAt ? `${fmt.dateLong(person.lastContactAt)} (${t('network.daysShort', { count: days })})` : t('network.never'),
        },
        { label: t('network.snapshot.pipelines'), value: searchCount },
      ]}
      tabs={tabs}
    />
  );
}
