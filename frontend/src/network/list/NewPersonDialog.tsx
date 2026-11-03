// "+ New person" dialog (F7). Before creating, /duplicates/check runs; a hit shows the "possible duplicate" prompt
// (use existing / create anyway). A duplicate e-mail (409 DUPLICATE) still links to the existing person.
import { Anchor, Box, Button, Collapse, Divider, Group, Modal, Select, SimpleGrid, Stack, Text, TextInput, Tooltip } from '@mantine/core';
import { IconChevronDown, IconInfoCircle } from '@tabler/icons-react';
import { useId, useState, type KeyboardEvent, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { problemText, type Schemas } from '../../pipeline/shared';
import { checkRequest, duplicateDecision, type Match } from '../duplicates/duplicateLogic';
import { DuplicatePrompt } from '../duplicates/DuplicatePrompt';
import { SOURCE_CHANNELS } from './networkFilters';

const TYPES: Schemas['PersonType'][] = ['EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE'];
const EMPTY = { name: '', type: 'EXECUTIVE' as Schemas['PersonType'], title: '', company: '', city: '', email: '', phone: '', linkedinUrl: '', howWeKnow: '', sourceChannel: '' };

/** Closed-by-default group for the fields that are not needed to create the record. Sentence-case label, no box. */
export function MoreDetails({ children, defaultOpened = false }: { children: ReactNode; defaultOpened?: boolean }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(defaultOpened);
  const id = useId();
  return (
    <Stack gap="sm">
      <Button
        variant="subtle"
        color="gray"
        justify="flex-start"
        px={0}
        fz="xs"
        fw={600}
        c="var(--color-ink2)"
        aria-expanded={open}
        aria-controls={id}
        onClick={() => setOpen((o) => !o)}
        rightSection={<IconChevronDown size={16} stroke={1.75} aria-hidden style={{ transform: open ? 'rotate(180deg)' : undefined, transition: 'transform 120ms ease-out' }} />}
      >
        {t('network.moreDetails')}
      </Button>
      <Collapse expanded={open}>
        <Stack id={id} gap="sm">
          {children}
        </Stack>
      </Collapse>
    </Stack>
  );
}

export function NewPersonDialog({ opened, onClose, onCreated }: { opened: boolean; onClose: () => void; onCreated: () => void }) {
  const { t } = useTranslation();
  const [f, setF] = useState(EMPTY);
  const [err, setErr] = useState<{ text: string; existingId?: string }>();
  const [matches, setMatches] = useState<Match[]>();
  const [busy, setBusy] = useState(false);
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const emailErr = f.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim()) ? t('network.err.email') : undefined;
  const linkErr = f.linkedinUrl.trim() && !/^https?:\/\//i.test(f.linkedinUrl.trim()) ? t('network.err.linkedin') : undefined;
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setF({ ...f, [k]: e.currentTarget.value });
    setMatches(undefined);
  };
  const cancel = () => {
    setF(EMPTY);
    setMatches(undefined);
    setErr(undefined);
    setTouched({});
    onClose();
  };
  const orNull = (v: string) => (v.trim() ? v.trim() : null);

  const create = async () => {
    setBusy(true);
    try {
      const r = await api.POST('/people', {
        body: {
          name: f.name.trim(),
          type: f.type,
          title: orNull(f.title),
          company: orNull(f.company),
          city: orNull(f.city),
          email: orNull(f.email),
          phone: orNull(f.phone),
          linkedinUrl: orNull(f.linkedinUrl),
          howWeKnow: orNull(f.howWeKnow),
          ...(f.sourceChannel ? { sourceChannel: f.sourceChannel as Schemas['SourceChannel'] } : {}),
        },
      });
      if (r.error) {
        setMatches(undefined);
        setErr({ text: problemText(r.error, t('network.createFailed')), existingId: (r.error as { existingId?: string }).existingId });
        return;
      }
      setErr(undefined);
      setMatches(undefined);
      setF(EMPTY);
      onCreated();
      onClose();
    } catch {
      setErr({ text: t('network.createFailed') });
    } finally {
      setBusy(false);
    }
  };

  const save = async () => {
    setErr(undefined);
    if (!f.name.trim() || emailErr || linkErr) {
      setTouched({ name: true });
      return;
    }
    const req = checkRequest(f);
    if (req) {
      setBusy(true);
      let found: Match[] | undefined;
      try {
        const r = await api.POST('/duplicates/check', { body: req });
        found = r.data;
      } catch {
        found = undefined; // a failed check must not block work; the server still guards the e-mail
      }
      setBusy(false);
      if (duplicateDecision(found) === 'prompt') {
        setMatches(found);
        return;
      }
    }
    await create();
  };

  const onKeyDown = (e: KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter' && !matches) {
      e.preventDefault();
      void save();
    }
  };

  return (
    <Modal opened={opened} onClose={cancel} title={t('network.newPerson')} size="lg" centered>
      <form
        noValidate
        onKeyDown={onKeyDown}
        onSubmit={(e) => {
          e.preventDefault();
          void save();
        }}
      >
        <Stack gap="sm">
          <TextInput label={t('network.fields.name')} placeholder={t('network.ph.name')} withAsterisk value={f.name} onChange={set('name')} error={touched.name && !f.name.trim() ? t('network.err.name') : undefined} onBlur={() => setTouched((x) => ({ ...x, name: true }))} data-autofocus />
          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
            <TextInput label={t('network.fields.title')} placeholder={t('network.ph.title')} value={f.title} onChange={set('title')} />
            <TextInput label={t('network.fields.company')} value={f.company} onChange={set('company')} />
            <TextInput
              label={t('network.fields.email')}
              placeholder={t('network.ph.email')}
              type="email"
              value={f.email}
              onChange={set('email')}
              error={emailErr}
              rightSectionPointerEvents="all"
              rightSection={
                <Tooltip label={t('network.consentHint')} withArrow position="top">
                  <Box component="span" tabIndex={0} role="img" aria-label={t('network.consentHint')} style={{ display: 'inline-flex', color: 'var(--color-ink3)' }}>
                    <IconInfoCircle size={16} stroke={1.75} aria-hidden />
                  </Box>
                </Tooltip>
              }
            />
            <TextInput label={t('network.dup.phone')} placeholder={t('network.ph.phone')} value={f.phone} onChange={set('phone')} />
            <TextInput label={t('network.fields.city')} value={f.city} onChange={set('city')} />
            <Select
              label={t('network.fields.type')}
              allowDeselect={false}
              data={TYPES.map((x) => ({ value: x, label: t(`network.types.${x}`) }))}
              value={f.type}
              onChange={(v) => v && setF({ ...f, type: v as Schemas['PersonType'] })}
            />
          </SimpleGrid>
          <MoreDetails>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
              <TextInput label="LinkedIn" type="url" placeholder="https://" value={f.linkedinUrl} onChange={set('linkedinUrl')} error={linkErr} />
              <Select
                label={t('network.pool.channels')}
                clearable
                value={f.sourceChannel || null}
                onChange={(v) => setF({ ...f, sourceChannel: v ?? '' })}
                data={SOURCE_CHANNELS.map((c) => ({ value: c, label: t(`network.pool.channel.${c}`) }))}
              />
            </SimpleGrid>
            <TextInput label={t('network.fields.howWeKnow')} placeholder={t('network.help.howWeKnow')} value={f.howWeKnow} onChange={set('howWeKnow')} />
          </MoreDetails>
          {matches && <DuplicatePrompt matches={matches} busy={busy} onBack={() => setMatches(undefined)} onCreateAnyway={() => void create()} />}
          {err && (
            <Text c="red" size="sm" role="alert">
              {err.text}{' '}
              {err.existingId && (
                <Anchor component={Link} to={`/people/${err.existingId}`}>
                  {t('network.openExisting')}
                </Anchor>
              )}
            </Text>
          )}
          <Box pos="sticky" bottom={0} bg="var(--mantine-color-body)" pt="xs">
            <Divider mb="sm" />
            <Group justify="space-between" wrap="nowrap" pb="xs">
              <Text size="xs" c="var(--color-ink3)">
                {t('network.ctrlSave')}
              </Text>
              <Group gap="sm" wrap="nowrap">
                <Button variant="default" onClick={cancel}>
                  {t('common.cancel')}
                </Button>
                {!matches && (
                  <Button type="submit" loading={busy}>
                    {t('network.createCandidate')}
                  </Button>
                )}
              </Group>
            </Group>
          </Box>
        </Stack>
      </form>
    </Modal>
  );
}
