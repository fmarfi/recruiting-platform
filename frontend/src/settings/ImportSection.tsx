import { Alert, Anchor, Button, FileInput, Group, Select, Stack, Table, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../api';
import { ErrorBanner } from '../shell/components/ErrorBanner';
import { ToneBadge } from '../shell/components/ToneBadge';
import { useAsync } from '../shell/hooks/useAsync';
import { fmtDate } from '../shell/lib/format';
import { problemText } from '../shell/lib/problem';

type Job = components['schemas']['ImportJob'];
type Kind = components['schemas']['ImportKind'];
const KINDS: Kind[] = ['ACCOUNTS', 'CONTACTS', 'PEOPLE'];

/** Import (A-11): upload a CSV, review the dry run, then commit the valid rows. Managers and admins. */
export function ImportSection() {
  const { t, i18n } = useTranslation();
  const [kind, setKind] = useState<Kind>('ACCOUNTS');
  const [file, setFile] = useState<File | null>(null);
  const [job, setJob] = useState<Job>();
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>();
  const past = useAsync(() => api.GET('/imports', { params: { query: { size: 10 } } }), []);

  const run = async (fn: () => Promise<{ data?: Job; error?: unknown }>, keep = true) => {
    setBusy(true);
    setErr(undefined);
    try {
      const r = await fn();
      if (r.error) setErr(problemText(r.error, t('settings.import.failed')));
      else setJob(keep ? r.data : undefined);
    } catch {
      setErr(t('settings.import.failed'));
    } finally {
      setBusy(false);
      past.reload();
    }
  };
  const upload = () =>
    run(() =>
      api.POST('/imports', {
        body: { kind, file: file as unknown as string },
        bodySerializer: () => {
          const fd = new FormData();
          fd.append('kind', kind);
          fd.append('file', file as File);
          return fd;
        },
      }),
    );
  const id = job?.id ?? '';
  const discard = () =>
    run(async () => {
      const r = await api.DELETE('/imports/{id}', { params: { path: { id } } });
      return { error: r.error };
    }, false);

  return (
    <Stack>
      <Group align="flex-end" wrap="wrap">
        <Select label={t('settings.import.kind')} allowDeselect={false} value={kind} onChange={(v) => v && setKind(v as Kind)} data={KINDS.map((k) => ({ value: k, label: t(`settings.import.${k}`) }))} />
        <FileInput label={t('settings.import.file')} accept=".csv,.xlsx" value={file} onChange={setFile} clearable />
        <Button onClick={() => void upload()} disabled={!file} loading={busy}>{t('settings.import.upload')}</Button>
        <Anchor href={`/api/v1/import-templates/${kind}`} download size="sm">{t('settings.import.template')}</Anchor>
      </Group>
      {err && <Alert color="red" role="alert">{err}</Alert>}
      {job && (
        <Stack gap="xs">
          <Group gap="lg">
            <Text size="sm">{t('settings.import.total')}: {job.totalRows}</Text>
            <Text size="sm">{t('settings.import.valid')}: {job.validRows}</Text>
            <Text size="sm">{t('settings.import.duplicates')}: {job.duplicateRows}</Text>
            <Text size="sm">{t('settings.import.imported')}: {job.importedRows}</Text>
            <ToneBadge size="sm" tone={job.status === 'FAILED' ? 'BAD' : job.status === 'COMPLETED' ? 'GOOD' : 'INFO'} label={job.status} />
          </Group>
          {job.failureReason && <Text c="red" size="sm">{job.failureReason}</Text>}
          {job.errors.length > 0 && (
            <Table withTableBorder>
              <Table.Thead>
                <Table.Tr><Table.Th>{t('settings.import.row')}</Table.Th><Table.Th>{t('settings.import.message')}</Table.Th></Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {job.errors.slice(0, 50).map((e, i) => (
                  <Table.Tr key={i}><Table.Td>{e.row}</Table.Td><Table.Td>{[e.column, e.message].filter(Boolean).join(': ')}</Table.Td></Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
          {job.status === 'VALIDATED' && (
            <Group>
              <Button onClick={() => void run(() => api.POST('/imports/{id}/commit', { params: { path: { id } } }))} loading={busy}>{t('settings.import.commit')}</Button>
              <Button variant="default" onClick={() => void discard()}>{t('settings.import.discard')}</Button>
            </Group>
          )}
        </Stack>
      )}
      <Text fw={600}>{t('settings.import.past')}</Text>
      {past.error !== undefined && <ErrorBanner onRetry={past.reload} />}
      {past.data && past.data.content.length === 0 && <Text c="dimmed" size="sm">{t('settings.common.none')}</Text>}
      {past.data && past.data.content.length > 0 && (
        <Table withTableBorder>
          <Table.Thead>
            <Table.Tr><Table.Th>{t('settings.common.date')}</Table.Th><Table.Th>{t('settings.import.kind')}</Table.Th><Table.Th>{t('settings.import.imported')}</Table.Th><Table.Th>{t('settings.import.status')}</Table.Th></Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {past.data.content.map((j) => (
              <Table.Tr key={j.id}>
                <Table.Td>{fmtDate(j.createdAt, i18n.language)}</Table.Td>
                <Table.Td>{t(`settings.import.${j.kind}`)}</Table.Td>
                <Table.Td>{j.importedRows}/{j.totalRows}</Table.Td>
                <Table.Td>{j.status}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  );
}
