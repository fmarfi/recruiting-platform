import { Box, Text } from '@mantine/core';
import type { ReactNode } from 'react';

export interface Highlight {
  label: string;
  value: ReactNode;
}

/** Highlights panel (lightning-style.md): 4 to 7 label-over-value pairs that wrap on narrow screens. */
export function Highlights({ items, testId }: { items: Highlight[]; testId?: string }) {
  return (
    <Box
      component="dl"
      m={0}
      data-testid={testId}
      style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 'var(--space-3) var(--space-5)' }}
    >
      {items.map((h) => (
        <div key={h.label} style={{ minWidth: 0 }}>
          <Text component="dt" size="xs" c="dimmed" fw={600} tt="uppercase" style={{ letterSpacing: '0.04em' }}>
            {h.label}
          </Text>
          <Text component="dd" m={0} fw={600} style={{ overflowWrap: 'anywhere' }}>
            {h.value}
          </Text>
        </div>
      ))}
    </Box>
  );
}
