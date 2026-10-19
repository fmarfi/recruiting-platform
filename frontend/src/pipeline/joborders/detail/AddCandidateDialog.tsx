// Searchable list of people not yet on the job order (GET candidate-suggestions); adds with POST applications.
import { Button, Group, Modal, Stack, Text, TextInput } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../../api';
import { problemText, type Schemas } from '../../shared';

export function AddCandidateDialog({ jobId, opened, onClose, onAdded }: { jobId: string; opened: boolean; onClose: () => void; onAdded: () => void }) {
  const { t } = useTranslation();
  const [q, setQ] = useState('');
  const [list, setList] = useState<Schemas['PersonSummary'][]>([]);
  const [err, setErr] = useState<string>();
  useEffect(() => {
    if (!opened) return;
    let live = true;
    api
      .GET('/job-orders/{id}/candidate-suggestions', { params: { path: { id: jobId }, query: { q: q || undefined, size: 10 } } })
      .then((r) => live && setList(r.data?.content ?? []))
      .catch(() => undefined);
    return () => {
      live = false;
    };
  }, [opened, q, jobId]);
  const add = async (p: Schemas['PersonSummary']) => {
    const r = await api.POST('/job-orders/{id}/applications', { params: { path: { id: jobId } }, body: { personId: p.id } });
    if (r.error) setErr(problemText(r.error, t('pipeline.addFailed')));
    else {
      setErr(undefined);
      onAdded();
      onClose();
    }
  };
  return (
    <Modal opened={opened} onClose={onClose} title={t('pipeline.addCandidate')} centered>
      <Stack>
        <TextInput label={t('pipeline.searchPeople')} value={q} onChange={(e) => setQ(e.currentTarget.value)} data-autofocus />
        {err && (
          <Text c="red" size="sm" role="alert">
            {err}
          </Text>
        )}
        {list.map((p) => (
          <Group key={p.id} justify="space-between" wrap="nowrap">
            <Text size="sm">
              {p.name}
              <Text span c="dimmed" size="xs">
                {' '}
                {[p.title, p.company].filter(Boolean).join(', ')}
              </Text>
            </Text>
            <Button size="compact-sm" onClick={() => void add(p)}>
              {t('pipeline.add')}
            </Button>
          </Group>
        ))}
      </Stack>
    </Modal>
  );
}
