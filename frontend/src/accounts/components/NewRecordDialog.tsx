import { Alert, Button, Group, Modal, Stack, Textarea, TextInput, NumberInput } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText } from '../../shell/lib/problem';

interface Props {
  kind: 'job' | 'proposal';
  opened: boolean;
  onClose: () => void;
  accountId: string;
  onDone: () => void;
}

/** "New job" / "New proposal" on the account page (A-05, A-06). Minimal fields; the rest is edited on the record. */
export function NewRecordDialog({ kind, opened, onClose, accountId, onDone }: Props) {
  const { t } = useTranslation();
  const [title, setTitle] = useState('');
  const [location, setLocation] = useState('');
  const [openings, setOpenings] = useState<number | string>(1);
  const [terms, setTerms] = useState('');
  const [fee, setFee] = useState('');
  const [currency, setCurrency] = useState('TRY');
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return setErr(t('accounts.fillRequired'));
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
    setErr(null);
    onClose();
    onDone();
  }

  return (
    <Modal opened={opened} onClose={onClose} title={t(kind === 'job' ? 'accounts.newJob' : 'accounts.newProposal')}>
      <form onSubmit={(e) => void submit(e)}>
        <Stack>
          <TextInput label={t(kind === 'job' ? 'accounts.jobTitle' : 'accounts.proposalTitle')} value={title} onChange={(e) => setTitle(e.currentTarget.value)} maxLength={200} required data-autofocus />
          {kind === 'job' ? (
            <>
              <TextInput label={t('accounts.location')} value={location} onChange={(e) => setLocation(e.currentTarget.value)} maxLength={200} />
              <NumberInput label={t('accounts.openings')} min={1} value={openings} onChange={setOpenings} />
            </>
          ) : (
            <>
              <Textarea label={t('accounts.terms')} value={terms} onChange={(e) => setTerms(e.currentTarget.value)} maxLength={10000} autosize minRows={2} />
              <Group grow align="flex-start">
                <TextInput label={t('accounts.fee')} value={fee} onChange={(e) => setFee(e.currentTarget.value)} placeholder="25000.00" error={fee && !/^\d+(\.\d{1,2})?$/.test(fee) ? ' ' : undefined} />
                <TextInput label={t('accounts.currency')} value={currency} onChange={(e) => setCurrency(e.currentTarget.value)} maxLength={3} />
              </Group>
            </>
          )}
          {err && <Alert color="red">{err}</Alert>}
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
