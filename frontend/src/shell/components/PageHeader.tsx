import { Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * One h1, a one-line purpose or count under it, and the actions on the right: secondary actions first, the single
 * primary action last (UX-02). Actions wrap under the title on narrow screens instead of overflowing.
 */
export function PageHeader({ title, subtitle, actions }: { title: ReactNode; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <Group justify="space-between" align="flex-start" gap="sm" mb="md" wrap="wrap">
      <Stack gap={2} style={{ flex: '1 1 280px', minWidth: 0 }}>
        <Title order={1} data-testid="screen-title" style={{ overflowWrap: 'anywhere' }}>
          {title}
        </Title>
        {subtitle && (
          <Text c="dimmed" size="sm" data-testid="screen-subtitle">
            {subtitle}
          </Text>
        )}
      </Stack>
      {actions && (
        <Group gap="xs" wrap="wrap" justify="flex-end" data-testid="page-actions">
          {actions}
        </Group>
      )}
    </Group>
  );
}
