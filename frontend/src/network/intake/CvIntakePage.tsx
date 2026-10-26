// CV intake: bulk CV drop zone and the review list. Each pending item is attached to a person (existing or new) or discarded; items expire after 14 days.
import { Alert, Anchor, Badge, Button, Card, Container, Group, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { problemCode, problemText, useAsync, useFormat, type Schemas } from '../../pipeline/shared';
import { EmptyState } from '../../shell/components/EmptyState';
import { DropZone } from '../files/DropZone';
import { formatSize, MAX_BATCH, splitFiles } from '../files/fileRules';
import { intakeFileUrl, uploadCvIntake } from '../files/useFiles';
import { ReasonBadges } from '../duplicates/DuplicatePrompt';
import { CreatePersonDialog } from './CreatePersonDialog';
import { attachExisting, expiryInfo, needsPolling } from './intakeLogic';

type Item = Schemas['CvIntakeItem'];
type Note = { color: string; text: string; action?: { label: string; run: () => void } };

export function CvIntakePage() {
  const { t } = useTranslation();
  const fmt = useFormat();
  const res = useAsync(() => api.GET('/cv-intake', { params: { query: { size: 100 } } }), []);
  const items = res.data?.content;
  const [busy, setBusy] = useState(false);
  const [working, setWorking] = useState<string>();
  const [notes, setNotes] = useState<Note[]>([]);
  const [creating, setCreating] = useState<Item>();
  const [createErr, setCreateErr] = useState<string>();

  // Text extraction is async: refresh every 4 s while something is still PENDING.
  const polling = needsPolling(items);
  useEffect(() => {
    if (!polling) return;
    const id = setInterval(res.reload, 4000);
    return () => clearInterval(id);
  }, [polling, res.reload]);

  const upload = async (picked: File[]) => {
    const { ok, rejected } = splitFiles(picked, MAX_BATCH);
    const next: Note[] = rejected.map((r) => ({ color: 'red', text: `${r.fileName}: ${t(`network.files.reject.${r.code}`)}` }));
    if (ok.length) {
      setBusy(true);
      try {
        const r = await uploadCvIntake(ok.map((i) => picked[i]!));
        if (r.error) next.push({ color: 'red', text: problemText(r.error, t('network.files.uploadFailed')) });
        else {
          for (const x of r.data?.rejected ?? []) next.push({ color: 'red', text: `${x.fileName}: ${t(`network.files.reject.${x.code}`)}` });
          next.push({ color: 'green', text: t('network.intake.uploaded', { count: r.data?.items.length ?? 0 }) });
        }
      } catch {
        next.push({ color: 'red', text: t('network.files.uploadFailed') });
      }
      setBusy(false);
    }
    setNotes(next);
    res.reload();
  };

  const attach = async (item: Item, body: Schemas['CvIntakeAttachRequest']) => {
    setWorking(item.id);
    setCreateErr(undefined);
    try {
      const r = await api.POST('/cv-intake/{id}/attach', { params: { path: { id: item.id } }, body });
      if (r.error) {
        const code = problemCode(r.error);
        const existing = (r.error as { existingId?: string }).existingId;
        const text = code === 'DUPLICATE' ? t('network.intake.emailExists') : problemText(r.error, t('network.intake.attachFailed'));
        if (creating) setCreateErr(text);
        else setNotes([{ color: 'red', text }]);
        // The e-mail already belongs to a person: offer to attach the CV there instead.
        if (code === 'DUPLICATE' && existing) {
          setCreating(undefined);
          setNotes([{ color: 'yellow', text, action: { label: t('network.intake.attachToThat'), run: () => void attach(item, attachExisting(existing)) } }]);
        }
        return;
      }
      setCreating(undefined);
      setNotes([{ color: 'green', text: t('network.intake.attached', { name: r.data.person.name }) }]);
      res.reload();
    } catch {
      setNotes([{ color: 'red', text: t('network.intake.attachFailed') }]);
    } finally {
      setWorking(undefined);
    }
  };

  const discard = async (item: Item) => {
    setWorking(item.id);
    const r = await api.DELETE('/cv-intake/{id}', { params: { path: { id: item.id } } });
    setWorking(undefined);
    if (r.error) setNotes([{ color: 'red', text: problemText(r.error, t('network.intake.discardFailed')) }]);
    else res.reload();
  };

  return (
    <Container maw="var(--content-max-width)" fluid>
      <Anchor component={Link} to="/network" size="sm">
        {t('nav.network')}
      </Anchor>
      <Title order={1} my="sm" data-testid="screen-title">
        {t('network.intake.title')}
        {items ? ` (${res.data!.page.totalElements})` : ''}
      </Title>
      <Stack>
        <DropZone multiple busy={busy} onFiles={(f) => void upload(f)} hint={t('network.intake.limits', { max: MAX_BATCH })} />
        <Text size="xs" c="dimmed">
          {t('network.intake.noConsentYet')}
        </Text>
        {notes.map((n, i) => (
          <Alert key={i} color={n.color} role={n.color === 'red' ? 'alert' : 'status'} withCloseButton onClose={() => setNotes(notes.filter((_, j) => j !== i))}>
            {n.text}{' '}
            {n.action && (
              <Button size="compact-sm" variant="light" onClick={n.action.run}>
                {n.action.label}
              </Button>
            )}
          </Alert>
        ))}
        {res.loading && !items && <Skeleton height={100} />}
        {!!res.error && (
          <Alert color="red" role="alert">
            {t('network.intake.loadFailed')}{' '}
            <Button size="compact-sm" variant="light" onClick={res.reload}>
              {t('common.retry')}
            </Button>
          </Alert>
        )}
        {items && items.length === 0 && <EmptyState title={t('network.intake.empty')} />}
        {items?.map((it) => {
          const exp = expiryInfo(it);
          const d = it.detected;
          return (
            <Card key={it.id} withBorder data-testid="intake-item">
              <Group justify="space-between" align="flex-start" wrap="wrap">
                <Stack gap={4} style={{ flex: 1, minWidth: 220 }}>
                  <Group gap="xs">
                    <Anchor href={intakeFileUrl(it.id)} download fw={600}>
                      {it.fileName ?? '-'}
                    </Anchor>
                    {it.sizeBytes != null && (
                      <Text span size="xs" c="dimmed">
                        {formatSize(it.sizeBytes)}
                      </Text>
                    )}
                    <Badge variant="light" color={it.textStatus === 'DONE' ? 'green' : it.textStatus === 'PENDING' ? 'gray' : 'yellow'}>
                      {t(`network.files.status.${it.textStatus}`)}
                    </Badge>
                  </Group>
                  <Text size="xs" c="dimmed">
                    {fmt.dateLong(it.uploadedAt)} - {it.uploadedBy.name}
                  </Text>
                  <Text size="xs" c={exp.urgent ? 'red' : 'dimmed'} data-testid="intake-expiry" fw={exp.urgent ? 600 : undefined}>
                    {t('network.intake.expires', { count: exp.days, date: fmt.dateLong(it.expiresAt) })}
                  </Text>
                  {(d.emails.length > 0 || d.phones.length > 0 || d.linkedinUrls.length > 0) && (
                    <Text size="sm">
                      {t('network.intake.detected')}: {[...d.emails, ...d.phones, ...d.linkedinUrls].join(' | ')}
                    </Text>
                  )}
                  {it.duplicates.map((m) => (
                    <Group key={m.person.id} gap="xs" data-testid="intake-duplicate">
                      <Text size="sm">
                        {t('network.intake.looksLike')}{' '}
                        <Anchor component={Link} to={`/people/${m.person.id}`} target="_blank">
                          {m.person.name}
                        </Anchor>
                      </Text>
                      <ReasonBadges reasons={m.reasons} />
                      <Button size="compact-sm" loading={working === it.id} onClick={() => void attach(it, attachExisting(m.person.id))}>
                        {t('network.intake.attachTo', { name: m.person.name })}
                      </Button>
                    </Group>
                  ))}
                </Stack>
                <Group gap="xs">
                  <Button size="compact-sm" variant="default" disabled={working === it.id} onClick={() => void discard(it)}>
                    {t('network.intake.discard')}
                  </Button>
                  <Button size="compact-sm" disabled={working === it.id} onClick={() => setCreating(it)}>
                    {t('network.intake.createPerson')}
                  </Button>
                </Group>
              </Group>
            </Card>
          );
        })}
      </Stack>
      <CreatePersonDialog item={creating} error={createErr} busy={!!creating && working === creating.id} onClose={() => setCreating(undefined)} onCreate={(p) => creating && void attach(creating, { person: p })} />
    </Container>
  );
}
