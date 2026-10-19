// Confirm sending the KVKK notice and consent request; shows recipient and notice language (S-01).
import { Button, Group, Modal, SegmentedControl, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText, type Schemas } from '../../pipeline/shared';

export function SendConsentDialog({ person, opened, onClose, onSent }: { person: Schemas['Person']; opened: boolean; onClose: () => void; onSent: () => void }) {
  const { t } = useTranslation();
  const [language, setLanguage] = useState<Schemas['Language']>('TR');
  const [err, setErr] = useState<string>();
  const send = async () => {
    const r = await api.POST('/people/{id}/consent/requests', { params: { path: { id: person.id } }, body: { language } });
    if (r.error) setErr(problemText(r.error, t('network.consentFailed')));
    else {
      onSent();
      onClose();
    }
  };
  return (
    <Modal opened={opened} onClose={onClose} title={t('network.sendConsent')} centered>
      <Stack>
        <Text size="sm">{person.email ? t('network.consentTo', { email: person.email }) : t('network.consentNoEmail')}</Text>
        <SegmentedControl value={language} onChange={(v) => setLanguage(v as Schemas['Language'])} data={['TR', 'EN']} />
        {err && (
          <Text c="red" size="sm" role="alert">
            {err}
          </Text>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button disabled={!person.email} onClick={() => void send()}>
            {t('network.sendConsent')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
