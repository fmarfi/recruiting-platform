// Bulk add to the longlist (S5): filter the pool (GET /people), tick people, add them in one go (one POST per person, in parallel).
import { Alert, Button, Checkbox, Group, Modal, Select, Stack, Text, TextInput } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText, type Schemas } from '../shared';

const FUNCTIONS: Schemas['JobFunction'][] = ['GENERAL_MANAGEMENT', 'FINANCE', 'HR', 'SALES', 'MARKETING', 'OPERATIONS', 'SUPPLY_CHAIN', 'PROCUREMENT', 'ENGINEERING', 'IT', 'RND', 'LEGAL', 'OTHER'];

export function BulkAddDialog({ jobId, existing, opened, onClose, onAdded }: { jobId: string; existing: Set<string>; opened: boolean; onClose: () => void; onAdded: () => void }) {
  const { t } = useTranslation();
  const [q, setQ] = useState('');
  const [city, setCity] = useState('');
  const [fn, setFn] = useState<string | null>(null);
  const [pool, setPool] = useState<Schemas['PersonSummary'][]>([]);
  const [picked, setPicked] = useState<Set<string>>(new Set());
  const [failed, setFailed] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!opened) return;
    let live = true;
    api
      .GET('/people', { params: { query: { q: q || undefined, city: city ? [city] : undefined, functionArea: fn ? [fn as Schemas['JobFunction']] : undefined, size: 100 } } })
      .then((r) => live && setPool((r.data?.content ?? []).filter((p) => !existing.has(p.id))))
      .catch(() => undefined);
    return () => {
      live = false;
    };
  }, [opened, q, city, fn, existing]);

  const allOn = pool.length > 0 && pool.every((p) => picked.has(p.id));
  const toggle = (id: string) =>
    setPicked((s) => {
      const n = new Set(s);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });
  const add = async () => {
    setBusy(true);
    const chosen = pool.filter((p) => picked.has(p.id));
    const results = await Promise.all(chosen.map((p) => api.POST('/job-orders/{id}/applications', { params: { path: { id: jobId } }, body: { personId: p.id } })));
    const bad = results.flatMap((r, i) => (r.error ? [`${chosen[i]?.name}: ${problemText(r.error, t('pipeline.addFailed'))}`] : []));
    setBusy(false);
    setFailed(bad);
    setPicked(new Set(chosen.filter((_, i) => results[i]?.error).map((p) => p.id)));
    onAdded();
    if (bad.length === 0) onClose();
  };

  return (
    <Modal opened={opened} onClose={onClose} title={t('pipeline.ll.bulkTitle')} size="lg" centered>
      <Stack>
        <Group grow align="flex-end">
          <TextInput label={t('pipeline.searchPeople')} value={q} onChange={(e) => setQ(e.currentTarget.value)} data-autofocus />
          <TextInput label={t('pipeline.ll.city')} value={city} onChange={(e) => setCity(e.currentTarget.value)} />
          <Select label={t('pipeline.ll.function')} data={FUNCTIONS.map((f) => ({ value: f, label: t(`pipeline.ll.fn.${f}`) }))} value={fn} onChange={setFn} clearable />
        </Group>
        {failed.length > 0 && (
          <Alert color="red" role="alert" data-testid="bulk-failed">
            {failed.map((m) => (
              <div key={m}>{m}</div>
            ))}
          </Alert>
        )}
        <Checkbox label={t('pipeline.ll.selectAll', { count: pool.length })} checked={allOn} indeterminate={!allOn && picked.size > 0} onChange={() => setPicked(allOn ? new Set() : new Set(pool.map((p) => p.id)))} />
        <Stack gap="xs" mah="24rem" style={{ overflowY: 'auto' }}>
          {pool.length === 0 && (
            <Text size="sm" c="var(--color-ink3)">
              {t('pipeline.ll.poolEmpty')}
            </Text>
          )}
          {pool.map((p) => (
            <Checkbox key={p.id} checked={picked.has(p.id)} onChange={() => toggle(p.id)} label={`${p.name} – ${[p.title, p.company, p.city].filter(Boolean).join(', ')}`} />
          ))}
        </Stack>
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('pipeline.ll.cancel')}
          </Button>
          <Button disabled={picked.size === 0} loading={busy} onClick={() => void add()}>
            {t('pipeline.ll.addSelected', { count: picked.size })}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
