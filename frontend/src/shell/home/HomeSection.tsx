import { Box, Group, Paper, Title } from '@mantine/core';
import type { ReactNode } from 'react';

/** Hairline used between sections inside a panel (visual system v3 section 4). */
export const HOME_HAIRLINE = '1px solid var(--color-border)';

/** The one white panel per column on the home page. No shadow; the border carries the edge. */
export function HomePanel({ children }: { children: ReactNode }) {
  return (
    <Paper withBorder p="md" style={{ boxShadow: 'none' }}>
      {children}
    </Paper>
  );
}

/**
 * Section on the home page: h2 16/600 with an optional right-aligned text link, then a plain list. Not a card. Inside a
 * panel, a section after the first is split off by a hairline and 16 px of space.
 */
export function HomeSection({ title, link, divided = false, children }: { title: string; link?: ReactNode; divided?: boolean; children: ReactNode }) {
  return (
    <Box component="section" pt={divided ? 'md' : 0} mt={divided ? 'md' : 0} style={divided ? { borderTop: HOME_HAIRLINE } : undefined}>
      <Group justify="space-between" align="center" wrap="nowrap" gap="sm" mb="var(--space-2)">
        <Title order={2} fz="1rem" fw={600}>
          {title}
        </Title>
        {link}
      </Group>
      {children}
    </Box>
  );
}

/** Row style: 1 px divider above every row except the first in its list. */
export function rowDivider(divided: boolean) {
  return { borderTop: divided ? HOME_HAIRLINE : undefined };
}
