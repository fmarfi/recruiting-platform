// Merge dialog (MANAGER/ADMIN): pick the survivor, which fields come from the other record, confirm. Needs the survivor's version.
import { Alert, Button, Checkbox, Group, Loader, Modal, SegmentedControl, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { problemCode, problemText, useAsync, type Schemas } from '../../pipeline/shared';
import { MERGE_FIELDS, mergeRequest, type Take } from './duplicateLogic';

type Pair = Schemas['DuplicateCandidate'];

export function MergeDialog({ pair, onClose, onMerged }: { pair: Pair | undefined; onClose: () => void; onMerged: (survivorId: string) => void }) {
  const { t } = useTranslation();
  const [survivor, setSurvivor] = useState<'A' | 'B'>('A');
  const [take, setTake] = useState<Take>([]);
  const [err, setErr] = useState<string>();
  const [busy, setBusy] = useState(false);
  const keep = pair && (survivor === 'A' ? pair.personA : pair.personB);
  const drop = pair && (survivor === 'A' ? pair.personB : pair.personA);
  // The version of the survivor is read fresh when the dialog opens or the survivor changes; a 409 reloads it.
  const loaded = useAsync<Schemas['Person']>(() => (keep ? api.GET('/people/{id}', { params: { path: { id: keep.id } } }) : Promise.resolve({})), [keep?.id]);

  const merge = async () => {
    if (!keep || !drop) return;
    const body = mergeRequest(drop.id, loaded.data?.version, take);
    if (!body) return;
    setBusy(true);
    setErr(undefined);
    try {
      const r = await api.POST('/people/{id}/merge', { params: { path: { id: keep.id } }, body });
      if (r.error) {
        const code = problemCode(r.error);
        setErr(code === 'VERSION_CONFLICT' ? t('network.dup.versionConflict') : code === 'FORBIDDEN' ? t('network.dup.forbidden') : problemText(r.error, t('network.dup.mergeFailed')));
        if (code === 'VERSION_CONFLICT') loaded.reload();
      } else {
        onMerged(keep.id);
        onClose();
      }
    } catch {
      setErr(t('network.dup.mergeFailed'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal opened={!!pair} onClose={onClose} title={t('network.dup.mergeTitle')} centered size="lg">
      {pair && keep && drop && (
        <Stack>
          <Text size="sm">{t('network.dup.survivorHelp')}</Text>
          <SegmentedControl
            value={survivor}
            onChange={(v) => {
              setSurvivor(v as 'A' | 'B');
              setErr(undefined);
            }}
            data={[
              { value: 'A', label: pair.personA.name },
              { value: 'B', label: pair.personB.name },
            ]}
          />
          <Text size="sm" data-testid="merge-summary">
            {t('network.dup.mergeSummary', { keep: keep.name, drop: drop.name })}
          </Text>
          <Text size="sm" fw={600}>
            {t('network.dup.takeTitle', { name: drop.name })}
          </Text>
          <Checkbox.Group value={take} onChange={(v) => setTake(v as Take)}>
            <Group gap="md">
              {MERGE_FIELDS.map((k) => (
                <Checkbox key={k} value={k} label={t(`network.dup.field.${k}`)} size="sm" />
              ))}
            </Group>
          </Checkbox.Group>
          <Alert color="orange">{t('network.dup.irreversible')}</Alert>
          {err && (
            <Alert color="red" role="alert">
              {err}
            </Alert>
          )}
          {!!loaded.error && <Alert color="red">{t('network.dup.loadSurvivorFailed')}</Alert>}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button color="red" disabled={loaded.data?.version == null} loading={busy} onClick={() => void merge()} leftSection={loaded.loading ? <Loader size="xs" /> : undefined}>
              {t('network.dup.mergeConfirm')}
            </Button>
          </Group>
        </Stack>
      )}
    </Modal>
  );
}
