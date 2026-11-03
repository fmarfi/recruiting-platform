import { ActionIcon, Alert, Box, Button, Collapse, Group, Modal, SimpleGrid, Stack, Text, TextInput, Textarea, Tooltip } from '@mantine/core';
import { IconChevronDown, IconInfoCircle } from '@tabler/icons-react';
import { useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText } from '../../shell/lib/problem';
import { useReference } from '../../shell/refdata/useReference';

/** Small info icon in the input's right edge; the help text is shown on hover and focus instead of under the label. */
function Hint({ text }: { text: string }) {
  return (
    <Tooltip label={text} multiline w={240} withArrow position="top-end">
      <ActionIcon variant="subtle" color="gray" size="sm" aria-label={text}>
        <IconInfoCircle size={16} stroke={1.75} />
      </ActionIcon>
    </Tooltip>
  );
}

export function NewAccountDialog({ opened, onClose, onCreated }: { opened: boolean; onClose: () => void; onCreated: (id: string) => void }) {
  const { t } = useTranslation();
  const ref = useReference();
  const formRef = useRef<HTMLFormElement>(null);
  const [name, setName] = useState('');
  const [sector, setSector] = useState('');
  const [city, setCity] = useState('');
  const [website, setWebsite] = useState('');
  const [notes, setNotes] = useState('');
  const [more, setMore] = useState(false);
  const [errs, setErrs] = useState<{ name?: string; sector?: string }>({});
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const next = { name: name.trim() ? undefined : t('accounts.err.name'), sector: sector.trim() ? undefined : t('accounts.err.sector') };
    setErrs(next);
    if (next.name || next.sector) return setErr(null);
    setBusy(true);
    const { data, error, response } = await api.POST('/accounts', {
      body: { name: name.trim(), sector: sector.trim(), city: city.trim() || null, website: website.trim() || null, notes: notes.trim() || null },
    });
    setBusy(false);
    if (response?.status === 409) return setErrs({ name: t('accounts.duplicate') });
    if (error || !data) return setErr(problemText(error, t('shell.saveError')));
    setErr(null);
    setErrs({});
    onClose();
    onCreated(data.id);
  }

  // Ctrl+Enter (Cmd+Enter on Mac) submits through the form, so validation and submit stay in one place.
  const onKeyDown = (e: KeyboardEvent<HTMLFormElement>) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      formRef.current?.requestSubmit();
    }
  };

  return (
    <Modal opened={opened} onClose={onClose} title={t('accounts.newAccount')} size="lg">
      <form ref={formRef} onSubmit={(e) => void submit(e)} onKeyDown={onKeyDown} noValidate>
        <Stack gap="md">
          <TextInput
            label={t('accounts.name')}
            placeholder={t('accounts.namePlaceholder')}
            rightSection={<Hint text={t('accounts.help.name')} />}
            rightSectionPointerEvents="all"
            value={name}
            onChange={(e) => {
              setName(e.currentTarget.value);
              setErrs((x) => ({ ...x, name: undefined }));
            }}
            error={errs.name}
            withAsterisk
            data-autofocus
          />
          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
            <TextInput
              label={t('accounts.sector')}
              list="sector-options"
              rightSection={<Hint text={t('accounts.help.sector')} />}
              rightSectionPointerEvents="all"
              value={sector}
              onChange={(e) => {
                setSector(e.currentTarget.value);
                setErrs((x) => ({ ...x, sector: undefined }));
              }}
              error={errs.sector}
              withAsterisk
            />
            <datalist id="sector-options">
              {(ref.data?.sectors ?? []).map((s) => (
                <option key={s} value={s} />
              ))}
            </datalist>
            <TextInput
              label={t('accounts.city')}
              rightSection={<Hint text={t('accounts.help.city')} />}
              rightSectionPointerEvents="all"
              value={city}
              onChange={(e) => setCity(e.currentTarget.value)}
            />
          </SimpleGrid>
          <Box>
            <Button
              variant="subtle"
              color="gray"
              w="fit-content"
              aria-expanded={more}
              aria-controls="new-account-more"
              onClick={() => setMore((m) => !m)}
              rightSection={<IconChevronDown size={16} stroke={1.75} style={{ transform: more ? 'rotate(180deg)' : undefined, transition: 'transform 120ms ease-out' }} />}
            >
              {t('accounts.moreDetails')}
            </Button>
            <Collapse expanded={more} id="new-account-more">
              <Stack gap="md" pt="md">
                <TextInput label={t('accounts.edit.website')} type="url" placeholder="https://" value={website} onChange={(e) => setWebsite(e.currentTarget.value)} />
                <Textarea label={t('accounts.edit.notes')} value={notes} onChange={(e) => setNotes(e.currentTarget.value)} autosize minRows={3} maxLength={4000} />
              </Stack>
            </Collapse>
          </Box>
          {err && (
            <Alert color="red" role="alert">
              {err}
            </Alert>
          )}
          <Box pos="sticky" bottom={0} bg="var(--mantine-color-body)" pt="sm">
            <Group justify="space-between" wrap="wrap" gap="sm">
              <Text size="xs" c="dimmed">
                {t('accounts.saveHint')}
              </Text>
              <Group gap="xs">
                <Button variant="default" onClick={onClose}>
                  {t('shell.cancel')}
                </Button>
                <Button type="submit" loading={busy}>
                  {t('accounts.createClient')}
                </Button>
              </Group>
            </Group>
          </Box>
        </Stack>
      </form>
    </Modal>
  );
}
