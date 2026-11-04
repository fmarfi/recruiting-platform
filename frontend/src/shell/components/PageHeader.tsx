import { Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * Visual system v3 section 5: optional breadcrumb, serif title with a badge slot, optional description, actions on the
 * right (secondary first, the single primary last). `subtitle` is the description. Actions wrap on narrow screens.
 */
export function PageHeader({
  title,
  subtitle,
  actions,
  breadcrumb,
  badge,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  breadcrumb?: ReactNode;
  badge?: ReactNode;
}) {
  return (
    <Group justify="space-between" align="flex-start" gap="sm" mb="xl" wrap="wrap">
      <Stack gap="xs" style={{ flex: '1 1 280px', minWidth: 0 }}>
        {breadcrumb && (
          <Text size="xs" c="dimmed" data-testid="screen-breadcrumb">
            {breadcrumb}
          </Text>
        )}
        <Group gap="sm" align="center" wrap="wrap">
          <Title order={1} data-testid="screen-title" ff="var(--font-display)" style={{ overflowWrap: 'anywhere' }}>
            {title}
          </Title>
          {badge}
        </Group>
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
