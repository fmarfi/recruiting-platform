// CV files: drag-and-drop upload, version list, download and text extraction status (UX: person page).
import { Alert, Anchor, Badge, Stack, Table, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { problemCode, problemText, useFormat, type Schemas } from '../../pipeline/shared';
import { DropZone } from '../files/DropZone';
import { formatSize, splitFiles } from '../files/fileRules';
import { personFileUrl, uploadPersonFile, usePersonFiles } from '../files/useFiles';

const STATUS_COLOR: Record<Schemas['TextExtractionStatus'], string> = { PENDING: 'gray', DONE: 'green', NO_TEXT: 'yellow', FAILED: 'red' };

export function FilesTab({ personId, anonymised, onUploaded }: { personId: string; anonymised: boolean; onUploaded: () => void }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const files = usePersonFiles(personId);
  const [busy, setBusy] = useState(false);
  const [msgs, setMsgs] = useState<{ color: string; text: string }[]>([]);

  const upload = async (picked: File[]) => {
    const { ok, rejected } = splitFiles(picked);
    const next: typeof msgs = rejected.map((r) => ({ color: 'red', text: `${r.fileName}: ${t(`network.files.reject.${r.code}`)}` }));
    setBusy(true);
    for (const i of ok) {
      const f = picked[i]!;
      try {
        const r = await uploadPersonFile(personId, f);
        if (r.error) {
          const code = problemCode(r.error);
          next.push({ color: code === 'DUPLICATE' ? 'yellow' : 'red', text: `${f.name}: ${code === 'DUPLICATE' ? t('network.files.duplicateFile') : problemText(r.error, t('network.files.uploadFailed'))}` });
        }
      } catch {
        next.push({ color: 'red', text: `${f.name}: ${t('network.files.uploadFailed')}` });
      }
    }
    setBusy(false);
    setMsgs(next);
    files.reload();
    onUploaded();
  };

  const rows = files.data ?? [];
  return (
    <Stack>
      {anonymised ? <Alert color="gray">{t('network.files.anonymised')}</Alert> : <DropZone onFiles={(f) => void upload(f)} busy={busy} />}
      {msgs.map((m, i) => (
        <Alert key={i} color={m.color} role="alert" withCloseButton onClose={() => setMsgs(msgs.filter((_, j) => j !== i))}>
          {m.text}
        </Alert>
      ))}
      <Text size="xs" c="dimmed">
        {t('network.files.consentNote')}
      </Text>
      {!!files.error && <Alert color="red">{t('network.files.loadFailed')}</Alert>}
      {!files.loading && !files.error && rows.length === 0 && <Text c="dimmed">{t('network.files.empty')}</Text>}
      {rows.length > 0 && (
        <Table.ScrollContainer minWidth={520}>
          <Table data-testid="file-versions">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{t('network.files.file')}</Table.Th>
                <Table.Th>{t('network.files.version')}</Table.Th>
                <Table.Th>{t('network.files.uploaded')}</Table.Th>
                <Table.Th>{t('network.files.text')}</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rows.map((f) => (
                <Table.Tr key={f.id}>
                  <Table.Td>
                    <Anchor href={personFileUrl(personId, f.id)} download>
                      {f.fileName}
                    </Anchor>{' '}
                    <Text span size="xs" c="dimmed">
                      {formatSize(f.sizeBytes)}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    {f.kind === 'CV' ? `v${f.version}` : t('network.files.document')}{' '}
                    {f.current && (
                      <Badge size="xs" color="blue" variant="light">
                        {t('network.files.current')}
                      </Badge>
                    )}
                  </Table.Td>
                  <Table.Td>
                    {fmt.dateLong(f.uploadedAt)} - {f.uploadedBy.name}
                  </Table.Td>
                  <Table.Td>
                    <Badge color={STATUS_COLOR[f.textStatus]} variant="light">
                      {t(`network.files.status.${f.textStatus}`)}
                    </Badge>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
    </Stack>
  );
}
