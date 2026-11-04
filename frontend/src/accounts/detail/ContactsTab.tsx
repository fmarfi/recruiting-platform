import { Alert, Anchor, Button, Group, Stack, Switch, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api, type components } from '../../api';
import { ConfirmDialog } from '../../shell/components/ConfirmDialog';
import { ToneBadge } from '../../shell/components/ToneBadge';
import { fmtDate } from '../../shell/lib/format';
import { AddButton } from './AddButton';
import { SectionEmpty } from './SectionEmpty';

type Contact = components['schemas']['Contact'];

const CONSENT_TONE: Record<string, string> = { GIVEN: 'GOOD', REFUSED: 'BAD', WITHDRAWN: 'BAD', SENT: 'INFO', NOT_SENT: 'NEUTRAL' };

interface Props {
  contacts: Contact[];
  showEnded: boolean;
  onShowEnded: (v: boolean) => void;
  accountId: string;
  onChanged: () => void;
  onAdd: () => void;
}

/** Contacts of the account; ended ones are muted and hidden unless "Show ended" is on (A-03). */
export function ContactsTab({ contacts, showEnded, onShowEnded, accountId, onChanged, onAdd }: Props) {
  const { t, i18n } = useTranslation();
  const [ending, setEnding] = useState<Contact | null>(null);
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);
  const end = async () => {
    if (!ending) return;
    setBusy(true);
    const r = await api.DELETE('/accounts/{id}/contacts/{contactId}', { params: { path: { id: accountId, contactId: ending.id } } }).catch(() => ({ error: true }));
    setBusy(false);
    setFailed(!!r.error);
    setEnding(null);
    if (!r.error) onChanged();
  };
  return (
    <Stack gap="xs">
      {failed && (
        <Alert color="red" role="alert" withCloseButton onClose={() => setFailed(false)}>
          {t('accounts.endContactFailed')}
        </Alert>
      )}
      <ConfirmDialog opened={!!ending} title={t('accounts.endContactTitle')} body={ending ? t('accounts.endContactBody', { name: ending.person.name }) : undefined} confirmLabel={t('accounts.endContact')} danger busy={busy} onConfirm={() => void end()} onCancel={() => setEnding(null)} />
      <Switch label={t('accounts.showEnded')} checked={showEnded} onChange={(e) => onShowEnded(e.currentTarget.checked)} />
      {contacts.length === 0 ? (
        <SectionEmpty text={t('accounts.emptyContacts')} action={<AddButton label={t('accounts.addContact')} onClick={onAdd} />} />
      ) : (
        contacts.map((c) => (
          <Group key={c.id} justify="space-between" wrap="nowrap" style={{ opacity: c.active ? 1 : 0.6 }}>
            <div>
              <Anchor component={Link} to={`/people/${c.person.id}`} fw={500} underline="never" c="var(--color-ink)">
                {c.person.name}
              </Anchor>{' '}
              {c.primary && (
                <Text span size="xs" c="dimmed" ml={4}>
                  {t('accounts.primary')}
                </Text>
              )}
              <Text size="sm" c="dimmed">
                {[c.role ?? c.person.title, c.person.email].filter(Boolean).join(' - ')}
                {c.endedOn ? ` - ${t('accounts.endedOn', { date: fmtDate(c.endedOn, i18n.language) })}` : ''}
              </Text>
            </div>
            <Group gap="xs" wrap="nowrap">
              {c.active && (
                <Button
                  size="compact-xs"
                  variant="subtle"
                  color="red"
                  onClick={() => setEnding(c)}
                  aria-label={`${t('accounts.endContact')}: ${c.person.name}`}
                >
                  {t('accounts.endContact')}
                </Button>
              )}
              <ToneBadge size="sm" tone={CONSENT_TONE[c.person.consentStatus] ?? 'NEUTRAL'} label={t(`accounts.consent.${c.person.consentStatus}`)} />
            </Group>
          </Group>
        ))
      )}
    </Stack>
  );
}
