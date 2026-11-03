import { Box, Group, Text } from '@mantine/core';
import type { ReactNode } from 'react';

/** Empty section on the account page: one sentence and, when one exists, the primary action for it. */
export function SectionEmpty({ text, action }: { text: string; action?: ReactNode }) {
  return (
    <Box data-testid="empty-state" p="md" style={{ border: 'thin dashed var(--color-borderStrong)', borderRadius: 'var(--radius-md)', background: 'var(--color-surface)' }}>
      <Group justify="space-between" gap="sm">
        <Text c="dimmed">{text}</Text>
        {action}
      </Group>
    </Box>
  );
}
