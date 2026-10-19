import { Anchor, Badge, Group, Stack, Switch, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { components } from '../../api';
import { EmptyState } from '../../shell/components/EmptyState';
import { ToneBadge } from '../../shell/components/ToneBadge';
import { fmtDate } from '../../shell/lib/format';

type Contact = components['schemas']['Contact'];

const CONSENT_TONE: Record<string, string> = { GIVEN: 'GOOD', REFUSED: 'BAD', WITHDRAWN: 'BAD', SENT: 'INFO', NOT_SENT: 'NEUTRAL' };

interface Props {
  contacts: Contact[];
  showEnded: boolean;
  onShowEnded: (v: boolean) => void;
}

/** Contacts of the account; ended ones are muted and hidden unless "Show ended" is on (A-03). */
export function ContactsTab({ contacts, showEnded, onShowEnded }: Props) {
  const { t, i18n } = useTranslation();
  return (
    <Stack gap="xs">
      <Switch label={t('accounts.showEnded')} checked={showEnded} onChange={(e) => onShowEnded(e.currentTarget.checked)} />
      {contacts.length === 0 ? (
        <EmptyState title={t('accounts.emptyContacts')} hint={t('accounts.emptyContactsHint')} />
      ) : (
        contacts.map((c) => (
          <Group key={c.id} justify="space-between" wrap="nowrap" style={{ opacity: c.active ? 1 : 0.6 }}>
            <div>
              <Anchor component={Link} to={`/people/${c.person.id}`} fw={600}>
                {c.person.name}
              </Anchor>{' '}
              {c.primary && <Badge size="xs">{t('accounts.primary')}</Badge>}
              <Text size="sm" c="dimmed">
                {[c.role ?? c.person.title, c.person.email].filter(Boolean).join(' - ')}
                {c.endedOn ? ` - ${t('accounts.endedOn', { date: fmtDate(c.endedOn, i18n.language) })}` : ''}
              </Text>
            </div>
            <ToneBadge size="sm" tone={CONSENT_TONE[c.person.consentStatus] ?? 'NEUTRAL'} label={t(`accounts.consent.${c.person.consentStatus}`)} />
          </Group>
        ))
      )}
    </Stack>
  );
}
