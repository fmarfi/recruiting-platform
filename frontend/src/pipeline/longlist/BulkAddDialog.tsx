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

  const [poolErr, setPoolErr] = useState(false);
  const [summary, setSummary] = useState<string>();
  const [dq, setDq] = useState({ q, city });
  useEffect(() => {
    const h = setTimeout(() => setDq({ q, city }), 300);
    return () => clearTimeout(h);
  }, [q, city]);

  useEffect(() => {
    if (!opened) return;
    let live = true;
    api
      .GET('/people', { params: { query: { q: dq.q || undefined, city: dq.city ? [dq.city] : undefined, functionArea: fn ? [fn as Schemas['JobFunction']] : undefined, size: 100 } } })
      .then((r) => {
        if (!live) return;
        setPoolErr(!!r.error);
        if (!r.error) setPool((r.data?.content ?? []).filter((p) => !existing.has(p.id)));
      })
      .catch(() => live && setPoolErr(true));
    return () => {
      live = false;
    };
  }, [opened, dq, fn, existing]);

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
    const bad: string[] = [];
    const failedIds = new Set<string>();
    try {
      for (const p of chosen) {
        try {
          const r = await api.POST('/job-orders/{id}/applications', { params: { path: { id: jobId } }, body: { personId: p.id } });
          if (r.error) throw r.error;
        } catch (e) {
          failedIds.add(p.id);
          bad.push(`${p.name}: ${problemText(e, t('pipeline.addFailed'))}`);
        }
      }
    } finally {
      setBusy(false);
    }
    const added = chosen.length - failedIds.size;
    setFailed(bad);
    setSummary(t('pipeline.ll.addResult', { added, failed: failedIds.size }));
    setPicked(failedIds);
    if (added > 0) onAdded();
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
        {poolErr && (
          <Alert color="red" role="alert" data-testid="bulk-pool-error">
            {t('pipeline.ll.poolFailed')}
          </Alert>
        )}
        {failed.length > 0 && (
          <Alert color="red" role="alert" data-testid="bulk-failed">
            <div data-testid="bulk-summary">{summary}</div>
            {failed.map((m) => (
              <div key={m}>{m}</div>
            ))}
          </Alert>
        )}
        <Checkbox label={t('pipeline.ll.selectAll', { count: pool.length })} checked={allOn} indeterminate={!allOn && picked.size > 0} onChange={() => setPicked(allOn ? new Set() : new Set(pool.map((p) => p.id)))} />
        <Stack gap="xs" mah="24rem" style={{ overflowY: 'auto' }}>
          {pool.length === 0 && !poolErr && (
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
