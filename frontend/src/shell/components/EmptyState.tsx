import { Card, Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

/** UX-08: every empty list says what to do next. */
export function EmptyState({ title, hint, actions }: { title: string; hint?: string; actions?: ReactNode }) {
  return (
    <Card withBorder padding="xl" data-testid="empty-state">
      <Stack align="center" gap="xs" ta="center">
        <Title order={2} size="h3">{title}</Title>
        {hint && <Text c="dimmed" maw="60ch">{hint}</Text>}
        {actions && <Group justify="center" mt="xs">{actions}</Group>}
      </Stack>
    </Card>
  );
}
