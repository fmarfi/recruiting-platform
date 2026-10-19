// 12-column page grid: 8 + 4 on laptop, stacked below 960 px (see docs/ux/lightning-style.md section 1).
import { Box } from '@mantine/core';
import type { ReactNode } from 'react';

/** Page body with the record-page layout; `side` drops under `main` on narrow screens. */
export function PageGrid({ main, side }: { main: ReactNode; side?: ReactNode }) {
  return (
    <Box
      style={{
        display: 'grid',
        gap: 'var(--space-3)',
        gridTemplateColumns: side ? 'repeat(auto-fit, minmax(min(100%, 320px), 1fr))' : '1fr',
        alignItems: 'start',
      }}
    >
      <Box style={{ gridColumn: side ? 'span 1' : undefined, minWidth: 0 }}>{main}</Box>
      {side && <Box style={{ minWidth: 0 }}>{side}</Box>}
    </Box>
  );
}
