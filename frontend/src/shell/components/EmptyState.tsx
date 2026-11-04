import { Group, Stack, Text } from '@mantine/core';
import type { ReactNode } from 'react';

/** UX-08 and v3: one sentence plus an optional action, centred, no illustration and no box. */
export function EmptyState({ title, hint, actions }: { title: string; hint?: string; actions?: ReactNode }) {
  return (
    <Stack align="center" gap="xs" ta="center" py="xl" data-testid="empty-state">
      <Text c="dimmed" size="sm">{title}</Text>
      {hint && <Text c="dimmed" size="xs" maw="60ch">{hint}</Text>}
      {actions && <Group justify="center" mt="xs">{actions}</Group>}
    </Stack>
  );
}
