// Signed copy of an agreement: upload or replace a PDF (PUT /agreements/{id}/signed-file), shows name, size, who and when.
import { Alert, Anchor, Button, FileButton, Group, Text } from '@mantine/core';
import { IconFileTypePdf, IconUpload } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api, API_BASE, type components } from '../../api';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';
import { fmtDate } from '../../shell/lib/format';
import { problemText } from '../../shell/lib/problem';
import { checkSignedFile, fmtSize } from './fileCheck';

type Agreement = components['schemas']['Agreement'];

export function SignedCopy({ agreement: a, onUploaded }: { agreement: Agreement; onUploaded: () => void }) {
  const { t, i18n } = useTranslation();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const f = a.signedFile;

  const pick = async (file: File | null) => {
    if (!file) return;
    const check = checkSignedFile(file);
    if (check !== 'ok') return setError(t(`agr.file.${check}`));
    setBusy(true);
    setError(null);
    const r = await api.PUT('/agreements/{id}/signed-file', {
      params: { path: { id: a.id } },
      body: { file: file as unknown as string, version: a.version },
      bodySerializer: (b) => {
        const fd = new FormData();
        fd.append('file', file);
        if (b?.version !== undefined) fd.append('version', String(b.version));
        return fd;
      },
    });
    setBusy(false);
    if (r.error) {
      const status = r.response?.status;
      return setError(status === 409 ? t('agr.file.conflict') : problemText(r.error, t('agr.file.failed')));
    }
    onUploaded();
  };

  return (
    <div>
      <Group justify="space-between" align="flex-start" wrap="nowrap" gap="sm">
        {f ? (
          <div style={{ minWidth: 0 }}>
            <Group gap="xs" wrap="nowrap">
              <IconFileTypePdf size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden color="var(--color-ink3)" />
              <Anchor href={`${API_BASE}/agreements/${a.id}/signed-file`} size="sm" fw={500} style={{ overflowWrap: 'anywhere' }}>
                {f.fileName}
              </Anchor>
            </Group>
            <Text size="sm" c="var(--color-ink3)" mt={4}>
              {t('agr.file.meta', { size: fmtSize(f.sizeBytes), who: f.uploadedBy.name, when: fmtDate(f.uploadedAt, i18n.language) })}
            </Text>
          </div>
        ) : (
          <Text size="sm" c="var(--color-ink3)">
            {t('agr.file.none')}
          </Text>
        )}
        <FileButton onChange={(x) => void pick(x)} accept="application/pdf">
          {(props) => (
            <Button {...props} variant="default" size="compact-md" loading={busy} leftSection={<IconUpload size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />}>
              {f ? t('agr.file.replace') : t('agr.file.upload')}
            </Button>
          )}
        </FileButton>
      </Group>
      {error && (
        <Alert color="red" role="alert" mt="xs" py="xs">
          {error}
        </Alert>
      )}
    </div>
  );
}
