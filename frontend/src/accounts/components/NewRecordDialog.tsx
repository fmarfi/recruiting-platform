import { Alert, Button, Group, Modal, Stack, Textarea, TextInput, NumberInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { FormSection, RequiredNote } from '../../shell/components/FormSection';
import { problemText } from '../../shell/lib/problem';

interface Props {
  kind: 'job' | 'proposal';
  opened: boolean;
  onClose: () => void;
  accountId: string;
  onDone: () => void;
}

const FEE = /^\d+(\.\d{1,2})?$/;

/** "New job" / "New proposal" on the account page (A-05, A-06). Minimal fields; the rest is edited on the record. */
export function NewRecordDialog({ kind, opened, onClose, accountId, onDone }: Props) {
  const { t } = useTranslation();
  const [title, setTitle] = useState('');
  const [location, setLocation] = useState('');
  const [openings, setOpenings] = useState<number | string>(1);
  const [terms, setTerms] = useState('');
  const [fee, setFee] = useState('');
  const [currency, setCurrency] = useState('TRY');
  const [titleErr, setTitleErr] = useState<string>();
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const feeErr = fee && !FEE.test(fee) ? t('accounts.err.fee') : undefined;
  const curErr = fee && currency.trim().length !== 3 ? t('accounts.err.currency') : undefined;

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return setTitleErr(t('accounts.err.title'));
    if (kind === 'proposal' && (feeErr || curErr)) return;
    setBusy(true);
    const orNull = (v: string) => (v.trim() ? v.trim() : null);
    const { error } =
      kind === 'job'
        ? await api.POST('/job-orders', { body: { accountId, title: title.trim(), location: orNull(location), openings: Number(openings) || 1 } })
        : await api.POST('/proposals', {
            body: { accountId, title: title.trim(), terms: orNull(terms), feeAmount: orNull(fee), currency: fee.trim() ? currency.trim().toUpperCase() : null },
          });
    setBusy(false);
    if (error) return setErr(problemText(error, t('shell.saveError')));
    setTitle('');
    setTitleErr(undefined);
    setErr(null);
    onClose();
    onDone();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t(kind === 'job' ? 'accounts.newJob' : 'accounts.newProposal')}>
      <form onSubmit={(e) => void submit(e)} noValidate>
        <Stack>
          <RequiredNote text={t('shell.requiredNote')} />
          <FormSection title={t(kind === 'job' ? 'accounts.section.role' : 'accounts.section.proposal')}>
            <TextInput
              label={t(kind === 'job' ? 'accounts.jobTitle' : 'accounts.proposalTitle')}
              description={kind === 'job' ? t('accounts.help.jobTitle') : undefined}
              value={title}
              onChange={(e) => {
                setTitle(e.currentTarget.value);
                setTitleErr(undefined);
              }}
              error={titleErr}
              maxLength={200}
              withAsterisk
              data-autofocus
            />
            {kind === 'job' ? (
              <>
                <TextInput label={t('accounts.location')} value={location} onChange={(e) => setLocation(e.currentTarget.value)} maxLength={200} />
                <NumberInput label={t('accounts.openings')} description={t('accounts.help.openings')} min={1} value={openings} onChange={setOpenings} styles={{ input: { fontVariantNumeric: 'tabular-nums' } }} />
              </>
            ) : (
              <Textarea label={t('accounts.terms')} description={t('accounts.help.terms')} value={terms} onChange={(e) => setTerms(e.currentTarget.value)} maxLength={10000} autosize minRows={2} />
            )}
          </FormSection>
          {kind === 'proposal' && (
            <FormSection title={t('accounts.section.fees')}>
              <Group grow align="flex-start">
                <TextInput label={t('accounts.fee')} description={t('accounts.help.fee')} value={fee} onChange={(e) => setFee(e.currentTarget.value)} placeholder="25000.00" error={feeErr} styles={{ input: { textAlign: 'right', fontVariantNumeric: 'tabular-nums' } }} />
                <TextInput label={t('accounts.currency')} value={currency} onChange={(e) => setCurrency(e.currentTarget.value)} maxLength={3} error={curErr} />
              </Group>
            </FormSection>
          )}
          {err && (
            <Alert color="red" role="alert">
              {err}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              {t('shell.cancel')}
            </Button>
            <Button type="submit" loading={busy}>
              {t('shell.save')}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
