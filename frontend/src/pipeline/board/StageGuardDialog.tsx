// Shown when a stage move is refused with 409 CONSENT_REQUIRED (resend consent; cannot be overridden) or 409 OFF_LIMITS
// (rule shown; MANAGER/ADMIN may retry with a written reason).
import { Alert, Button, Group, List, Modal, Stack, Text, Textarea } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText } from '../shared';
import type { PendingGuard } from './useBoardMoves';
import { canOverrideOffLimits, consentResendPossible, REASON_MAX, validOverrideReason } from './stageGuard';

export function StageGuardDialog({
  guard,
  role,
  onClose,
  onOverride,
}: {
  guard: PendingGuard | undefined;
  role?: string | null;
  onClose: () => void;
  onOverride: (g: PendingGuard, reason: string) => void;
}) {
  const { t, i18n } = useTranslation();
  const [reason, setReason] = useState('');
  const [sent, setSent] = useState<string>();
  const [err, setErr] = useState<string>();
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    setReason('');
    setSent(undefined);
    setErr(undefined);
  }, [guard?.app.id, guard?.kind]);

  const resend = async () => {
    if (!guard) return;
    setBusy(true);
    setErr(undefined);
    try {
      const r = await api.POST('/people/{id}/consent/requests', { params: { path: { id: guard.app.person.id } }, body: { language: i18n.language === 'en' ? 'EN' : 'TR' } });
      if (r.error) setErr(problemText(r.error, t('network.consentFailed')));
      else setSent(guard.app.person.name);
    } catch {
      setErr(t('network.consentFailed'));
    } finally {
      setBusy(false);
    }
  };

  const trimmed = reason.trim().length;
  return (
    <Modal opened={!!guard} onClose={onClose} centered title={guard ? t(guard.kind === 'CONSENT_REQUIRED' ? 'pipeline.guard.consentTitle' : 'pipeline.guard.offTitle') : ''} size="md">
      {guard?.kind === 'CONSENT_REQUIRED' && (
        <Stack data-testid="guard-consent">
          <Text size="sm">{t('pipeline.guard.consentBody', { name: guard.app.person.name })}</Text>
          <List size="sm">
            {(guard.items.length ? guard.items : [{ id: guard.app.person.id, title: guard.app.person.name }]).map((i) => (
              <List.Item key={i.id}>
                {i.title}
                {i.reasonCode ? `: ${t(`pipeline.guard.share.${i.reasonCode}`, { defaultValue: i.reasonCode })}` : ''}
              </List.Item>
            ))}
          </List>
          <Text size="xs" c="dimmed">
            {t('pipeline.guard.notOverridable')}
          </Text>
          {!consentResendPossible(guard.items) && (
            <Alert color="orange" data-testid="guard-no-resend">
              {t('pipeline.guard.noResend')}
            </Alert>
          )}
          {sent && (
            <Alert color="green" role="status">
              {t('pipeline.guard.consentSent', { name: sent })}
            </Alert>
          )}
          {err && (
            <Alert color="red" role="alert">
              {err}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button onClick={() => void resend()} loading={busy} disabled={!consentResendPossible(guard.items) || !!sent}>
              {t('network.profile.resendConsent')}
            </Button>
          </Group>
        </Stack>
      )}
      {guard?.kind === 'OFF_LIMITS' && (
        <Stack data-testid="guard-offlimits">
          <Text size="sm">{t('pipeline.guard.offBody', { name: guard.app.person.name })}</Text>
          <List size="sm">
            {guard.items.map((i) => (
              <List.Item key={i.id}>
                {i.title}
                {i.reasonCode ? ` (${t(`pipeline.guard.offReason.${i.reasonCode}`, { defaultValue: i.reasonCode })})` : ''}
              </List.Item>
            ))}
          </List>
          {canOverrideOffLimits(role) ? (
            <>
              <Textarea
                label={t('pipeline.guard.overrideLabel')}
                description={t('pipeline.guard.overrideHint')}
                value={reason}
                onChange={(e) => setReason(e.currentTarget.value)}
                maxLength={REASON_MAX}
                autosize
                minRows={2}
                data-autofocus
                error={trimmed > 0 && !validOverrideReason(reason) ? t('pipeline.guard.reasonInvalid') : undefined}
              />
              <Group justify="flex-end">
                <Button variant="default" onClick={onClose}>
                  {t('common.cancel')}
                </Button>
                <Button color="red" disabled={!validOverrideReason(reason)} onClick={() => onOverride(guard, reason.trim())}>
                  {t('pipeline.guard.moveAnyway')}
                </Button>
              </Group>
            </>
          ) : (
            <>
              <Alert color="gray">{t('pipeline.guard.managerOnly')}</Alert>
              <Group justify="flex-end">
                <Button variant="default" onClick={onClose}>
                  {t('common.cancel')}
                </Button>
              </Group>
            </>
          )}
        </Stack>
      )}
    </Modal>
  );
}
