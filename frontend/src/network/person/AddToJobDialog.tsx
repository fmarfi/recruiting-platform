// Add to a job: open job orders the person is not yet on; the application starts at Sourced (A-07).
import { Button, Group, Modal, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemText, useAsync } from '../../pipeline/shared';

export function AddToJobDialog({ personId, exclude, opened, onClose, onSaved }: { personId: string; exclude: string[]; opened: boolean; onClose: () => void; onSaved: () => void }) {
  const { t } = useTranslation();
  const [err, setErr] = useState<string>();
  const jobs = useAsync(() => (opened ? api.GET('/job-orders', { params: { query: { open: true, size: 50 } } }) : Promise.resolve({ data: undefined, error: undefined })), [opened]);
  const add = async (jobId: string) => {
    const r = await api.POST('/job-orders/{id}/applications', { params: { path: { id: jobId } }, body: { personId } });
    if (r.error) setErr(problemText(r.error, t('pipeline.addFailed')));
    else {
      onSaved();
      onClose();
    }
  };
  const list = (jobs.data?.content ?? []).filter((j) => !exclude.includes(j.id));
  return (
    <Modal opened={opened} onClose={onClose} title={t('network.addToJob')} centered>
      <Stack>
        {list.length === 0 && <Text c="dimmed">{t('network.noOpenJobs')}</Text>}
        {list.map((j) => (
          <Group key={j.id} justify="space-between" wrap="nowrap">
            <Text size="sm">
              {j.title} <Text span c="dimmed">- {j.account.name}</Text>
            </Text>
            <Button size="compact-sm" onClick={() => void add(j.id)}>
              {t('pipeline.add')}
            </Button>
          </Group>
        ))}
        {err && (
          <Text c="red" size="sm" role="alert">
            {err}
          </Text>
        )}
      </Stack>
    </Modal>
  );
}
