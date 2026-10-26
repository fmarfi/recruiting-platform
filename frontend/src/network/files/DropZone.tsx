// Drag-and-drop file area with a real button for keyboard and phone users. No upload logic: hands File[] to the parent.
import { Button, Group, Paper, Stack, Text } from '@mantine/core';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ACCEPT_ATTR } from './fileRules';

export function DropZone({ onFiles, multiple, busy, hint }: { onFiles: (files: File[]) => void; multiple?: boolean; busy?: boolean; hint?: string }) {
  const { t } = useTranslation();
  const input = useRef<HTMLInputElement>(null);
  const [over, setOver] = useState(false);
  const take = (list: FileList | null) => {
    const files = Array.from(list ?? []);
    if (files.length) onFiles(multiple ? files : files.slice(0, 1));
  };
  return (
    <Paper
      withBorder
      p="md"
      data-testid="dropzone"
      data-over={over}
      style={{ borderStyle: 'dashed', background: over ? 'var(--mantine-color-blue-0)' : undefined }}
      onDragOver={(e) => {
        e.preventDefault();
        setOver(true);
      }}
      onDragLeave={() => setOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setOver(false);
        if (!busy) take(e.dataTransfer.files);
      }}
    >
      <Stack gap={4} align="center">
        <Text size="sm" fw={600}>
          {t(multiple ? 'network.files.dropMany' : 'network.files.dropOne')}
        </Text>
        <Text size="xs" c="dimmed">
          {hint ?? t('network.files.limits')}
        </Text>
        <Group>
          <Button variant="light" loading={busy} onClick={() => input.current?.click()}>
            {t(multiple ? 'network.files.chooseMany' : 'network.files.chooseOne')}
          </Button>
        </Group>
        <input
          ref={input}
          type="file"
          hidden
          multiple={multiple}
          accept={ACCEPT_ATTR}
          data-testid="dropzone-input"
          onChange={(e) => {
            take(e.currentTarget.files);
            e.currentTarget.value = '';
          }}
        />
      </Stack>
    </Paper>
  );
}
