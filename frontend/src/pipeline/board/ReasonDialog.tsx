import { Button, Chip, Group, Modal, Stack, Textarea } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

/** Asks for a reason (3 to 500 chars, per Reason in the contract). Chips fill the text. */
export function ReasonDialog({
  opened,
  title,
  chips,
  onCancel,
  onConfirm,
}: {
  opened: boolean;
  title: string;
  chips?: string[];
  onCancel: () => void;
  onConfirm: (reason: string) => void;
}) {
  const { t } = useTranslation();
  const [text, setText] = useState('');
  useEffect(() => {
    if (opened) setText('');
  }, [opened]);
  const trimmed = text.trim();
  const valid = trimmed.length >= 3 && trimmed.length <= 500;
  return (
    <Modal opened={opened} onClose={onCancel} title={title} centered>
      <Stack>
        {chips && (
          <Chip.Group multiple={false} value={chips.includes(text) ? text : ''} onChange={(v) => setText(String(v))}>
            <Group gap="xs">
              {chips.map((c) => (
                <Chip key={c} value={c} size="sm">
                  {c}
                </Chip>
              ))}
            </Group>
          </Chip.Group>
        )}
        <Textarea
          label={t('pipeline.reason.label')}
          value={text}
          onChange={(e) => setText(e.currentTarget.value)}
          maxLength={500}
          autosize
          minRows={2}
          data-autofocus
        />
        <Group justify="flex-end">
          <Button variant="default" onClick={onCancel}>
            {t('common.cancel')}
          </Button>
          <Button disabled={!valid} onClick={() => onConfirm(trimmed)}>
            {t('common.confirm')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
