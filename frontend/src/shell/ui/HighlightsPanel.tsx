// Highlights panel: 4 to 7 label-over-value pairs directly under the page header.
import { Box, Text } from '@mantine/core';
import type { ReactNode } from 'react';

export interface Highlight {
  label: string;
  value: ReactNode;
}

/** Wraps to two columns on phones; empty values show a dash. */
export function HighlightsPanel({ items }: { items: Highlight[] }) {
  return (
    <Box
      mb="md"
      p="md"
      data-testid="highlights"
      style={{
        display: 'grid',
        gap: 'var(--space-3)',
        gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
        background: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-md)',
      }}
    >
      {items.map((h) => (
        <Box key={h.label} style={{ minWidth: 0 }}>
          <Text fz="xs" c="dimmed">{h.label}</Text>
          <Text fw={500} truncate component="div">{h.value ?? '-'}</Text>
        </Box>
      ))}
    </Box>
  );
}
