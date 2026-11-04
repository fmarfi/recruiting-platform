import { Group, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * Visual system v3 section 4: white region, 1 px border, radius 8, no shadow. Header = h2 16/600 with optional count and
 * right-aligned actions over a 1 px divider. `padding="none"` is for tables and lists that draw their own row dividers.
 */
export function Panel({
  title,
  count,
  actions,
  children,
  padding = 'md',
  empty,
  testId,
}: {
  title: ReactNode;
  count?: number;
  actions?: ReactNode;
  children?: ReactNode;
  padding?: 'md' | 'none';
  empty?: ReactNode;
  testId?: string;
}) {
  return (
    <section
      data-testid={testId}
      style={{ background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 'var(--space-2)', minWidth: 0 }}
    >
      <Group
        justify="space-between"
        align="center"
        wrap="nowrap"
        gap="sm"
        style={{ padding: 'var(--space-3) var(--space-4)', borderBottom: '1px solid var(--color-border)' }}
      >
        <Group gap="xs" align="baseline" wrap="nowrap" style={{ minWidth: 0 }}>
          <Title order={2} fz="1rem" fw={600} c="var(--color-ink)">
            {title}
          </Title>
          {count !== undefined && (
            <Text span fz="0.875rem" c="var(--color-ink3)" style={{ fontVariantNumeric: 'tabular-nums' }}>
              {count}
            </Text>
          )}
        </Group>
        {actions && (
          <Group gap="xs" wrap="nowrap">
            {actions}
          </Group>
        )}
      </Group>
      <div style={{ padding: padding === 'md' ? 'var(--space-4)' : 0 }}>
        {empty ? (
          <Text ta="center" size="sm" c="var(--color-ink3)" py="md" data-testid="panel-empty">
            {empty}
          </Text>
        ) : (
          children
        )}
      </div>
    </section>
  );
}
