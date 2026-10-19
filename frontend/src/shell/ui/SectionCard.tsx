// Lightning-style card: white surface, 1 px border, 44 px header with title left and actions right.
import { Box, Group, Text } from '@mantine/core';
import type { ReactNode } from 'react';

interface Props {
  title?: ReactNode;
  /** Short text after the title, for example a count "(3)". */
  count?: number;
  /** Right side of the header: a button or menu. */
  actions?: ReactNode;
  children: ReactNode;
  /** Removes the body padding, for tables that should touch the card edges. */
  flush?: boolean;
  'data-testid'?: string;
}

/** Shared card used by record pages, related lists and dashboard blocks. */
export function SectionCard({ title, count, actions, children, flush, ...rest }: Props) {
  return (
    <Box
      component="section"
      data-testid={rest['data-testid']}
      style={{
        background: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-md)',
        minWidth: 0,
      }}
    >
      {title && (
        <Group justify="space-between" wrap="nowrap" px="md" h={44} style={{ borderBottom: '1px solid var(--color-border)' }}>
          <Text fw={600} truncate>
            {title}
            {count !== undefined && <Text span c="dimmed" fw={400}> ({count})</Text>}
          </Text>
          {actions}
        </Group>
      )}
      <Box p={flush ? 0 : 'md'}>{children}</Box>
    </Box>
  );
}
